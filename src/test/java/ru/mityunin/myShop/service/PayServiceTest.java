package ru.mityunin.myShop.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import ru.mityunin.myShop.SpringBootPostgreSQLBase;
import ru.mityunin.myShop.model.Order;
import ru.mityunin.myShop.model.OrderStatus;
import ru.mityunin.myShop.model.Product;
import ru.mityunin.myShop.repository.OrderRepository;
import ru.mityunin.myShop.repository.OrderedProductRepository;
import ru.mityunin.myShop.repository.ProductRepository;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PayServiceTest extends SpringBootPostgreSQLBase {
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private OrderedProductRepository orderedProductRepository;

    @Autowired
    private PayService payService;

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
    @Transactional
    public void shouldSetPaidForOrder() {
        Order order = new Order();
        order.setStatus(OrderStatus.PRE_ORDER);
        order.setTotalPrice(BigDecimal.TEN);

        orderRepository.save(order);

        payService.setPaidFor(order.getId());

        assertEquals(OrderStatus.PAID, orderRepository.findAll().stream().filter(o -> o.getStatus().equals(OrderStatus.PAID)).findFirst().get().getStatus());
        assertEquals(2, orderRepository.findAll().size());
    }
}
