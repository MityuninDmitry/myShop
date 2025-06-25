package ru.mityunin.myShop.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.mityunin.myShop.model.*;
import ru.mityunin.myShop.repository.OrderRepository;
import ru.mityunin.myShop.repository.ProductRepository;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class OrderService {
    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    private final ConcurrentHashMap<String, Mono<Order>> basketCreationLocks = new ConcurrentHashMap<>();

    private static final String CACHE_PREFIX = "orders:";
    @Value("${order.cache.ttl:1m}")
    private Duration CACHE_TTL;

    private OrderRepository orderRepository;
    private ProductRepository productRepository;
    private final ReactiveRedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper; // Добавляем ObjectMapper
    private AuthService authService;

    public OrderService(OrderRepository orderRepository, ProductRepository productRepository, ReactiveRedisTemplate<String, Object> redisTemplate, ObjectMapper objectMapper, AuthService authService) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.authService = authService;
    }

    public Mono<Boolean> cacheProducts(Mono<String> keyMono, List<Product> products) {
        return keyMono.flatMap(key -> redisTemplate.opsForValue()
                .set(key, products, CACHE_TTL));
    }

    public Flux<Order> findOrdersBy(String userName, OrderStatus orderStatus) {
        return orderRepository.findByUsernameAndStatus(userName,orderStatus);
    }

    public Mono<BigDecimal> getTotalPriceOrders(Flux<Order> orders) {
        return orders.map(Order::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public Flux<Product> getProductsByOrder(Mono<Order> orderMono) {
        Mono<String> cacheKeyMono = orderMono.map(order -> CACHE_PREFIX + ":" + order.getId().toString());

        return getProductsByOrderFromCache(cacheKeyMono, orderMono)
                .switchIfEmpty(
                        getProductsByOrderFromDB(orderMono)
                                .collectList()
                                .flatMapMany(products ->
                                        cacheProducts(cacheKeyMono, products).thenMany(Flux.fromIterable(products)
                                        )))
                .sort(Comparator.comparing(Product::getId));
    }
    public Mono<Boolean> clearCacheForOrder(Order order) {
        String cacheKeyMono = CACHE_PREFIX + ":" + order.getId();
        return redisTemplate.opsForValue().delete(cacheKeyMono);
    }
    public Flux<Product> getProductsByOrderFromCache(Mono<String> cacheKeyMono,Mono<Order> orderMono) {
        return cacheKeyMono.flatMapMany(cacheKey -> Mono.zip(
                redisTemplate.opsForValue().get(cacheKey), orderMono)
                .flatMapMany(tuple -> {
                    if (tuple.getT1() instanceof List<?> list) {
                            return Flux.fromIterable(list)
                                    .flatMap(item -> {
                                        if (item instanceof Product) {
                                            return Mono.just((Product) item);
                                        } else if (item instanceof Map) {
                                            // Конвертируем LinkedHashMap в Product
                                            return Mono.fromCallable(() ->
                                                    objectMapper.convertValue(item, Product.class)
                                            );
                                        }
                                        return Mono.empty();
                                    })
                                    .map(product -> {
                                        Integer count = tuple.getT2().countInOrderedProductWith(product.getId());
                                        product.setCountInBasket(count);
                                        return product;
                                    });
                    }
                    return Flux.empty();
                })
        );
    }
    public Flux<Product> getProductsByOrderFromDB(Mono<Order> orderMono) {
        return orderMono.flatMapMany(order ->
                productRepository.findAll()
                        .filterWhen(
                                product -> Mono.fromCallable(() -> order.countInOrderedProductWith(product.getId()))
                                        .map(count -> count > 0)
                        )
                        .map(product -> {
                            Integer count = order.countInOrderedProductWith(product.getId());
                            product.setCountInBasket(count);
                            return product;
                        })
        );
    }
    public Flux<Product> getProductsByOrderId(Long order_id) {
        Mono<Order> orderMono = orderRepository.findById(order_id);
        return getProductsByOrder(orderMono);
    }

//    @Transactional
//    public Mono<Order> getBasket() {
//        return authService.getCurrentUsername()
//                .flatMap(userName ->
//                        findOrdersBy(userName,OrderStatus.PRE_ORDER).collectList()
//                .flatMap(orders -> {
//                    if (orders.isEmpty()) {
//                        // Если корзины нет - создаем новую
//
//                        return createBasket(userName);
//                    } else {
//                        // Берем первую найденную корзину
//
//                        return Mono.just(orders.get(0));
//                    }
//                })) ;
//    }

    public Mono<Order> getBasket() {
        return authService.getCurrentUsername()
                .flatMap(userName -> basketCreationLocks
                        .computeIfAbsent(userName, un -> createBasketWithLock(un).cache())
                );
    }

    private Mono<Order> createBasketWithLock(String userName) {
        return Mono.defer(() -> findOrdersBy(userName, OrderStatus.PRE_ORDER)
                .collectList()
                .flatMap(orders -> {
                    if (orders.isEmpty()) {
                        return createBasket(userName)
                                .onErrorResume(e -> {
                                    basketCreationLocks.remove(userName);
                                    return Mono.error(e);
                                });
                    }
                    return Mono.just(orders.get(0));
                })
                .doFinally(signal -> basketCreationLocks.remove(userName)));
    }

    @Transactional
    public Mono<Order> createBasket(String userName) {
        Order basket = new Order();
        basket.setStatus(OrderStatus.PRE_ORDER);
        basket.setTotalPrice(BigDecimal.ZERO);
        basket.setUsername(userName);
        return orderRepository.save(basket);
    }

    @Transactional
    public Mono<Void> setPaidFor(String userName, Long order_id) {
        return orderRepository.findById(order_id)
                .flatMap(order1 -> {
                    order1.setStatus(OrderStatus.PAID);
                    return orderRepository.save(order1);
                })
                .then(createBasket(userName))
                .then();
    }

    @Transactional
    public Mono<Void> updateProductInBasketBy(Long product_id, ActionWithProduct action) {
        return getBasket()
                .flatMap(basket -> Mono.zip(
                        Mono.just(basket),
                        productRepository.findById(product_id),
                        Mono.fromCallable(() -> basket.countInOrderedProductWith(product_id))
                ))
                .flatMap(tuple -> {
                    Order basket = tuple.getT1();
                    Product product = tuple.getT2();
                    int currentCount = tuple.getT3();

                    return updateBasketContents(basket, product, currentCount, action)
                            .then(calculateTotalPrice(basket))
                            .then(orderRepository.save(basket));
                })
                .then();
    }

    private Mono<Void> updateBasketContents(Order basket, Product product, int currentCount, ActionWithProduct action) {
        return Mono.fromRunnable(() -> {
            if (action == ActionWithProduct.INCREASE) {
                addOrIncreaseProduct(basket, product, currentCount);

            } else {
                decreaseOrRemoveProduct(basket, product.getId(), currentCount);
            }
            clearCacheForOrder(basket).subscribe();
        });
    }

    private void addOrIncreaseProduct(Order basket, Product product, int currentCount) {
        basket.getOrderedProducts().stream()
                .filter(op -> op.getProduct_id().equals(product.getId()))
                .findFirst()
                .ifPresentOrElse(
                        op -> op.setCount(currentCount + 1),
                        () -> {
                            OrderedProduct newOp = new OrderedProduct();
                            newOp.setProduct_id(product.getId());
                            newOp.setCount(1);
                            basket.getOrderedProducts().add(newOp);
                        }
                );
    }

    private void decreaseOrRemoveProduct(Order basket, Long productId, int currentCount) {
        if (currentCount > 1) {
            basket.getOrderedProducts().stream()
                    .filter(op -> op.getProduct_id().equals(productId))
                    .findFirst()
                    .ifPresent(op -> op.setCount(currentCount - 1));
        } else {
            basket.getOrderedProducts().removeIf(op -> op.getProduct_id().equals(productId));
        }
    }

    private Mono<BigDecimal> calculateTotalPrice(Order basket) {
        return Flux.fromIterable(basket.getOrderedProducts())
                .flatMap(op -> productRepository.findById(op.getProduct_id())
                        .map(product -> product.getPrice().multiply(BigDecimal.valueOf(op.getCount())))
                )
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .doOnNext(basket::setTotalPrice);
    }

    public Flux<Product> getBasketProducts() {
        return getProductsByOrder(getBasket());
    }

    public Mono<BigDecimal> getBasketPrice() {
        return getBasket().flatMap(order ->
            Mono.just(order.getTotalPrice())
        );
    }

    public Mono<BigDecimal> getOrderTotalPriceBy(Long order_id) {
        return orderRepository.findById(order_id).flatMap(order ->
                Mono.just(order.getTotalPrice())
        );
    }
}
