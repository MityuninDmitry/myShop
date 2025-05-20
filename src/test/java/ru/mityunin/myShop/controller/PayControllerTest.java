package ru.mityunin.myShop.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Mono;
import ru.mityunin.myShop.SpringBootPostgreSQLBase;
import ru.mityunin.myShop.service.PayService;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureWebTestClient
public class PayControllerTest extends SpringBootPostgreSQLBase {

    @Autowired
    private WebTestClient webTestClient;
    @MockitoBean
    private PayService payService;

    @Test
    public void payOrderWithOrderId() throws Exception {
        when(payService.setPaidFor(anyLong())).thenReturn(Mono.empty());

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("order_id", "1");

        webTestClient.post().uri("/pay")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(formData))
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/order/basket");

        verify(payService).setPaidFor(1L);

    }
}
