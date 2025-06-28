package ru.mityunin.myShop.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.mityunin.client.api.DefaultApi;
import ru.mityunin.client.domain.BalanceGet200Response;
import ru.mityunin.client.domain.PaymentPostRequest;
import ru.mityunin.client.domain.PaymentResponse;
import ru.mityunin.myShop.SpringBootPostgreSQLBase;
import ru.mityunin.myShop.model.Order;
import ru.mityunin.myShop.model.OrderStatus;
import ru.mityunin.myShop.model.Product;
import ru.mityunin.myShop.repository.OrderRepository;
import ru.mityunin.myShop.repository.ProductRepository;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
public class PayServiceTest extends SpringBootPostgreSQLBase {
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PayService payService;

    @MockitoBean
    private DefaultApi paymentApi;
    @MockitoBean
    private ReactiveOAuth2AuthorizedClientManager authorizedClientManager;
    @MockitoBean
    private AdminInitializer adminInitializer;

    @BeforeEach
    public void createTestData() {
        // Очистка данных перед каждым тестом
        orderRepository.deleteAll().block();
        productRepository.deleteAll().block();

        // Инициализация тестовых данных
        Product product = new Product();
        product.setName("Test Product");
        product.setPrice(BigDecimal.TEN);
        productRepository.save(product).block();
    }

    @Test
    @WithMockUser(username = "user1")
    public void shouldGetCurrentBalance() {
        // 1. Создаем тестовый токен
        OAuth2AccessToken accessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                "test-token",
                Instant.now(),
                Instant.now().plusSeconds(3600));

        // 2. Создаем мок авторизованного клиента
        org.springframework.security.oauth2.client.OAuth2AuthorizedClient authorizedClient =
                Mockito.mock(org.springframework.security.oauth2.client.OAuth2AuthorizedClient.class);
        when(authorizedClient.getAccessToken()).thenReturn(accessToken);

        // 3. Настраиваем менеджер для возврата мока клиента
        when(authorizedClientManager.authorize(any(OAuth2AuthorizeRequest.class)))
                .thenReturn(Mono.just(authorizedClient));

        // 4. Мокаем ApiClient
        ru.mityunin.client.ApiClient apiClient = Mockito.mock(ru.mityunin.client.ApiClient.class);
        when(paymentApi.getApiClient()).thenReturn(apiClient);

        // 5. Настраиваем ответ API
        BalanceGet200Response response = new BalanceGet200Response();
        response.setBalance(15F);
        when(paymentApi.balanceGet("user1")).thenReturn(Mono.just(response));

        // 6. Вызываем тестируемый метод и проверяем результат
        assertEquals(response.getBalance(), payService.getCurrentBalance().block());

        // 7. Проверяем, что заголовок был установлен
        Mockito.verify(apiClient).addDefaultHeader("Authorization", "Bearer test-token");
    }
//
@Test
@WithMockUser(username = "user1")
public void shouldGetMinusBalanceOnError() {
    // 1. Мокируем получение токена
    OAuth2AccessToken accessToken = new OAuth2AccessToken(
            OAuth2AccessToken.TokenType.BEARER,
            "test-token",
            Instant.now(),
            Instant.now().plusSeconds(3600));

    org.springframework.security.oauth2.client.OAuth2AuthorizedClient authorizedClient =
            Mockito.mock(org.springframework.security.oauth2.client.OAuth2AuthorizedClient.class);
    when(authorizedClient.getAccessToken()).thenReturn(accessToken);
    when(authorizedClientManager.authorize(any(OAuth2AuthorizeRequest.class)))
            .thenReturn(Mono.just(authorizedClient));

    // 2. Мокируем ApiClient
    ru.mityunin.client.ApiClient apiClient = Mockito.mock(ru.mityunin.client.ApiClient.class);
    when(paymentApi.getApiClient()).thenReturn(apiClient);

    // 3. Мокируем ошибку при запросе баланса
    when(paymentApi.balanceGet("user1")).thenReturn(Mono.error(new RuntimeException("API error")));

    StepVerifier.create(payService.getCurrentBalance())
            .expectNext(-1F)
            .verifyComplete();

    // 4. Проверяем, что заголовок был установлен
    Mockito.verify(apiClient).addDefaultHeader("Authorization", "Bearer test-token");
}

    @Test
    @WithMockUser(username = "user1")
    public void shouldGetPaymentResponse() {
        // 1. Мокируем получение токена
        OAuth2AccessToken accessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                "test-token",
                Instant.now(),
                Instant.now().plusSeconds(3600));

        org.springframework.security.oauth2.client.OAuth2AuthorizedClient authorizedClient =
                Mockito.mock(org.springframework.security.oauth2.client.OAuth2AuthorizedClient.class);
        when(authorizedClient.getAccessToken()).thenReturn(accessToken);
        when(authorizedClientManager.authorize(any(OAuth2AuthorizeRequest.class)))
                .thenReturn(Mono.just(authorizedClient));

        // 2. Мокируем ApiClient
        ru.mityunin.client.ApiClient apiClient = Mockito.mock(ru.mityunin.client.ApiClient.class);
        when(paymentApi.getApiClient()).thenReturn(apiClient);

        // 3. Подготавливаем тестовые данные
        PaymentPostRequest paymentPostRequest = new PaymentPostRequest();
        paymentPostRequest.setAmount(9F);

        PaymentResponse paymentResponse = new PaymentResponse();
        paymentResponse.setProcessed(true);

        // 4. Мокируем вызов API
        when(paymentApi.paymentPost("user1", paymentPostRequest))
                .thenReturn(Mono.just(paymentResponse));

        // 5. Создаем тестовый заказ
        Order order = new Order();
        order.setStatus(OrderStatus.PRE_ORDER);
        order.setTotalPrice(BigDecimal.valueOf(9F));
        order.setUsername("user1");
        orderRepository.save(order).block();

        // 6. Выполняем тест
        StepVerifier.create(payService.setPaidFor(order.getId()))
                .expectNext(paymentResponse)
                .verifyComplete();

        // 7. Проверяем, что заголовок был установлен
        Mockito.verify(apiClient).addDefaultHeader("Authorization", "Bearer test-token");
    }
}
