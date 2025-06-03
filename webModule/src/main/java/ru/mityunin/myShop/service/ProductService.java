package ru.mityunin.myShop.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.mityunin.myShop.controller.DTO.FilterRequest;
import ru.mityunin.myShop.model.*;
import ru.mityunin.myShop.repository.OrderRepository;
import ru.mityunin.myShop.repository.ProductCustomRepository;
import ru.mityunin.myShop.repository.ProductRepository;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;


@Service
public class ProductService {
    private static final String PRODUCT_CACHE_PREFIX = "products:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(1);

    private ProductRepository productRepository;
    private ProductCustomRepository productCustomRepository;
    private OrderRepository orderRepository;
    private OrderService orderService;
    private final ReactiveRedisTemplate<String, Object> redisTemplate;

    public ProductService(ProductRepository productRepository, ProductCustomRepository productCustomRepository, OrderRepository orderRepository, OrderService orderService, ReactiveRedisTemplate<String, Object> redisTemplate) {
        this.productRepository = productRepository;
        this.productCustomRepository = productCustomRepository;
        this.orderRepository = orderRepository;
        this.orderService = orderService;
        this.redisTemplate = redisTemplate;
    }

    private Mono<Boolean> cacheProducts(String key, List<Product> products) {
        return redisTemplate.opsForValue()
                .set(key, products, CACHE_TTL);
    }
    private Mono<Boolean> cacheProduct(String key, Product product) {
        return redisTemplate.opsForValue()
                .set(key, product, CACHE_TTL);
    }

    public Flux<Product> findAll(FilterRequest filterRequest) {
        String cacheKey = PRODUCT_CACHE_PREFIX + filterRequest.cacheKey();
        return findAllFromCache(cacheKey)
                .switchIfEmpty(
                        findAllFromDB(filterRequest)
                                .collectList()
                                .flatMapMany(products ->
                                        cacheProducts(cacheKey, products).thenMany(Flux.fromIterable(products)
                                )));
    }
    public Flux<Product> findAllFromCache(String cacheKey) {
        return redisTemplate.opsForValue().get(cacheKey)
                .flatMapMany(obj -> {
                    if (obj instanceof List<?> list) {
                        return Flux.fromIterable(list)
                                .filter(Product.class::isInstance)
                                .map(Product.class::cast)
                                .flatMap(this::setCountInBasketFor);
                    }
                    return Flux.empty();
                });
    }
    public Flux<Product> findAllFromDB(FilterRequest filterRequest) {
        Sort sort = Sort.by(Sort.Direction.fromString(filterRequest.sortDirection()), filterRequest.sortBy());
        Pageable pageable = PageRequest.of(filterRequest.page(), filterRequest.size(), sort);
        Flux<Product> products = filterRequest.textFilter() == null || filterRequest.textFilter().isBlank() ?
                productCustomRepository.findAll(pageable):
                productCustomRepository.findAllFiltered(filterRequest.textFilter(),pageable);

        return orderService.getBasket()
                .flatMapMany(basket ->
                        products.flatMap(product ->
                                Mono.fromCallable(() -> basket.countInOrderedProductWith(product.getId()))
                                        .map(count -> {
                                            product.setCountInBasket(count);
                                            return product;
                                        })
                        ));
    }

    @Transactional
    public Mono<Void> createTestProducts() {
        return Mono.zip(productRepository.deleteAll(),orderRepository.deleteAll())
                .thenMany(Flux.range(0,50))
                .flatMap(i -> {
                    Product product = new Product();
                    product.setName("Name " + i);
                    product.setPrice(BigDecimal.valueOf(i));
                    product.setDescription("Some desc " + i);
                    product.setImageUrl("https://images.hdqwalls.com/download/sunset-ronin-ghost-of-tsushima-40-2880x1800.jpg");
                    return productRepository.save(product);
                })
                .then();

    }

    // проставление свойства на товаре countInBasket - для отображения в интерфейсе (не для БД)
    public Mono<Product> setCountInBasketFor(Product product) {
        return orderService.getBasket().map(order -> {
            int count = order.countInOrderedProductWith(product.getId());
            product.setCountInBasket(count);
            return product;
        });

    }

    public Mono<Product> getProductBy(Long id) {
        String cacheKey = PRODUCT_CACHE_PREFIX + ":" + id;
        return getProductByFromCache(cacheKey)
                .switchIfEmpty(
                        getProductByFromDB(id)
                                .flatMap(product ->
                                        cacheProduct(cacheKey, product).then(Mono.fromCallable(() -> product)
                                        )));
    }
    public Mono<Product> getProductByFromDB(Long id) {
        return productRepository.findById(id)
                .flatMap(product ->
                        setCountInBasketFor(product)
                                .onErrorResume(e -> {
                                    return Mono.just(product); // Возвращаем продукт без количества при ошибке
                                })
                );
    }
    public Mono<Product> getProductByFromCache(String cacheKey) {
        return redisTemplate.opsForValue().get(cacheKey)
                .flatMap(obj -> {
                    if (obj instanceof Product) {
                        return Mono.fromCallable(() -> obj)
                                .filter(Product.class::isInstance)
                                .map(Product.class::cast)
                                .flatMap(this::setCountInBasketFor);
                    }
                    return Mono.empty();
                });
    }
}
