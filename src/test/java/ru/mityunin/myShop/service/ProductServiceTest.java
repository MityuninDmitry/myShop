package ru.mityunin.myShop.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Flux;
import ru.mityunin.myShop.SpringBootPostgreSQLBase;
import ru.mityunin.myShop.controller.DTO.FilterRequest;
import ru.mityunin.myShop.model.*;
import ru.mityunin.myShop.repository.OrderRepository;
import ru.mityunin.myShop.repository.ProductRepository;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProductServiceTest extends SpringBootPostgreSQLBase {
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductService productService;

    @BeforeEach
    public void createTestData() {
        Flux.zip(productRepository.deleteAll(), orderRepository.deleteAll())
                .thenMany(Flux.range(1,50).flatMap( i ->
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
    public void shouldGetProductByProductId() {
        Product product = productRepository.findAll().collectList().block().get(0);
        Product sameProduct = productService.getProductBy(product.getId()).block();

        assertEquals(product.getId(),sameProduct.getId());
        assertEquals(product.getName(),sameProduct.getName());
        assertEquals(product.getDescription(),sameProduct.getDescription());
        assertEquals(product.getImageUrl(),sameProduct.getImageUrl());

    }

    @Test
    public void shouldReturnExactCountProductsByDifferentFilters() {
        FilterRequest filterRequest = new FilterRequest(0,3,"","name","asc");
        List<Product> products = productService.findAll(filterRequest).collectList().block();
        assertEquals(3, products.size());

        filterRequest = new FilterRequest(1,3,"","name","asc");
        products = productService.findAll(filterRequest).collectList().block();
        assertEquals(3, products.size());

        filterRequest = new FilterRequest(16,3,"","name","asc");
        products = productService.findAll(filterRequest).collectList().block();
        assertEquals(2, products.size());

        // нет такого имени
        filterRequest = new FilterRequest(0,10,"dsfsdf","name","asc");
        products = productService.findAll(filterRequest).collectList().block();
        assertEquals(0, products.size());

        // проверка, что возрвщается четко по фильтру
        filterRequest = new FilterRequest(0,10,"9","name","asc");
        products = productService.findAll(filterRequest).collectList().block();
        assertEquals(5, products.size());

        // проверка сортировки
        filterRequest = new FilterRequest(0,3,"","price","desc");
        products = productService.findAll(filterRequest).collectList().block();
        assertEquals(3, products.size());
        assertEquals(BigDecimal.valueOf(50).doubleValue(), products.get(0).getPrice().doubleValue());
        assertEquals(BigDecimal.valueOf(49).doubleValue(), products.get(1).getPrice().doubleValue());
        assertEquals(BigDecimal.valueOf(48).doubleValue(), products.get(2).getPrice().doubleValue());
    }

    @Test
    public void shouldReturnZeroCountInBasketWhenProductNotInBasket() {
        FilterRequest filterRequest = new FilterRequest(0,10,"Name 19", "name", "asc");

        Product product = productService.findAll(filterRequest).collectList().block().getFirst();
        productService.setCountInBasketFor(product).block();
        assertEquals(BigDecimal.valueOf(0.00).doubleValue(), product.getCountInBasket().doubleValue());
    }

    @Test
    public void shouldReturnNotZeroCountInBasketWhenProductInBasket() {
        Order basket = new Order();
        basket.setStatus(OrderStatus.PRE_ORDER);
        basket.setTotalPrice(BigDecimal.ZERO);

        Product product = productRepository.findAll().collectList().block().getFirst();


        OrderedProduct orderedProduct = new OrderedProduct();
        orderedProduct.setCount(4);
        orderedProduct.setProduct_id(product.getId());

        basket.setOrderedProducts(List.of(orderedProduct));
        orderRepository.save(basket).block();


        productService.setCountInBasketFor(product).block();
        assertEquals(4, product.getCountInBasket());
    }
}
