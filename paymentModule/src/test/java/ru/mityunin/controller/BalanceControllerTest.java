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
import ru.mityunin.server.domain.BalanceGet200Response;
import ru.mityunin.server.domain.BalancePost200Response;
import ru.mityunin.server.domain.BalancePostRequest;
import ru.mityunin.service.PaymentService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
public class BalanceControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        when(paymentService.getBalance(anyString())).thenReturn(Mono.just(100.0f));
        when(paymentService.increaseBalance(anyString(), any(Mono.class))).thenReturn(Mono.just(150.0f));
    }

    @Test
    @WithMockUser(roles = "WEB_APP")
    void balanceGet_ShouldReturnBalance() {
        webTestClient
                .get()
                .uri("/balance?username=testUser")
                .exchange()
                .expectStatus().isOk()
                .expectBody(BalanceGet200Response.class)
                .value(response -> response.getBalance().equals(100.0f));
    }

    @Test
    @WithMockUser(roles = "WEB_APP")
    void balancePost_ShouldIncreaseBalance() {
        BalancePostRequest request = new BalancePostRequest();
        request.setAmount(50.0f);

        webTestClient.post()
                .uri("/balance?username=testUser")
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody(BalancePost200Response.class)
                .value(response -> response.getNewBalance().equals(150.0f));
    }

    @Test
    void balanceGet_Unauthorized_ShouldReturnForbidden() {
        webTestClient.get()
                .uri("/balance?username=testUser")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void balanceGet_WithoutRequiredRole_ShouldReturnForbidden() {
        webTestClient.mutateWith(SecurityMockServerConfigurers.mockJwt()
                        .authorities(new SimpleGrantedAuthority("ROLE_OTHER_ROLE")))
                .get()
                .uri("/balance?username=testUser")
                .exchange()
                .expectStatus().isForbidden();
    }
}
