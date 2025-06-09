package ru.mityunin.myShop.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.test.context.bean.override.mockito.MockitoBean;
import reactor.core.publisher.Flux;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

public class PayServiceTest extends SpringBootPostgreSQLBase {
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PayService payService;

    @MockitoBean
    private DefaultApi paymentApi;

    @BeforeEach
    public void createTestData() {
        Flux.zip(productRepository.deleteAll(), orderRepository.deleteAll())
                .thenMany(Flux.range(1,51).flatMap( i ->
                        {
                            Product product = new Product();
                            product.setName("Name " + i);
                            product.setPrice(BigDecimal.valueOf(i));
                            product.setDescription("Some desc " + i);
                            product.setImageUrl("https://images.hdqwalls.com/download/sunset-ronin-ghost-of-tsushima-40-2880x1800.jpg");

                            return productRepository.save(product);
                        }
                )).blockLast();
    }

    @Test
    public void shouldGetCurrentBalance() {
        BalanceGet200Response response = new BalanceGet200Response();
        response.setBalance(15F);
        when(paymentApi.balanceGet()).thenReturn(Mono.just(response));

        assertEquals(response.getBalance(), payService.getCurrentBalance().block());
    }

    @Test
    public void shouldGetMinusBalanceOnError() {;
        when(paymentApi.balanceGet()).thenReturn(Mono.error(new Exception()));

        StepVerifier.create(payService.getCurrentBalance())
                .expectNext(-1F)
                .verifyComplete();
    }

    @Test
    public void shouldGetPaymentResponse() {
        PaymentPostRequest paymentPostRequest = new PaymentPostRequest();
        paymentPostRequest.setAmount(9F);

        PaymentResponse paymentResponse = new PaymentResponse();
        paymentResponse.setProcessed(true);

        when(paymentApi.paymentPost(paymentPostRequest)).thenReturn(Mono.just(paymentResponse));

        Order order = new Order();
        order.setStatus(OrderStatus.PRE_ORDER);
        order.setTotalPrice(BigDecimal.valueOf(9F));
        orderRepository.save(order).block();

        StepVerifier.create(payService.setPaidFor(order.getId()))
                .expectNext(paymentResponse)
                .verifyComplete();
    }
}
