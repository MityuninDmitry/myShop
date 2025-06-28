package ru.mityunin.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import ru.mityunin.server.domain.PaymentPostRequest;
import ru.mityunin.server.domain.PaymentResponse;
import ru.mityunin.service.PaymentService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class PaymentControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        when(paymentService.processPayment(anyString(), any(Mono.class)))
                .thenReturn(Mono.just(true));
    }

    @Test
    @WithMockUser(roles = "WEB_APP")
    void paymentPost_ShouldProcessPayment() {
        PaymentPostRequest request = new PaymentPostRequest();
        request.setAmount(50.0f);

        webTestClient.post()
                .uri("/payment?username=testUser")
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody(PaymentResponse.class)
                .value(response -> {
                    assert response.getProcessed();
                    assert response.getDescription().equals("Заказ успешно оплачен");
                });
    }

    @Test
    @WithMockUser(roles = "WEB_APP")
    void paymentPost_InvalidAmount_ShouldReturnBadRequest() {
        PaymentPostRequest request = new PaymentPostRequest();
        request.setAmount(-10.0f);

        webTestClient.post()
                .uri("/payment?username=testUser")
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(PaymentResponse.class)
                ;
    }

    @Test
    void paymentPost_Unauthorized_ShouldReturnUnauthorized() {
        PaymentPostRequest request = new PaymentPostRequest();
        request.setAmount(50.0f);

        webTestClient.post()
                .uri("/payment?username=testUser")
                .bodyValue(request)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void paymentPost_WithInvalidRole_ShouldReturnForbidden() {
        webTestClient.mutateWith(SecurityMockServerConfigurers.mockJwt()
                        .authorities(new SimpleGrantedAuthority("ROLE_INVALID_ROLE")))
                .post()
                .uri("/payment?username=testUser")
                .bodyValue(new PaymentPostRequest())
                .exchange()
                .expectStatus().isForbidden();
    }
}