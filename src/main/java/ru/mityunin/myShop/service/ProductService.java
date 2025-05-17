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
import ru.mityunin.myShop.repository.ProductRepository;

import java.math.BigDecimal;

@Service
public class ProductService {

    private ProductRepository productRepository;
    private OrderRepository orderRepository;
    private OrderService orderService;

    public ProductService(ProductRepository productRepository, OrderRepository orderRepository, OrderService orderService) {
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.orderService = orderService;
    }

    public Flux<Product> findAll(FilterRequest filterRequest) {
        Flux<Product> products;
        Sort sort = Sort.by(Sort.Direction.fromString(filterRequest.sortDirection()), filterRequest.sortBy());
        Pageable pageable = PageRequest.of(filterRequest.page(), filterRequest.size(), sort);

        if (filterRequest.textFilter() == null || filterRequest.textFilter().isBlank()) {
            products = productRepository.findAll(pageable);
        } else  {
            products = productRepository.findByNameOrDescriptionContainingIgnoreCase(filterRequest.textFilter(),filterRequest.textFilter(), pageable);
        }

        Order basket = orderService.getBasket().block();

        return products.map(product -> {
            product.setCountInBasket(basket.countInOrderedProductWith(product.getId()));
            return product;
        });
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
                    product.setImageUrl("https://example.com/image.jpg");
                    return productRepository.save(product);
                })
                .then();

    }

    // проставление свойства на товаре countInBasket - для отображения в интерфейсе (не для БД)
    public void setCountInBasketFor(Product product) {
        Order basket = orderService.getBasket().block();
        product.setCountInBasket(basket.countInOrderedProductWith(product.getId()));
    }

    public Mono<Product> getProductBy(Long id) {
        return  productRepository.findById(id).map(p -> {
            setCountInBasketFor(p);
            return p;
        });
    }
}
