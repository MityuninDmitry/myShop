package ru.mityunin.myShop.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;
import ru.mityunin.myShop.SpringBootPostgreSQLBase;
import ru.mityunin.myShop.controller.DTO.FilterRequest;
import ru.mityunin.myShop.model.*;
import ru.mityunin.myShop.repository.OrderRepository;
import ru.mityunin.myShop.repository.OrderedProductRepository;
import ru.mityunin.myShop.repository.ProductRepository;
import ru.mityunin.myShop.service.OrderService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
    private OrderedProductRepository orderedProductRepository;

    @Autowired
    private OrderService orderService;

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
    public void getEmptyBasket() throws Exception {
//        mockMvc.perform(MockMvcRequestBuilders.get("/order/basket"))
//                .andExpect(view().name("Basket"))
//                .andExpect(model().attributeExists("products"))
//                .andExpect(model().attributeExists("totalPrice"))
//                .andExpect(model().attributeExists("order"))
//                .andExpect(result -> {
//                    List<Product> products = (List<Product>) result.getModelAndView().getModel().get("products");
//                    assertEquals(0,products.size());
//                })
//                .andExpect(result -> {
//                    BigDecimal totalPrice = (BigDecimal) result.getModelAndView().getModel().get("totalPrice");
//                    assertEquals(BigDecimal.ZERO.doubleValue(),totalPrice.doubleValue());
//                });

    }

    @Test
    @Transactional
    public void getOrderInfo() throws Exception {
        //  подготовка заказа
//
//        Pageable pageable = PageRequest.of(0,10);
//        List<Product> products = productRepository.findAll(pageable).toList();
//
//        Order order = new Order();
//        order.setStatus(OrderStatus.PAID);
//        order.setTotalPrice(BigDecimal.TEN);
//
//        List<OrderedProduct> orderedProducts = new ArrayList<>();
//        for (Product p: products) {
//            OrderedProduct orderedProduct = new OrderedProduct();
//            orderedProduct.setProduct(p);
//            orderedProduct.setCount(1);
//            orderedProduct.setOrder(order);
//
//            orderedProducts.add(orderedProduct);
//        }
//
//        order.setOrderedProducts(orderedProducts);
//
//        orderRepository.save(order);
//
//        mockMvc.perform(MockMvcRequestBuilders.get("/order/" + order.getId()))
//                .andExpect(view().name("Order"))
//                .andExpect(model().attributeExists("products"))
//                .andExpect(model().attributeExists("totalPrice"))
//                .andExpect(result -> {
//                    List<Product> modelProducts = (List<Product>) result.getModelAndView().getModel().get("products");
//                    assertEquals(products.size(), modelProducts.size());
//                })
//                .andExpect(result -> {
//                    BigDecimal totalPrice = (BigDecimal) result.getModelAndView().getModel().get("totalPrice");
//                    assertEquals(order.getTotalPrice().doubleValue(), totalPrice.doubleValue());
//                })
//        ;
    }

    @Test
    @Transactional
    public void getOrders_OnlyPaidOrdersAndExactSize() throws Exception {
        // подготовка данных
//        Pageable pageable = PageRequest.of(0,10);
//        List<Product> products = productRepository.findAll(pageable).toList();
//
//        for (int i = 0; i < 10; i++) {
//            Order order = new Order();
//            order.setStatus(OrderStatus.PAID);
//            order.setTotalPrice(BigDecimal.TEN);
//
//            List<OrderedProduct> orderedProducts = new ArrayList<>();
//            for (Product p: products) {
//                OrderedProduct orderedProduct = new OrderedProduct();
//                orderedProduct.setProduct(p);
//                orderedProduct.setCount(1);
//                orderedProduct.setOrder(order);
//
//                orderedProducts.add(orderedProduct);
//            }
//
//            order.setOrderedProducts(orderedProducts);
//
//            orderRepository.save(order);
//        }
//
//        Order order = new Order();
//        order.setStatus(OrderStatus.PRE_ORDER);
//        order.setTotalPrice(BigDecimal.ZERO);
//        orderRepository.save(order);
//
//        // сам тест
//        mockMvc.perform(MockMvcRequestBuilders.get("/order/orders"))
//                .andExpect(view().name("Orders"))
//                .andExpect(model().attributeExists("orders"))
//                .andExpect(model().attributeExists("totalPrice"))
//                .andExpect(result -> {
//                    List<Order> modelOrdres = (List<Order>) result.getModelAndView().getModel().get("orders");
//                    assertEquals(10, modelOrdres.size());
//                    assertFalse(modelOrdres.stream().filter(o -> o.getStatus().equals(OrderStatus.PRE_ORDER)).findFirst().isPresent());
//                });
//


    }

    @Test
    @Transactional
    public void increaseProductInBasketFromProducts() throws Exception  {
//        FilterRequest filterRequest = new FilterRequest(0,10,"","name", "asc");
//        Product product = productRepository.findAll().getFirst();
//
//        // Мокаем только для этого теста
//        OrderService mockOrderService = mock(OrderService.class);
//        doNothing().when(mockOrderService).updateProductInBasketBy(any(), any());
//
//        // Вручную заменяем бин для данного теста к контексте спринга
//        ReflectionTestUtils.setField(
//                mockMvc.getDispatcherServlet().getWebApplicationContext()
//                        .getBean(OrderController.class),
//                "orderService",
//                mockOrderService
//        );
//
//        mockMvc.perform(MockMvcRequestBuilders.post("/order/change")
//                        .param("product_id",product.getId().toString())
//                        .param("actionWithProduct", ActionWithProduct.INCREASE.toString())
//                        .flashAttr("filterRequest", filterRequest)
//                        .param("source","products"))
//                .andExpect(status().is3xxRedirection())
//                .andExpect(redirectedUrl("/?page=0&size=10&textFilter=&sortBy=name&sortDirection=asc"))
//                ;
//
//        verify(mockOrderService).updateProductInBasketBy(product.getId(),ActionWithProduct.INCREASE);
//
//        // Возвращаем обратно, чтобы другие тесты проходили
//        ReflectionTestUtils.setField(mockMvc.getDispatcherServlet().getWebApplicationContext()
//                        .getBean(OrderController.class),
//                "orderService",
//                orderService);

    }

    @Test
    @Transactional
    public void decreaseProductInBasketFromProducts() throws Exception  {
//        FilterRequest filterRequest = new FilterRequest(0,10,"","name", "asc");
//        Product product = productRepository.findAll().getFirst();
//
//        // Мокаем только для этого теста
//        OrderService mockOrderService = mock(OrderService.class);
//        doNothing().when(mockOrderService).updateProductInBasketBy(any(), any());
//
//        // Вручную заменяем бин для данного теста к контексте спринга
//        ReflectionTestUtils.setField(
//                mockMvc.getDispatcherServlet().getWebApplicationContext()
//                        .getBean(OrderController.class),
//                "orderService",
//                mockOrderService
//        );
//
//        mockMvc.perform(MockMvcRequestBuilders.post("/order/change")
//                        .param("product_id",product.getId().toString())
//                        .param("actionWithProduct", ActionWithProduct.DECREASE.toString())
//                        .flashAttr("filterRequest", filterRequest)
//                        .param("source","products"))
//                .andExpect(status().is3xxRedirection())
//                .andExpect(redirectedUrl("/?page=0&size=10&textFilter=&sortBy=name&sortDirection=asc"))
//        ;
//
//        verify(mockOrderService).updateProductInBasketBy(product.getId(),ActionWithProduct.DECREASE);
//
//        // Возвращаем обратно, чтобы другие тесты проходили
//        ReflectionTestUtils.setField(mockMvc.getDispatcherServlet().getWebApplicationContext()
//                        .getBean(OrderController.class),
//                "orderService",
//                orderService);

    }

    @Test
    public void increaseProductInBasketFromBasket() throws Exception {
//        FilterRequest filterRequest = new FilterRequest();
//        Product product = productRepository.findAll().getFirst();
//
//        // Мокаем только для этого теста
//        OrderService mockOrderService = mock(OrderService.class);
//        doNothing().when(mockOrderService).updateProductInBasketBy(any(), any());
//
//        // Вручную заменяем бин для данного теста к контексте спринга
//        ReflectionTestUtils.setField(
//                mockMvc.getDispatcherServlet().getWebApplicationContext()
//                        .getBean(OrderController.class),
//                "orderService",
//                mockOrderService
//        );
//
//        mockMvc.perform(MockMvcRequestBuilders.post("/order/change")
//                        .param("product_id",product.getId().toString())
//                        .param("actionWithProduct", ActionWithProduct.INCREASE.toString())
//                        .flashAttr("filterRequest", filterRequest)
//                        .param("source","basket"))
//                .andExpect(status().is3xxRedirection())
//                .andExpect(redirectedUrl("/order/basket"));
//
//        verify(mockOrderService).updateProductInBasketBy(product.getId(),ActionWithProduct.INCREASE);
//
//        // Возвращаем обратно, чтобы другие тесты проходили
//        ReflectionTestUtils.setField(mockMvc.getDispatcherServlet().getWebApplicationContext()
//                        .getBean(OrderController.class),
//                "orderService",
//                orderService);
    }
    @Test
    public void decreaseProductInBasketFromBasket() throws Exception {
//        FilterRequest filterRequest = new FilterRequest();
//        Product product = productRepository.findAll().getFirst();
//
//        // Мокаем только для этого теста
//        OrderService mockOrderService = mock(OrderService.class);
//        doNothing().when(mockOrderService).updateProductInBasketBy(any(), any());
//
//        // Вручную заменяем бин для данного теста к контексте спринга
//        ReflectionTestUtils.setField(
//                mockMvc.getDispatcherServlet().getWebApplicationContext()
//                        .getBean(OrderController.class),
//                "orderService",
//                mockOrderService
//        );
//
//        mockMvc.perform(MockMvcRequestBuilders.post("/order/change")
//                        .param("product_id",product.getId().toString())
//                        .param("actionWithProduct", ActionWithProduct.DECREASE.toString())
//                        .flashAttr("filterRequest", filterRequest)
//                        .param("source","basket"))
//                .andExpect(status().is3xxRedirection())
//                .andExpect(redirectedUrl("/order/basket"));
//
//        verify(mockOrderService).updateProductInBasketBy(product.getId(),ActionWithProduct.DECREASE);
//
//        // Возвращаем обратно, чтобы другие тесты проходили
//        ReflectionTestUtils.setField(mockMvc.getDispatcherServlet().getWebApplicationContext()
//                        .getBean(OrderController.class),
//                "orderService",
//                orderService);
    }
    @Test
    public void changeProductInBasketFromProduct() throws Exception {
//        FilterRequest filterRequest = new FilterRequest();
//        Product product = productRepository.findAll().getFirst();
//
//        // Мокаем только для этого теста
//        OrderService mockOrderService = mock(OrderService.class);
//        doNothing().when(mockOrderService).updateProductInBasketBy(any(), any());
//
//        // Вручную заменяем бин для данного теста к контексте спринга
//        ReflectionTestUtils.setField(
//                mockMvc.getDispatcherServlet().getWebApplicationContext()
//                        .getBean(OrderController.class),
//                "orderService",
//                mockOrderService
//        );
//
//        mockMvc.perform(MockMvcRequestBuilders.post("/order/change")
//                        .param("product_id",product.getId().toString())
//                        .param("actionWithProduct", ActionWithProduct.INCREASE.toString())
//                        .flashAttr("filterRequest", filterRequest)
//                        .param("source","product"))
//                .andExpect(status().is3xxRedirection())
//                .andExpect(redirectedUrl("/product/" + product.getId()));
//
//        verify(mockOrderService).updateProductInBasketBy(product.getId(),ActionWithProduct.INCREASE);
//
//        // Возвращаем обратно, чтобы другие тесты проходили
//        ReflectionTestUtils.setField(mockMvc.getDispatcherServlet().getWebApplicationContext()
//                        .getBean(OrderController.class),
//                "orderService",
//                orderService);
    }
    @Test
    public void changeProductInBasketWithNoNameSource() throws Exception {
//        FilterRequest filterRequest = new FilterRequest();
//        Product product = productRepository.findAll().getFirst();
//
//        // Мокаем только для этого теста
//        OrderService mockOrderService = mock(OrderService.class);
//        doNothing().when(mockOrderService).updateProductInBasketBy(any(), any());
//
//        // Вручную заменяем бин для данного теста к контексте спринга
//        ReflectionTestUtils.setField(
//                mockMvc.getDispatcherServlet().getWebApplicationContext()
//                        .getBean(OrderController.class),
//                "orderService",
//                mockOrderService
//        );
//
//        mockMvc.perform(MockMvcRequestBuilders.post("/order/change")
//                        .param("product_id",product.getId().toString())
//                        .param("actionWithProduct", ActionWithProduct.INCREASE.toString())
//                        .flashAttr("filterRequest", filterRequest)
//                        .param("source", ""))
//                .andExpect(status().is3xxRedirection())
//                .andExpect(redirectedUrl("/"));
//
//        verify(mockOrderService).updateProductInBasketBy(product.getId(),ActionWithProduct.INCREASE);
//
//        // Возвращаем обратно, чтобы другие тесты проходили
//        ReflectionTestUtils.setField(mockMvc.getDispatcherServlet().getWebApplicationContext()
//                        .getBean(OrderController.class),
//                "orderService",
//                orderService);
    }
}
