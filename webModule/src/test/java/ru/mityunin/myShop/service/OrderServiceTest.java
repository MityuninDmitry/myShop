package ru.mityunin.myShop.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.test.context.TestPropertySource;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.mityunin.myShop.SpringBootPostgreSQLBase;
import ru.mityunin.myShop.model.*;
import ru.mityunin.myShop.repository.OrderRepository;
import ru.mityunin.myShop.repository.ProductRepository;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestPropertySource(properties = "order.cache.ttl=2s")
public class OrderServiceTest extends SpringBootPostgreSQLBase {

    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private OrderService orderService;
    @Autowired
    private ReactiveRedisTemplate<String, Object> redisTemplate;
    @Autowired
    private ReactiveRedisConnectionFactory redisConnectionFactory;



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
    public void cacheProducts_shouldSaveDataToRedisAndReturnTheSame() {
        Order order = new Order();
        order.setStatus(OrderStatus.PRE_ORDER);
        order.setTotalPrice(BigDecimal.ZERO);
        Mono<Order> orderMono = orderRepository.save(order);
        String key = "orders:test_cache";
        List<Product> products = productRepository.findAll().collectList().block();
        orderService.cacheProducts(Mono.just(key), products).block();

        List<Product> rawCached = orderService.getProductsByOrderFromCache(Mono.just(key),orderMono).collectList().block();

        assertEquals(products.size(), rawCached.size());
        assertTrue(products.containsAll(rawCached) && rawCached.containsAll(products));
    }

    @Test
    public void shouldFindOrdersByDifferentOrderStatus() {
        Order order = new Order();
        order.setStatus(OrderStatus.PRE_ORDER);
        order.setTotalPrice(BigDecimal.ZERO);
        Mono<Order> orderMono = orderRepository.save(order);

        Order order1 = new Order();
        order1.setStatus(OrderStatus.PAID);
        order1.setTotalPrice(BigDecimal.ZERO);
        Mono<Order> orderMono1 =orderRepository.save(order1);

        Flux.zip(orderMono, orderMono1).blockLast();



        List<Order> orders = orderService.findOrdersBy(OrderStatus.PRE_ORDER).collectList().block();
        assertEquals(1,orders.size());
        assertEquals(OrderStatus.PRE_ORDER, orders.getFirst().getStatus());

        orders = orderService.findOrdersBy(OrderStatus.PAID).collectList().block();
        assertEquals(1,orders.size());
        assertEquals(OrderStatus.PAID, orders.getFirst().getStatus());
    }

    @Test
    public void shouldGetTotalPriceOrders() {
        List<Order> orders = Flux.range(1,3).flatMap(i -> {
            Order order = new Order();
            order.setStatus(OrderStatus.PAID);
            order.setTotalPrice(BigDecimal.valueOf(i));
            return Mono.fromCallable(()-> order);
        }).collectList().block();

        assertEquals(BigDecimal.valueOf(6),orderService.getTotalPriceOrders(orderRepository.saveAll(orders)).block());
    }

    @Test
    public void shouldGetProductsByOrder() {
        // воссоздаем схему при которой n orders имеет n заказанных продуктов
        List<Product> products1 = productRepository.findAll().collectList().block();
        List<Order> orders = Flux.range(1,3).flatMap(i -> {
            Order order = new Order();
            order.setStatus(OrderStatus.PAID);
            order.setTotalPrice(BigDecimal.valueOf(i));
            Flux.range(1,i).flatMap( j -> {
                OrderedProduct orderedProduct = new OrderedProduct();
                orderedProduct.setCount(1);

                orderedProduct.setProduct_id(products1.get(j).getId());
                order.getOrderedProducts().add(orderedProduct);
                return Mono.fromCallable(() -> order);
            }).subscribe();
            return orderRepository.save(order);
        }).collectList().block();
        orders.stream().forEach(order -> {
            List<Product> products = orderService.getProductsByOrder(Mono.just(order)).collectList().block();

            assertEquals(products.size(), order.getOrderedProducts().size());
        });

    }

    @Test
    public void shouldReturnBasketWhenBasketNotExists() {
        Order order = orderService.getBasket().block();
        assertEquals(order.getStatus(), OrderStatus.PRE_ORDER);
    }

    @Test
    public void shouldReturnBasketWhenBasketExists() {
        Order basket = new Order();
        basket.setStatus(OrderStatus.PRE_ORDER);
        basket.setTotalPrice(BigDecimal.valueOf(10));
        orderRepository.save(basket).block();

        Order order = orderService.getBasket().block();
        assertEquals(order.getStatus(), OrderStatus.PRE_ORDER);
        assertEquals(order.getTotalPrice().doubleValue(), BigDecimal.valueOf(10).doubleValue());

    }

    @Test
    public void shouldChangeOrderAndCreateNewBasket() {
        Mono<Order> orderMono = orderService.getBasket();
        Order basket = orderMono.block();
        basket.setTotalPrice(BigDecimal.valueOf(10));
        orderRepository.save(basket).subscribe();
        orderService.setPaidFor(basket.getId()).block();


        List<Order> orders = orderRepository.findAll().collectList().block();
        assertEquals(2, orders.size());
        Order newBasket = orders.stream().filter(o -> o.getStatus().equals(OrderStatus.PRE_ORDER)).findFirst().get();
        assertEquals(OrderStatus.PRE_ORDER, newBasket.getStatus());
        assertEquals(BigDecimal.ZERO.doubleValue(), newBasket.getTotalPrice().doubleValue());

        Order oldBasket = orders.stream().filter(o -> o.getStatus().equals(OrderStatus.PAID)).findFirst().get();
        assertEquals(OrderStatus.PAID, oldBasket.getStatus());
        assertEquals(BigDecimal.TEN.doubleValue(), oldBasket.getTotalPrice().doubleValue() );
    }

    @Test
    public void shouldIncreaseCountAndSetNewTotalPriceBasket() {
        orderService.getBasket().block();

        Product product = productRepository.findAll().blockFirst();

        orderService.updateProductInBasketBy(product.getId(), ActionWithProduct.INCREASE).block();

        assertEquals(product.getPrice().doubleValue(), orderService.getBasket().block().getTotalPrice().doubleValue());

        orderService.updateProductInBasketBy(product.getId(), ActionWithProduct.INCREASE).block();

        assertEquals(product.getPrice().multiply(BigDecimal.valueOf(2)).doubleValue(), orderService.getBasket().block().getTotalPrice().doubleValue());

        assertEquals(1, orderService.getBasket().block().getOrderedProducts().size());

    }
    @Test
    public void shouldDecreaseCountAndSetNewTotalPriceBasket() {
        // подготовка к тесту (в корзину складываем товар в количестве 2 штуки)
        Product product = productRepository.findAll().blockFirst();

        Order basket = orderService.getBasket().block();
        basket.setTotalPrice(product.getPrice().multiply(BigDecimal.valueOf(2)));

        OrderedProduct orderedProduct = new OrderedProduct();
        orderedProduct.setCount(2);
        orderedProduct.setProduct_id(product.getId());
        basket.getOrderedProducts().add(orderedProduct);
        orderRepository.save(basket).block();

        // уменьшаем количество постепенно и попутно делаем проверки
        orderService.updateProductInBasketBy(product.getId(), ActionWithProduct.DECREASE).block();
        assertEquals(product.getPrice().doubleValue(), orderService.getBasket().block().getTotalPrice().doubleValue());

        orderService.updateProductInBasketBy(product.getId(), ActionWithProduct.DECREASE).block();

        assertEquals(BigDecimal.ZERO.doubleValue(), orderService.getBasket().block().getTotalPrice().doubleValue());

        assertEquals(0, orderService.getBasket().block().getOrderedProducts().size());

    }

    @Test
    public void shouldGetOrderTotalPriceByOrderId() {
        Order order = new Order();
        order.setTotalPrice(BigDecimal.TEN);
        order.setStatus(OrderStatus.PAID);
        orderRepository.save(order).block();

        assertEquals(BigDecimal.TEN.doubleValue(), orderService.getOrderTotalPriceBy(order.getId()).block().doubleValue());
    }

    @Test
    public void getProductsByOrderFromCacheAndNotCache() {
        List<Product> products = productRepository.findAll().collectList().block();
        orderService.updateProductInBasketBy(products.get(0).getId(),ActionWithProduct.INCREASE).block();
        orderService.updateProductInBasketBy(products.get(1).getId(),ActionWithProduct.INCREASE).block();

        List<Product> testProducts = orderService.getProductsByOrderId(orderService.getBasket().block().getId()).collectList().block();

        assertEquals(2, testProducts.size());

        String cacheKey = "orders::" + orderService.getBasket().block().getId();
        List<Product> cachedProducts = orderService.getProductsByOrderFromCache(Mono.just(cacheKey), orderService.getBasket()).collectList().block();
        assertThat(cachedProducts).hasSize(2);

        await().atMost(10, SECONDS)
                .pollInterval(1, SECONDS)
                .untilAsserted(() ->
                        assertThat(orderService.getProductsByOrderFromCache(Mono.just(cacheKey),orderService.getBasket()).collectList().block())
                                .isEmpty()
                );

    }
}
