package ru.mityunin.myShop.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import ru.mityunin.myShop.SpringBootPostgreSQLBase;
import ru.mityunin.myShop.controller.DTO.FilterRequest;
import ru.mityunin.myShop.model.*;
import ru.mityunin.myShop.repository.OrderRepository;
import ru.mityunin.myShop.repository.OrderedProductRepository;
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
    private OrderedProductRepository orderedProductRepository;

    @Autowired
    private ProductService productService;

    @BeforeEach
    public void createTestData() {
        productRepository.deleteAll();
        orderRepository.deleteAll();
        orderedProductRepository.deleteAll();

        for (int i = 0; i < 10; i++) {
            Product product = new Product();
            product.setName("Name " + i);
            product.setPrice(BigDecimal.valueOf(i));
            product.setDescription("Some desc " + i);
            product.setImageUrl("https://images.hdqwalls.com/download/sunset-ronin-ghost-of-tsushima-40-2880x1800.jpg");

            productRepository.save(product);
        }
    }

    @Test
    public void shouldGetProductByProductId() {
//        Product product = productRepository.findAll().get(0);
//        Product sameProduct = productService.getProductBy(product.getId());
//
//        assertEquals(product.getId(),sameProduct.getId());
//        assertEquals(product.getName(),sameProduct.getName());
//        assertEquals(product.getDescription(),sameProduct.getDescription());
//        assertEquals(product.getImageUrl(),sameProduct.getImageUrl());

    }

    @Test
    @Transactional
    public void shouldReturnExactCountProductsByDifferentFilters() {
//        FilterRequest filterRequest = new FilterRequest(0,3,"","name","asc");
//        List<Product> products = productService.findAll(filterRequest);
//        assertEquals(3, products.size());
//
//        filterRequest = new FilterRequest(1,3,"","name","asc");
//        products = productService.findAll(filterRequest);
//        assertEquals(3, products.size());
//
//        filterRequest = new FilterRequest(3,3,"","name","asc");
//        products = productService.findAll(filterRequest);
//        assertEquals(1, products.size());
//
//        // нет такого имени
//        filterRequest = new FilterRequest(0,10,"dsfsdf","name","asc");
//        products = productService.findAll(filterRequest);
//        assertEquals(0, products.size());
//
//        // проверка, что возрвщается четко по фильтру
//        filterRequest = new FilterRequest(0,10,"9","name","asc");
//        products = productService.findAll(filterRequest);
//        assertEquals(1, products.size());
//
//        // проверка сортировки
//        filterRequest = new FilterRequest(0,3,"","price","desc");
//        products = productService.findAll(filterRequest);
//        assertEquals(3, products.size());
//        assertEquals(BigDecimal.valueOf(9), products.get(0).getPrice());
//        assertEquals(BigDecimal.valueOf(8), products.get(1).getPrice());
//        assertEquals(BigDecimal.valueOf(7), products.get(2).getPrice());
    }

    @Test
    public void shouldReturnZeroCountInBasketWhenProductNotInBasket() {
//        Product product = productRepository.findAll().getFirst();
//        productService.setCountInBasketFor(product);
//        assertEquals(BigDecimal.valueOf(0.00).doubleValue(), product.getPrice().doubleValue());
    }

    @Test
    @Transactional
    public void shouldReturnNotZeroCountInBasketWhenProductInBasket() {
//        Order basket = new Order();
//        basket.setStatus(OrderStatus.PRE_ORDER);
//        basket.setTotalPrice(BigDecimal.ZERO);
//
//
//        Product product = productRepository.findAll().getFirst();
//
//
//        OrderedProduct orderedProduct = new OrderedProduct();
//        orderedProduct.setCount(4);
//        orderedProduct.setOrder(basket);
//        orderedProduct.setProduct(product);
//
//        basket.setOrderedProducts(List.of(orderedProduct));
//        orderRepository.save(basket);
//
//
//        productService.setCountInBasketFor(product);
//        assertEquals(4, product.getCountInBasket());
    }
}
