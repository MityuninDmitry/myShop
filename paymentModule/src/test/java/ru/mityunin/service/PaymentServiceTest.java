package ru.mityunin.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.mityunin.server.domain.BalancePostRequest;
import ru.mityunin.server.domain.PaymentPostRequest;



public class PaymentServiceTest {

    @InjectMocks
    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        paymentService = new PaymentService();
        ReflectionTestUtils.setField(paymentService, "initialBalance", 100f);
    }

    @Test
    void getBalance_shouldReturnCurrentBalance() {
        StepVerifier.create(paymentService.getBalance("NEED_NAME"))
                .expectNext(100f)
                .verifyComplete();
    }

    @Test
    void increaseBalance_shouldAddAmountToBalance() {
        BalancePostRequest request = new BalancePostRequest();
        request.setAmount(50f);

        StepVerifier.create(paymentService.increaseBalance("NEED_NAME", Mono.just(request)))
                .expectNext(150f)
                .verifyComplete();

        StepVerifier.create(paymentService.getBalance("NEED_NAME"))
                .expectNext(150f)
                .verifyComplete();
    }

    @Test
    void processPayment_whenBalanceIsSufficient_shouldReturnTrueAndDeductAmount() {
        PaymentPostRequest request = new PaymentPostRequest();
        request.setAmount(50f);

        StepVerifier.create(paymentService.processPayment("NEED_NAME",Mono.just(request)))
                .expectNext(true)
                .verifyComplete();

        StepVerifier.create(paymentService.getBalance("NEED_NAME"))
                .expectNext(50f)
                .verifyComplete();
    }

    @Test
    void processPayment_whenBalanceIsInsufficient_shouldReturnFalse() {
        PaymentPostRequest request = new PaymentPostRequest();
        request.setAmount(150f);

        StepVerifier.create(paymentService.processPayment("NEED_NAME",Mono.just(request)))
                .expectNext(false)
                .verifyComplete();

        StepVerifier.create(paymentService.getBalance("NEED_NAME"))
                .expectNext(100f)
                .verifyComplete();
    }
}
