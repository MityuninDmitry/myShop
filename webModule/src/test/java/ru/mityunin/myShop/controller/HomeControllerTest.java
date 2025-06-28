package ru.mityunin.myShop.controller;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import ru.mityunin.myShop.SpringBootPostgreSQLBase;
import ru.mityunin.myShop.controller.DTO.FilterRequest;
import ru.mityunin.myShop.model.Product;
import ru.mityunin.myShop.repository.OrderRepository;
import ru.mityunin.myShop.repository.ProductRepository;
import ru.mityunin.myShop.repository.UserRepository;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@AutoConfigureWebTestClient
public class HomeControllerTest extends SpringBootPostgreSQLBase {


    @Autowired
    private WebTestClient webTestClient;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;


    @BeforeEach
    public void createTestData() {
        Flux.zip(
                        productRepository.deleteAll(),
                        orderRepository.deleteAll()
                )
                .thenMany(Flux.range(0, 50).flatMap(i -> {
                    Product product = new Product();
                    product.setName("Name " + i);
                    product.setPrice(BigDecimal.valueOf(i));
                    product.setDescription("Some desc " + i);
                    product.setImageUrl("https://images.hdqwalls.com/download/sunset-ronin-ghost-of-tsushima-40-2880x1800.jpg");
                    return productRepository.save(product);
                }))
                .blockLast();

    }

    @Test
    @WithMockUser(username = "user1", roles = "USER")
    public void getProducts_shouldReturnDefaultProducts() {
        FilterRequest filterRequest = new FilterRequest();

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/")
                        .queryParam("page", filterRequest.page())
                        .queryParam("size", filterRequest.size())
                        .queryParam("textFilter", filterRequest.textFilter())
                        .queryParam("sortBy", filterRequest.sortBy())
                        .queryParam("sortDirection", filterRequest.sortDirection())
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .consumeWith(response -> {
                    String body = response.getResponseBody();
                    Document document = Jsoup.parse(body);
                    var productCards = document.select(".product-card");
                    assertEquals(10, productCards.size());
                });
    }
    @Test
    @WithMockUser(username = "user2", roles = "USER")
    public void getProducts_shouldReturnZeroProductsWhenPageOutOfRange() {
        FilterRequest filterRequest = new FilterRequest();
        filterRequest.setPage(6);

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/")
                        .queryParam("page", filterRequest.page())
                        .queryParam("size", filterRequest.size())
                        .queryParam("textFilter", filterRequest.textFilter())
                        .queryParam("sortBy", filterRequest.sortBy())
                        .queryParam("sortDirection", filterRequest.sortDirection())
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .consumeWith(response -> {
                    String body = response.getResponseBody();
                    Document document = Jsoup.parse(body);
                    var productCards = document.select(".product-card");
                    assertEquals(0, productCards.size());
                });
    }

    @Test
    public void getProducts_shouldAllowAccessWithoutAuthentication() {
        FilterRequest filterRequest = new FilterRequest();

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/")
                        .queryParam("page", filterRequest.page())
                        .queryParam("size", filterRequest.size())
                        .build())
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    public void getProducts_shouldWorkForAdmin() {
        FilterRequest filterRequest = new FilterRequest();

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/")
                        .queryParam("page", filterRequest.page())
                        .queryParam("size", filterRequest.size())
                        .build())
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    public void getProducts_shouldDenyAccessForUnauthorizedUser() {
        webTestClient.get()
                .uri("/order/basket")
                .exchange()
                .expectStatus().is3xxRedirection();
    }
}
