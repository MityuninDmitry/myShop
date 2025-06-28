package ru.mityunin.myShop.controller;

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
import reactor.core.publisher.Mono;
import ru.mityunin.client.domain.PaymentResponse;
import ru.mityunin.myShop.SpringBootPostgreSQLBase;
import ru.mityunin.myShop.service.OrderService;
import ru.mityunin.myShop.service.PayService;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@SpringBootTest
@AutoConfigureWebTestClient
public class PayControllerTest extends SpringBootPostgreSQLBase {

    @Autowired
    private WebTestClient webTestClient;
    @MockitoBean
    private PayService payService;

    @MockitoBean
    private OrderService orderService;

    @BeforeEach
    public void setup() {
        // Reset mocks before each test
        reset(payService, orderService);
    }

    @Test
    @WithMockUser(username = "user1", roles = {"USER"})
    public void successfulPaymentShouldRedirectToBasket() {
        // Arrange
        PaymentResponse successResponse = new PaymentResponse();
        successResponse.setProcessed(true);
        successResponse.setDescription("Processed");

        when(payService.setPaidFor(anyLong())).thenReturn(Mono.just(successResponse));
        when(orderService.setPaidFor(eq("user1"), anyLong())).thenReturn(Mono.empty());

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("order_id", "123");

        webTestClient.post().uri("/pay")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(formData))
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/order/basket");

        verify(payService).setPaidFor(123L);
        verify(orderService).setPaidFor("user1", 123L);
    }

    @Test
    @WithMockUser(username = "user1", roles = {"USER"})
    public void failedPaymentShouldRedirectWithErrorMessage() {
        // Arrange
        String errorMessage = "No money";
        PaymentResponse failedResponse = new PaymentResponse();
        failedResponse.setProcessed(false);
        failedResponse.setDescription(errorMessage);

        when(payService.setPaidFor(anyLong())).thenReturn(Mono.just(failedResponse));

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("order_id", "456");

        // Act & Assert
        webTestClient.post().uri("/pay")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(formData))
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().value("Location", location -> {
                    String expectedLocation = "/order/basket?error=" +
                            URLEncoder.encode("Оплата не прошла: " + errorMessage, StandardCharsets.UTF_8);
                    assert expectedLocation.equals(location);
                });

        verify(payService).setPaidFor(456L);
        verify(orderService, never()).setPaidFor(eq("user1"), anyLong());
    }

    @Test
    public void unauthorizedAccessShouldRedirectToLogin() {
        // Arrange
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("order_id", "123");

        // Act & Assert
        webTestClient.post().uri("/pay")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(formData))
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().value("Location", location ->  {
                    assert location.startsWith("/login");
                });
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void adminShouldBeAbleToPay() {
        // Arrange
        PaymentResponse successResponse = new PaymentResponse();
        successResponse.setProcessed(true);
        successResponse.setDescription("Processed");

        when(payService.setPaidFor(anyLong())).thenReturn(Mono.just(successResponse));
        when(orderService.setPaidFor(eq("admin"), anyLong())).thenReturn(Mono.empty());

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("order_id", "789");

        // Act & Assert
        webTestClient.post().uri("/pay")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(formData))
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/order/basket");

        verify(payService).setPaidFor(789L);
        verify(orderService).setPaidFor("admin", 789L);
    }
}
