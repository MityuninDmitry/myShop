package ru.mityunin.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.mityunin.server.domain.PaymentPostRequest;
import ru.mityunin.server.domain.PaymentResponse;
import ru.mityunin.service.PaymentService;

import static org.junit.jupiter.api.Assertions.*;

class PaymentControllerTest {

    private PaymentControllerImpl paymentController;
    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService();
        paymentController = new PaymentControllerImpl(paymentService);
    }

    @Test
    void paymentPost_whenAmountIsNegative_shouldReturnUnprocessedResponse() {
        PaymentPostRequest request = new PaymentPostRequest();
        request.setAmount(-10f);

        Mono<ResponseEntity<PaymentResponse>> result = paymentController.paymentPost("NEED_NAME",Mono.just(request), null);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(200, response.getStatusCodeValue());
                    assertFalse(response.getBody().getProcessed());
                    assertEquals("Неверный запрос (некорректная сумма)", response.getBody().getDescription());

                    // Проверяем, что баланс не изменился
                    StepVerifier.create(paymentService.getBalance("NEED_NAME"))
                            .expectNext(100f)
                            .verifyComplete();
                })
                .verifyComplete();
    }

    @Test
    void paymentPost_whenBalanceIsSufficient_shouldReturnProcessedResponse() {
        PaymentPostRequest request = new PaymentPostRequest();
        request.setAmount(50f);

        Mono<ResponseEntity<PaymentResponse>> result = paymentController.paymentPost("NEED_NAME",Mono.just(request), null);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(200, response.getStatusCodeValue());
                    assertTrue(response.getBody().getProcessed());
                    assertEquals("Заказ успешно оплачен", response.getBody().getDescription());

                    // Проверяем, что баланс уменьшился на 50
                    StepVerifier.create(paymentService.getBalance("NEED_NAME"))
                            .expectNext(50f)
                            .verifyComplete();
                })
                .verifyComplete();
    }

    @Test
    void paymentPost_whenBalanceIsInsufficient_shouldReturnUnprocessedResponse() {
        PaymentPostRequest request = new PaymentPostRequest();
        request.setAmount(150f); // Запрашиваем больше, чем есть на балансе (100)

        Mono<ResponseEntity<PaymentResponse>> result = paymentController.paymentPost("NEED_NAME",Mono.just(request), null);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(200, response.getStatusCodeValue());
                    assertFalse(response.getBody().getProcessed());
                    assertEquals("Недостаточно средств на балансе", response.getBody().getDescription());

                    // Проверяем, что баланс не изменился
                    StepVerifier.create(paymentService.getBalance("NEED_NAME"))
                            .expectNext(100f)
                            .verifyComplete();
                })
                .verifyComplete();
    }

    @Test
    void paymentPost_whenAmountIsZero_shouldReturnUnprocessedResponse() {
        PaymentPostRequest request = new PaymentPostRequest();
        request.setAmount(0f);

        Mono<ResponseEntity<PaymentResponse>> result = paymentController.paymentPost("NEED_NAME",Mono.just(request), null);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(200, response.getStatusCodeValue());
                    assertFalse(response.getBody().getProcessed());
                    assertEquals("Неверный запрос (некорректная сумма)", response.getBody().getDescription());
                })
                .verifyComplete();
    }
}