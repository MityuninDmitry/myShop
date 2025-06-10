package ru.mityunin.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.mityunin.server.domain.BalanceGet200Response;
import ru.mityunin.server.domain.BalancePost200Response;
import ru.mityunin.server.domain.BalancePostRequest;
import ru.mityunin.service.PaymentService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

public class BalanceControllerTest {

    private BalanceControllerImpl balanceController;


    private PaymentService paymentService;

    @BeforeEach
    void setUp() {

        paymentService = new PaymentService();
        paymentService.setBalance(100f); // Устанавливаем начальный баланс
        balanceController = new BalanceControllerImpl(paymentService);
    }

    @Test
    void balanceGet_shouldReturnCurrentBalance() {
        Mono<ResponseEntity<BalanceGet200Response>> result = balanceController.balanceGet(null);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(200, response.getStatusCodeValue());
                    assertEquals(100f, response.getBody().getBalance());
                })
                .verifyComplete();
    }

    @Test
    void balancePost_shouldIncreaseBalanceAndReturnNewBalance() {
        BalancePostRequest request = new BalancePostRequest();
        request.setAmount(50f);

        Mono<ResponseEntity<BalancePost200Response>> result =
                balanceController.balancePost(Mono.just(request), null);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(200, response.getStatusCodeValue());
                    assertEquals(150f, response.getBody().getNewBalance());

                    // Проверяем, что баланс действительно изменился в сервисе
                    StepVerifier.create(paymentService.getBalance())
                            .expectNext(150f)
                            .verifyComplete();
                })
                .verifyComplete();
    }
}
