package ru.mityunin.myShop.service;

import org.aspectj.weaver.ast.Or;
import org.junit.Before;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import ru.mityunin.myShop.SpringBootPostgreSQLBase;
import ru.mityunin.myShop.model.*;
import ru.mityunin.myShop.repository.OrderRepository;
import ru.mityunin.myShop.repository.OrderedProductRepository;
import ru.mityunin.myShop.repository.ProductRepository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class OrderServiceTest extends SpringBootPostgreSQLBase {

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
    @Transactional
    public void shouldFindOrdersByDifferentOrderStatus() {
        Order order = new Order();
        order.setStatus(OrderStatus.PRE_ORDER);
        order.setTotalPrice(BigDecimal.ZERO);
        orderRepository.save(order);

        Order order1 = new Order();
        order1.setStatus(OrderStatus.PAID);
        order1.setTotalPrice(BigDecimal.ZERO);
        orderRepository.save(order1);

        List<Order> orders = orderService.findOrdersBy(OrderStatus.PRE_ORDER);
        assertEquals(1,orders.size());
        assertEquals(OrderStatus.PRE_ORDER, orders.getFirst().getStatus());

        orders = orderService.findOrdersBy(OrderStatus.PAID);
        assertEquals(1,orders.size());
        assertEquals(OrderStatus.PAID, orders.getFirst().getStatus());
    }

    @Test
    @Transactional
    public void shouldGetTotalPriceOrders() {
        List<Order> orders = new ArrayList<>();
        for (int i = 1; i < 4; i++) {
            Order order = new Order();
            order.setStatus(OrderStatus.PAID);
            order.setTotalPrice(BigDecimal.valueOf(i));
            orders.add(order);
        }
        orderRepository.saveAll(orders);

        assertEquals(BigDecimal.valueOf(6),orderService.getTotalPriceOrders(orders));
    }

    @Test
    @Transactional
    public void shouldGetProductsByOrder() {
        // воссоздаем схему при которой n orders имеет n заказанных продуктов
        List<Order> orders = new ArrayList<>();
        for (int i = 1; i < 4; i++) {
            Order order = new Order();
            order.setStatus(OrderStatus.PAID);
            order.setTotalPrice(BigDecimal.valueOf(i));
            orders.add(order);
            List<OrderedProduct> orderedProducts = new ArrayList<>();
            for (int j = 0; j < i; j++) {

                OrderedProduct orderedProduct = new OrderedProduct();
                orderedProduct.setOrder(order);
                orderedProduct.setProduct(productRepository.findAll().stream().findAny().get());
                orderedProduct.setCount(1);
                orderedProducts.add(orderedProduct);

            }
            order.setOrderedProducts(orderedProducts);
            orderRepository.saveAll(orders);
        }
        // проверяем, что сервис вернул столько, сколько должно быть
        for (Order order: orders) {
            List<Product> products = orderService.getProductsByOrder(order);
            assertEquals(products.size(), order.getOrderedProducts().size());
        }
    }

    @Test
    @Transactional
    public void shouldReturnBasketWhenBasketNotExists() {
        Order order = orderService.getBasket();
        assertEquals(order.getStatus(), OrderStatus.PRE_ORDER);
    }

    @Test
    @Transactional
    public void shouldReturnBasketWhenBasketExists() {
        Order basket = new Order();
        basket.setStatus(OrderStatus.PRE_ORDER);
        basket.setTotalPrice(BigDecimal.valueOf(10));
        orderRepository.save(basket);

        Order order = orderService.getBasket();
        assertEquals(order.getStatus(), OrderStatus.PRE_ORDER);
        assertEquals(order.getTotalPrice().doubleValue(), BigDecimal.valueOf(10).doubleValue());

    }

    @Test
    @Transactional
    public void shouldChangeOrderAndCreateNewBasket() {
        Order basket = new Order();
        basket.setStatus(OrderStatus.PRE_ORDER);
        basket.setTotalPrice(BigDecimal.valueOf(10));
        orderRepository.save(basket);

        orderService.setPaidFor(basket.getId());

        List<Order> orders = orderRepository.findAll();
        assertEquals(2, orders.size());
        Order newBasket = orders.stream().filter(o -> o.getStatus().equals(OrderStatus.PRE_ORDER)).findFirst().get();
        assertEquals(OrderStatus.PRE_ORDER, newBasket.getStatus());
        assertEquals(BigDecimal.ZERO.doubleValue(), newBasket.getTotalPrice().doubleValue());

        Order oldBasket = orders.stream().filter(o -> o.getStatus().equals(OrderStatus.PAID)).findFirst().get();
        assertEquals(OrderStatus.PAID, oldBasket.getStatus());
        assertEquals(BigDecimal.TEN.doubleValue(), oldBasket.getTotalPrice().doubleValue() );
    }

    @Test
    @Transactional
    public void shouldIncreaseCountAndSetNewTotalPriceBasket() {
        Order basket = new Order();
        basket.setStatus(OrderStatus.PRE_ORDER);
        basket.setTotalPrice(BigDecimal.ZERO);
        orderRepository.save(basket);

        Product product = productRepository.findAll().getFirst();

        orderService.updateProductInBasketBy(product.getId(), ActionWithProduct.INCREASE);

        assertEquals(product.getPrice().doubleValue(), orderService.getBasket().getTotalPrice().doubleValue());

        orderService.updateProductInBasketBy(product.getId(), ActionWithProduct.INCREASE);

        assertEquals(product.getPrice().multiply(BigDecimal.valueOf(2)).doubleValue(), orderService.getBasket().getTotalPrice().doubleValue());

        assertEquals(1, orderService.getBasket().getOrderedProducts().size());

    }
    @Test
    @Transactional
    public void shouldDecreaseCountAndSetNewTotalPriceBasket() {
        // подготовка к тесту (в корзину складываем товар в количестве 2 штуки)
        Product product = productRepository.findAll().getFirst();

        Order basket = new Order();
        basket.setStatus(OrderStatus.PRE_ORDER);
        basket.setTotalPrice(product.getPrice().multiply(BigDecimal.valueOf(2)));

        OrderedProduct orderedProduct = new OrderedProduct();
        orderedProduct.setCount(2);
        orderedProduct.setOrder(basket);
        orderedProduct.setProduct(product);

        basket.getOrderedProducts().add(orderedProduct);
        orderRepository.save(basket);

        // уменьшаем количество постепенно и попутно делаем проверки
        orderService.updateProductInBasketBy(product.getId(), ActionWithProduct.DECREASE);
        assertEquals(product.getPrice().doubleValue(), orderService.getBasket().getTotalPrice().doubleValue());

        orderService.updateProductInBasketBy(product.getId(), ActionWithProduct.DECREASE);

        assertEquals(BigDecimal.ZERO.doubleValue(), orderService.getBasket().getTotalPrice().doubleValue());

        assertEquals(0, orderService.getBasket().getOrderedProducts().size());

    }

    @Test
    @Transactional
    public void shouldGetOrderTotalPriceByOrderId() {
        Order order = new Order();
        order.setTotalPrice(BigDecimal.TEN);
        order.setStatus(OrderStatus.PAID);
        orderRepository.save(order);

        assertEquals(BigDecimal.TEN.doubleValue(), orderService.getOrderTotalPriceBy(order.getId()).doubleValue());
    }

}
