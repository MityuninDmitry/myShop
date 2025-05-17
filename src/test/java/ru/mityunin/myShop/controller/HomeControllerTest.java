package ru.mityunin.myShop.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import ru.mityunin.myShop.SpringBootPostgreSQLBase;
import ru.mityunin.myShop.controller.DTO.FilterRequest;
import ru.mityunin.myShop.model.Product;
import ru.mityunin.myShop.repository.OrderRepository;
import ru.mityunin.myShop.repository.OrderedProductRepository;
import ru.mityunin.myShop.repository.ProductRepository;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class HomeControllerTest extends SpringBootPostgreSQLBase {

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
    public void getProducts_shouldReturnDefaultProducts() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/")
                        .param("page", "0")
                        .param("size", "10")
                        .param("textFilter", "")
                        .param("sortBy", "name")
                        .param("sortDirection", "asc")
                )
                .andExpect(view().name("Products"))
                .andExpect(model().attributeExists("products"))
                .andExpect(model().attributeExists("filterRequest"))
                .andExpect(model().attribute("products",hasSize(10)))
                .andExpect(result -> {
                    FilterRequest filterRequest = (FilterRequest) result.getModelAndView()
                            .getModel()
                            .get("filterRequest");
                    assertEquals(0,filterRequest.page());
                    assertEquals(10,filterRequest.size());
                    assertEquals("",filterRequest.textFilter());
                    assertEquals("name", filterRequest.sortBy());
                    assertEquals("asc", filterRequest.sortDirection());
                });
    }

    @Test
    public void getProducts_shouldReturnZeroProducts() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/")
                        .param("page", "6")
                        .param("size", "10")
                        .param("textFilter", "")
                        .param("sortBy", "name")
                        .param("sortDirection", "asc")
                )
                .andExpect(view().name("Products"))
                .andExpect(model().attributeExists("products"))
                .andExpect(model().attributeExists("filterRequest"))
                .andExpect(model().attribute("products",hasSize(0)))
                .andExpect(result -> {
                    FilterRequest filterRequest = (FilterRequest) result.getModelAndView()
                            .getModel()
                            .get("filterRequest");
                    assertEquals(6,filterRequest.page());
                    assertEquals(10,filterRequest.size());
                    assertEquals("",filterRequest.textFilter());
                    assertEquals("name", filterRequest.sortBy());
                    assertEquals("asc", filterRequest.sortDirection());
                });
    }
}
