package ru.mityunin.myShop.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

@Service
public class ProductService {

    private ProductRepository productRepository;
    private ProductCustomRepository productCustomRepository;
    private OrderRepository orderRepository;
    private OrderService orderService;

    public ProductService(ProductRepository productRepository, ProductCustomRepository productCustomRepository, OrderRepository orderRepository, OrderService orderService) {
        this.productRepository = productRepository;
        this.productCustomRepository = productCustomRepository;
        this.orderRepository = orderRepository;
        this.orderService = orderService;
    }


    public Flux<Product> findAll(FilterRequest filterRequest) {
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
        return productRepository.findById(id)
                .flatMap(product ->
                        setCountInBasketFor(product)
                                .onErrorResume(e -> {
                                    return Mono.just(product); // Возвращаем продукт без количества при ошибке
                                })
                );
    }
}
