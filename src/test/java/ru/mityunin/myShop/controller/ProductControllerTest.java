package ru.mityunin.myShop.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import ru.mityunin.myShop.SpringBootPostgreSQLBase;
import ru.mityunin.myShop.model.Product;
import ru.mityunin.myShop.repository.OrderRepository;
import ru.mityunin.myShop.repository.OrderedProductRepository;
import ru.mityunin.myShop.repository.ProductRepository;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class ProductControllerTest extends SpringBootPostgreSQLBase {
    @Autowired
    MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private OrderedProductRepository orderedProductRepository;

    @BeforeEach
    public void createTestData() {
        productRepository.deleteAll();
        orderRepository.deleteAll();
        orderedProductRepository.deleteAll();

        for (int i = 0; i < 50; i++) {
            Product product = new Product();
            product.setName("Name " + i);
            product.setPrice(BigDecimal.valueOf(i));
            product.setDescription("Some desc " + i);
            product.setImageUrl("https://images.hdqwalls.com/download/sunset-ronin-ghost-of-tsushima-40-2880x1800.jpg");

            productRepository.save(product);
        }
    }

    @Test
    public void getProductPage() throws Exception {
        Product product = productRepository.findAll().getFirst();
        mockMvc.perform(MockMvcRequestBuilders.get("/product/" + product.getId()))
                .andExpect(view().name("Product"))
                .andExpect(model().attributeExists("product"))
                .andExpect(result -> {
                    Product p = (Product) result.getModelAndView().getModel().get("product");
                    assertEquals(p.getName(), product.getName());
                    assertEquals(p.getImageUrl(), product.getImageUrl());
                    assertEquals(p.getPrice(), product.getPrice());
                    assertEquals(p.getDescription(), product.getDescription());
                })
        ;
    }
}
