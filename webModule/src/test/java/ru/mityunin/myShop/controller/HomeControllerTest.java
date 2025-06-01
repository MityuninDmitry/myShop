package ru.mityunin.myShop.controller;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import ru.mityunin.myShop.SpringBootPostgreSQLBase;
import ru.mityunin.myShop.controller.DTO.FilterRequest;
import ru.mityunin.myShop.model.Product;
import ru.mityunin.myShop.repository.OrderRepository;
import ru.mityunin.myShop.repository.ProductRepository;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureWebTestClient
public class HomeControllerTest extends SpringBootPostgreSQLBase {

    @Autowired
    private WebTestClient webTestClient;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private OrderRepository orderRepository;

    @BeforeEach
    public void createTestData() {
        Flux.zip(productRepository.deleteAll(), orderRepository.deleteAll())
                .thenMany(Flux.range(0,50).flatMap( i ->
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
    public void getProducts_shouldReturnDefaultProducts() throws Exception {
        FilterRequest filterRequest = new FilterRequest();
        webTestClient.get().uri(uriBuilder ->
                uriBuilder.path("/")
                        .queryParam("page",filterRequest.page())
                        .queryParam("size",filterRequest.size())
                        .queryParam("textFilter", filterRequest.textFilter())
                        .queryParam("sortBy",filterRequest.sortBy())
                        .queryParam("sortDirection",filterRequest.sortDirection())
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .consumeWith(response -> {
                    String body = new String(response.getResponseBody());
                    Document document = Jsoup.parse(body);
                    var productCards = document.select(".product-card");
                    assertEquals(10, productCards.size());
                })
                ;
    }

    @Test
    public void getProducts_shouldReturnZeroProducts() throws Exception {
        FilterRequest filterRequest = new FilterRequest();
        filterRequest.setPage(6);
        webTestClient.get().uri(uriBuilder ->
                        uriBuilder.path("/")
                                .queryParam("page",filterRequest.page())
                                .queryParam("size",filterRequest.size())
                                .queryParam("textFilter", filterRequest.textFilter())
                                .queryParam("sortBy",filterRequest.sortBy())
                                .queryParam("sortDirection",filterRequest.sortDirection())
                                .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .consumeWith(response -> {
                    String body = new String(response.getResponseBody());
                    Document document = Jsoup.parse(body);
                    var productCards = document.select(".product-card");
                    assertEquals(0, productCards.size());
                })
        ;
    }
}
