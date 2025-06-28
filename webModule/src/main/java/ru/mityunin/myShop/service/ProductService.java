package ru.mityunin.myShop.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.mityunin.myShop.controller.DTO.FilterRequest;
import ru.mityunin.myShop.model.Order;
import ru.mityunin.myShop.model.Product;
import ru.mityunin.myShop.repository.OrderRepository;
import ru.mityunin.myShop.repository.ProductCustomRepository;
import ru.mityunin.myShop.repository.ProductRepository;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Map;


@Service
public class ProductService {
    private static final Logger log = LoggerFactory.getLogger(ProductService.class);
    private static final String PRODUCT_CACHE_PREFIX = "products:";
    @Value("${product.cache.ttl:1m}")
    private Duration CACHE_TTL;

    private ProductRepository productRepository;
    private ProductCustomRepository productCustomRepository;
    private OrderRepository orderRepository;
    private OrderService orderService;
    private final ReactiveRedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper; // Добавляем ObjectMapper


    public ProductService(ProductRepository productRepository, ProductCustomRepository productCustomRepository, OrderRepository orderRepository, OrderService orderService, ReactiveRedisTemplate<String, Object> redisTemplate, ObjectMapper objectMapper) {
        this.productRepository = productRepository;
        this.productCustomRepository = productCustomRepository;
        this.orderRepository = orderRepository;
        this.orderService = orderService;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public Mono<Boolean> cacheProducts(String key, List<Product> products) {
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
                .sort(new Comparator<Product>() {
                    @Override
                    public int compare(Product o1, Product o2) {
                        if (filterRequest.sortBy().equals("name")) {
                            if (filterRequest.sortDirection().equals("asc")) {
                                return o1.getName().compareTo(o2.getName());
                            }
                            else return o2.getName().compareTo(o1.getName());
                        } else {
                            if (filterRequest.sortDirection().equals("asc")) {
                                return o1.getPrice() .compareTo(o2.getPrice());
                            } else return o2.getPrice().compareTo(o1.getPrice());
                        }
                    }
                })
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
        log.info("findAllFromDB before get basket");
        return orderService.getBasket()
                .doOnNext(value -> log.info("findAllFromDB after get basket"))
                .flatMapMany(basket ->
                        products.flatMap(product ->
                                Mono.fromCallable(() -> basket.countInOrderedProductWith(product.getId()))
                                        .map(count -> {
                                            product.setCountInBasket(count);
                                            return product;
                                        })
                        ))
                .switchIfEmpty(products.map(p -> { // для анонимных
                    p.setCountInBasket(0);
                    return p;
                }));
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
        return orderService.getBasket().defaultIfEmpty(Order.emptyOrder()).map(order -> {
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
