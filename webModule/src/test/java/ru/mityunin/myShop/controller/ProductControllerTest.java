package ru.mityunin.myShop.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;
import ru.mityunin.myShop.SpringBootPostgreSQLBase;
import ru.mityunin.myShop.model.Product;
import ru.mityunin.myShop.repository.OrderRepository;
import ru.mityunin.myShop.repository.ProductRepository;

import java.math.BigDecimal;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureWebTestClient(timeout = "10000")
public class ProductControllerTest extends SpringBootPostgreSQLBase {
    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private OrderRepository orderRepository;

    @BeforeEach
    public void createTestData() {
        productRepository.deleteAll().thenMany(Flux.range(0,50).flatMap(
                i -> {
                    Product product = new Product();
                    product.setName("Name " + i);
                    product.setDescription("Description " + i);
                    product.setPrice(BigDecimal.valueOf(i));
                    return productRepository.save(product);
                }
        )).blockLast();
    }


    @Test
    public void getProductPage() throws Exception {
        Product product = productRepository.findAll().blockFirst();
        webTestClient.get().uri("/product/" + product.getId())
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).consumeWith(response -> {
                    String body = response.getResponseBody();
                    assertNotNull(body);
                    assertTrue(body.contains("<div class=\"product-name\">" + product.getName() + "</div>"));
                    assertTrue(body.contains("<div class=\"product-name\">" + product.getDescription() + "</div>"));
                });
    }
}
