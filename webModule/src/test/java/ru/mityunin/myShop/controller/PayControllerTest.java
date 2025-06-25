package ru.mityunin.myShop.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
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

import static org.junit.Assert.assertEquals;
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

    @Test
    public void successfulPaymentShouldRedirectToBasket() {
        // Arrange
        PaymentResponse successResponse = new PaymentResponse();
        successResponse.setProcessed(true);
        successResponse.setDescription("Processed");
        when(payService.setPaidFor(anyLong())).thenReturn(Mono.just(successResponse));
        when(orderService.setPaidFor("NEED_NAME",anyLong())).thenReturn(Mono.empty());

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("order_id", "123");

        webTestClient.post().uri("/pay")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(formData))
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/order/basket");

        verify(payService).setPaidFor(123L);
        verify(orderService).setPaidFor("NEED_NAME",123L);
    }

    @Test
    public void failedPaymentShouldRedirectWithErrorMessage() {

        String errorMessage = "No money";
        PaymentResponse failedResponse = new PaymentResponse();
        failedResponse.setProcessed(false);
        failedResponse.setDescription(errorMessage);

        when(payService.setPaidFor(anyLong())).thenReturn(Mono.just(failedResponse));

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("order_id", "456");


        webTestClient.post().uri("/pay")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(formData))
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().value("Location", location -> {
                    String expectedLocation = "/order/basket?error=" +
                            URLEncoder.encode("Оплата не прошла: " + errorMessage, StandardCharsets.UTF_8);
                    assertEquals(expectedLocation, location);
                });


        verify(payService).setPaidFor(456L);
        verify(orderService, never()).setPaidFor("NEED_NAME",anyLong());
    }

}
