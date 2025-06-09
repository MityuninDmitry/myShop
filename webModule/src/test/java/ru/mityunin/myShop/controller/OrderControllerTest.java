package ru.mityunin.myShop.controller;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Flux;
import ru.mityunin.myShop.SpringBootPostgreSQLBase;
import ru.mityunin.myShop.controller.DTO.FilterRequest;
import ru.mityunin.myShop.model.*;
import ru.mityunin.myShop.repository.OrderRepository;
import ru.mityunin.myShop.repository.ProductCustomRepository;
import ru.mityunin.myShop.repository.ProductRepository;
import ru.mityunin.myShop.service.OrderService;
import ru.mityunin.myShop.service.ProductService;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureWebTestClient
public class OrderControllerTest extends SpringBootPostgreSQLBase {
    @Autowired
    private WebTestClient webTestClient;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductCustomRepository productCustomRepository;

    @Autowired
    private OrderService orderService;
    @Autowired
    private ProductService productService;

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
    public void getEmptyBasket() throws Exception {
        webTestClient.get().uri("/order/basket")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .consumeWith(response -> {
                    String body = new String(response.getResponseBody());
                    Document document = Jsoup.parse(body);
                    var productCards = document.select(".product-card");
                    assertEquals(0, productCards.size());
                    assertTrue(body.contains("<span>Общая цена корзины: <span>0,00 ₽</span></span>"));
                    assertTrue(body.contains("<div class=\"empty-message\">\n" +
                            "  Товары не найдены\n" +
                            "</div>"));
                });
    }

    @Test
    public void getOrderInfo() throws Exception {
        //  подготовка заказа

        List<Product> products = productRepository.findAll().collectList().block().subList(0,10);
        products.forEach(product -> orderService.updateProductInBasketBy(product.getId(),ActionWithProduct.INCREASE).block(Duration.ofSeconds(5)));
        Order order = orderService.getBasket().block(Duration.ofSeconds(5));
        Double orderTotalPrice = order.getTotalPrice().doubleValue();
        String formattedPrice = String.format("%.2f", orderTotalPrice).replace('.', ',');
        orderService.setPaidFor(order.getId()).block(Duration.ofSeconds(5));


        webTestClient.get().uri("/order/" + order.getId())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .consumeWith(response -> {
                    String body = new String(response.getResponseBody());
                    Document document = Jsoup.parse(body);

                    var productCards = document.select(".product-card");
                    assertEquals(10, productCards.size());


                    assertTrue(body.contains("<span>Общая стоимость заказа: <span>" +formattedPrice+ " ₽</span></span>"));

                });
    }

    @Test
    public void getOrders_OnlyPaidOrdersAndExactSize() throws Exception {
        // подготовка данных
        FilterRequest filterRequest = new FilterRequest();
        List<Product> products = productService.findAll(filterRequest)
                .collectList()
                .block();

        products.forEach(product -> orderService.updateProductInBasketBy(product.getId(),ActionWithProduct.INCREASE).block());
        Order order = orderService.getBasket().block();
        orderService.setPaidFor(order.getId()).block();

        filterRequest.setPage(2);
        products = productService.findAll(filterRequest)
                .collectList()
                .block();

        products.forEach(product -> orderService.updateProductInBasketBy(product.getId(),ActionWithProduct.INCREASE).block());
        order = orderService.getBasket().block();
        orderService.setPaidFor(order.getId()).block();

        webTestClient.get().uri("/order/orders")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .consumeWith(response -> {
                    String body = new String(response.getResponseBody());
                    Document document = Jsoup.parse(body);

                    var productCards = document.select(".product-card");
                    assertEquals(2, productCards.size());
                })
                ;
    }

    @Test
    public void increaseProductInBasketFromDifferentSources() throws Exception  {
        FilterRequest filterRequest = new FilterRequest();
        List<Product> products = productService.findAll(filterRequest)
                .collectList()
                .block();

        Product product = products.getFirst();

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("product_id", product.getId().toString());
        formData.add("actionWithProduct", "INCREASE");
        formData.add("source", "products");
        formData.add("page", filterRequest.page().toString());
        formData.add("size", filterRequest.size().toString());
        formData.add("textFilter", filterRequest.textFilter());
        formData.add("sortBy", filterRequest.sortBy());
        formData.add("sortDirection", filterRequest.sortDirection());

        webTestClient.post().uri("/order/change")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(formData))
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/?page=0&size=10&textFilter=&sortBy=name&sortDirection=asc")
        ;

        Order order = orderService.getBasket().block();

        assertEquals(1,order.getOrderedProducts().stream().filter(orderedProduct -> orderedProduct.getProduct_id().equals(product.getId())).findFirst().get().getCount());

        formData = new LinkedMultiValueMap<>();
        formData.add("product_id", product.getId().toString());
        formData.add("actionWithProduct", "INCREASE");
        formData.add("source", "product");
        formData.add("page", filterRequest.page().toString());
        formData.add("size", filterRequest.size().toString());
        formData.add("textFilter", filterRequest.textFilter());
        formData.add("sortBy", filterRequest.sortBy());
        formData.add("sortDirection", filterRequest.sortDirection());

        webTestClient.post().uri("/order/change")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(formData))
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/product/" + product.getId())
        ;

        order = orderService.getBasket().block();

        assertEquals(2,order.getOrderedProducts().stream().filter(orderedProduct -> orderedProduct.getProduct_id().equals(product.getId())).findFirst().get().getCount());

        formData = new LinkedMultiValueMap<>();
        formData.add("product_id", product.getId().toString());
        formData.add("actionWithProduct", "INCREASE");
        formData.add("source", "basket");
        formData.add("page", filterRequest.page().toString());
        formData.add("size", filterRequest.size().toString());
        formData.add("textFilter", filterRequest.textFilter());
        formData.add("sortBy", filterRequest.sortBy());
        formData.add("sortDirection", filterRequest.sortDirection());

        webTestClient.post().uri("/order/change")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(formData))
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/order/basket")
        ;

        order = orderService.getBasket().block();

        assertEquals(3,order.getOrderedProducts().stream().filter(orderedProduct -> orderedProduct.getProduct_id().equals(product.getId())).findFirst().get().getCount());

    }

    @Test
    public void decreaseProductInBasketFromDifferentSources() throws Exception  {
        FilterRequest filterRequest = new FilterRequest();
        List<Product> products = productService.findAll(filterRequest)
                .collectList()
                .block();

        Product product = products.getFirst();
        orderService.updateProductInBasketBy(product.getId(), ActionWithProduct.INCREASE).block();
        orderService.updateProductInBasketBy(product.getId(), ActionWithProduct.INCREASE).block();
        orderService.updateProductInBasketBy(product.getId(), ActionWithProduct.INCREASE).block();

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("product_id", product.getId().toString());
        formData.add("actionWithProduct", "DECREASE");
        formData.add("source", "products");
        formData.add("page", filterRequest.page().toString());
        formData.add("size", filterRequest.size().toString());
        formData.add("textFilter", filterRequest.textFilter());
        formData.add("sortBy", filterRequest.sortBy());
        formData.add("sortDirection", filterRequest.sortDirection());

        webTestClient.post().uri("/order/change")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(formData))
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/?page=0&size=10&textFilter=&sortBy=name&sortDirection=asc")
        ;

        Order order = orderService.getBasket().block();

        assertEquals(2,order.getOrderedProducts().stream().filter(orderedProduct -> orderedProduct.getProduct_id().equals(product.getId())).findFirst().get().getCount());

        formData = new LinkedMultiValueMap<>();
        formData.add("product_id", product.getId().toString());
        formData.add("actionWithProduct", "DECREASE");
        formData.add("source", "product");
        formData.add("page", filterRequest.page().toString());
        formData.add("size", filterRequest.size().toString());
        formData.add("textFilter", filterRequest.textFilter());
        formData.add("sortBy", filterRequest.sortBy());
        formData.add("sortDirection", filterRequest.sortDirection());

        webTestClient.post().uri("/order/change")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(formData))
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/product/" + product.getId())
        ;

        order = orderService.getBasket().block();

        assertEquals(1,order.getOrderedProducts().stream().filter(orderedProduct -> orderedProduct.getProduct_id().equals(product.getId())).findFirst().get().getCount());

        formData = new LinkedMultiValueMap<>();
        formData.add("product_id", product.getId().toString());
        formData.add("actionWithProduct", "DECREASE");
        formData.add("source", "basket");
        formData.add("page", filterRequest.page().toString());
        formData.add("size", filterRequest.size().toString());
        formData.add("textFilter", filterRequest.textFilter());
        formData.add("sortBy", filterRequest.sortBy());
        formData.add("sortDirection", filterRequest.sortDirection());

        webTestClient.post().uri("/order/change")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(formData))
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/order/basket")
        ;

        order = orderService.getBasket().block();

        assertTrue(order.getOrderedProducts().isEmpty());

    }
}
