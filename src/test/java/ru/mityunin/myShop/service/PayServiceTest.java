package ru.mityunin.myShop.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Flux;
import ru.mityunin.myShop.SpringBootPostgreSQLBase;
import ru.mityunin.myShop.model.Order;
import ru.mityunin.myShop.model.OrderStatus;
import ru.mityunin.myShop.model.Product;
import ru.mityunin.myShop.repository.OrderRepository;
import ru.mityunin.myShop.repository.ProductRepository;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PayServiceTest extends SpringBootPostgreSQLBase {
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PayService payService;

    @BeforeEach
    public void createTestData() {
        Flux.zip(productRepository.deleteAll(), orderRepository.deleteAll())
                .thenMany(Flux.range(1,51).flatMap( i ->
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
    public void shouldSetPaidForOrder() {
        Order order = new Order();
        order.setStatus(OrderStatus.PRE_ORDER);
        order.setTotalPrice(BigDecimal.TEN);
        orderRepository.save(order).block();

        payService.setPaidFor(order.getId()).block();

        assertEquals(OrderStatus.PAID, orderRepository.findAll().collectList().block().stream().filter(o -> o.getStatus().equals(OrderStatus.PAID)).findFirst().get().getStatus());
        assertEquals(2, orderRepository.findAll().collectList().block().size());
    }
}
