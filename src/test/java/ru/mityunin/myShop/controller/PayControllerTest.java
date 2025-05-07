package ru.mityunin.myShop.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import ru.mityunin.myShop.SpringBootPostgreSQLBase;
import ru.mityunin.myShop.service.PayService;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class PayControllerTest extends SpringBootPostgreSQLBase {

    @Autowired
    MockMvc mockMvc;
    @MockitoBean
    private PayService payService;

    @Test
    public void payOrderWithOrderId() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/pay")
                .param("order_id","1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/order/1"));

        verify(payService).setPaidFor(Long.valueOf(1));
    }
}
