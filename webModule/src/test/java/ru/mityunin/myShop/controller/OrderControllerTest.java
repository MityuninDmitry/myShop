package ru.mityunin.myShop.controller;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.mityunin.client.domain.PaymentResponse;
import ru.mityunin.myShop.SpringBootPostgreSQLBase;
import ru.mityunin.myShop.model.ActionWithProduct;
import ru.mityunin.myShop.model.Order;
import ru.mityunin.myShop.model.Product;
import ru.mityunin.myShop.repository.OrderRepository;
import ru.mityunin.myShop.repository.ProductCustomRepository;
import ru.mityunin.myShop.repository.ProductRepository;
import ru.mityunin.myShop.repository.UserRepository;
import ru.mityunin.myShop.service.AuthService;
import ru.mityunin.myShop.service.OrderService;
import ru.mityunin.myShop.service.PayService;
import ru.mityunin.myShop.service.ProductService;

import java.math.BigDecimal;
import java.time.Duration;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@SpringBootTest
@AutoConfigureWebTestClient
public class OrderControllerTest extends SpringBootPostgreSQLBase {
    @Autowired
    private WebTestClient webTestClient;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductCustomRepository productCustomRepository;

    @Autowired
    private OrderService orderService;
    @Autowired
    private ProductService productService;
    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private PayService payService;
    @Autowired
    private AuthService authService;

    @BeforeEach
    public void setUp() {
        // Очистка данных и создание тестовых пользователей

        Flux.zip(
                        productRepository.deleteAll(),
                        orderRepository.deleteAll()
                )
                .thenMany(Flux.range(0, 50).flatMap(i -> {
                    Product product = new Product();
                    product.setName("Name " + i);
                    product.setPrice(BigDecimal.valueOf(i));
                    product.setDescription("Some desc " + i);
                    product.setImageUrl("https://images.hdqwalls.com/download/sunset-ronin-ghost-of-tsushima-40-2880x1800.jpg");
                    return productRepository.save(product);
                }))
                .blockLast();


        // Моки для PayService
        when(payService.getCurrentBalance()).thenReturn(Mono.just(1000.0f));
        when(payService.setPaidFor(anyLong())).thenReturn(Mono.just(
                new PaymentResponse().processed(true).description("Payment successful")));
    }

    @Test
    @WithMockUser(username = "user1", roles = "USER")
    public void getBasket_shouldShowBasketWithCurrentBalance() {
        webTestClient.get().uri("/order/basket")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .consumeWith(response -> {
                    String body = response.getResponseBody();
                    Document document = Jsoup.parse(body);

                    assertTrue(body.contains("Общая цена корзины"));
                    assertTrue(body.contains("1000,00")); // Проверка баланса
                    assertTrue(body.contains("Товары не найдены")); // Пустая корзина
                });
    }

    @Test
    @WithMockUser(username = "user1", roles = "USER")
    public void getBasket_shouldShowPaymentErrorWhenPresent() {
        String errorMessage = "Ошибка оплаты";
        webTestClient.get().uri("/order/basket?error=" + errorMessage)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .consumeWith(response -> {
                    String body = response.getResponseBody();
                    assertTrue(body.contains(errorMessage));
                });
    }

    @Test
    @WithMockUser(username = "user1", roles = "USER")
    public void getOrderInfo_shouldShowOrderDetails() {
        // Создаем тестовый заказ
        Product product = productRepository.findAll().blockFirst(Duration.ofSeconds(5));
        orderService.updateProductInBasketBy(product.getId(), ActionWithProduct.INCREASE)
                .block(Duration.ofSeconds(5));

        Order order = orderService.getBasket().block(Duration.ofSeconds(5));
        orderService.setPaidFor("user1", order.getId()).block(Duration.ofSeconds(5));

        webTestClient.get().uri("/order/" + order.getId())
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .consumeWith(response -> {
                    String body = response.getResponseBody();
                    Document document = Jsoup.parse(body);

                    assertTrue(body.contains("Общая стоимость заказа"));
                    assertTrue(body.contains(product.getName()));
                });
    }

    @Test
    @WithMockUser(username = "user1", roles = "USER")
    public void getOrders_shouldShowOnlyPaidOrdersForCurrentUser() {
        // Создаем заказы для разных пользователей
        Product product = productRepository.findAll().blockFirst(Duration.ofSeconds(5));

        // Заказ для user1
        orderService.updateProductInBasketBy(product.getId(), ActionWithProduct.INCREASE)
                .block(Duration.ofSeconds(5));
        Order user1Order = orderService.getBasket().block(Duration.ofSeconds(5));
        orderService.setPaidFor("user1", user1Order.getId()).block(Duration.ofSeconds(5));

        // Заказ для user2 (не должен отображаться)
        orderService.updateProductInBasketBy(product.getId(), ActionWithProduct.INCREASE)
                .block(Duration.ofSeconds(5));
        Order user2Order = orderService.getBasket().block(Duration.ofSeconds(5));
        orderService.setPaidFor("user2", user2Order.getId()).block(Duration.ofSeconds(5));

        webTestClient.get().uri("/order/orders")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .consumeWith(response -> {
                    String body = response.getResponseBody();
                    Document document = Jsoup.parse(body);

                    // Проверяем что отображается только заказ текущего пользователя
                    assertTrue(body.contains(user1Order.getId().toString()));
                });
    }
    @Test
    @WithMockUser(username = "user1", roles = "USER")
    public void changeProductQuantity_shouldRedirectToCorrectPage() {
        Product product = productRepository.findAll().blockFirst(Duration.ofSeconds(5));

        // Проверка для source=products
        testProductActionRedirect(product, "products",
                "/?page=0&size=10&textFilter=&sortBy=name&sortDirection=asc");

        // Проверка для source=product
        testProductActionRedirect(product, "product",
                "/product/" + product.getId());

        // Проверка для source=basket
        testProductActionRedirect(product, "basket",
                "/order/basket");
    }

    @Test
    @WithMockUser(username = "user1", roles = "USER")
    public void changeProductQuantity_shouldHandleInvalidParameters() {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("actionWithProduct", "INVALID_ACTION");
        formData.add("source", "products");

        webTestClient.post().uri("/order/change")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(formData))
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/error");
    }

    @Test
    public void unauthorizedAccess_shouldBeDenied() {
        webTestClient.get().uri("/order/basket")
                .exchange()
                .expectStatus().is3xxRedirection();

        webTestClient.post().uri("/order/change")
                .exchange()
                .expectStatus().is3xxRedirection();
    }


    private void testProductActionRedirect(Product product, String source, String expectedLocation) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("product_id", product.getId().toString());
        formData.add("actionWithProduct", "INCREASE");
        formData.add("source", source);

        webTestClient.post().uri("/order/change")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(formData))
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", expectedLocation);
    }
}
