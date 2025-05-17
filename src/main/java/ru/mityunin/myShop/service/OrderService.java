package ru.mityunin.myShop.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.mityunin.myShop.model.*;
import ru.mityunin.myShop.repository.OrderRepository;
import ru.mityunin.myShop.repository.ProductRepository;

import java.math.BigDecimal;
import java.util.*;

@Service
public class OrderService {

    private OrderRepository orderRepository;
    private ProductRepository productRepository;

    public OrderService(OrderRepository orderRepository, ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
    }

    public Flux<Order> findOrdersBy(OrderStatus orderStatus) {
        Sort sort = Sort.by(Sort.Direction.fromString("desc"), "createDateTime");
        return orderRepository.findByStatus(orderStatus);
    }

    public Mono<BigDecimal> getTotalPriceOrders(Flux<Order> orders) {
        return orders.map(Order::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public Flux<Product> getProductsByOrder(Mono<Order> orderMono) {
        return orderMono.flatMapMany(order ->
                productRepository.findAll() // получаем поток продуктов
                        .filterWhen( // фильтруем асинхронно, чтобы выполнить подсчет и отфильтровать
                                product -> Mono.fromCallable(() -> order.countInOrderedProductWith(product.getId()))
                                        .map(count -> count > 0)
                        )
                        .map(product -> { // проставляем свойство счетчика количества позиций продукта в корзине и возращаем продукт
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
    @Transactional
    public Mono<Order> getBasket() {
        // ищем заказы с типом корзина (должен быт всегда 1 штука, а если нет, то создать)
        return findOrdersBy(OrderStatus.PRE_ORDER).collectList()
                .flatMap(orders -> {
                    if (orders.isEmpty()) {
                        // Если корзины нет - создаем новую
                        return createBasket();
                    } else {
                        // Берем первую найденную корзину
                        return Mono.just(orders.get(0));
                    }
                });
    }

    @Transactional
    public Mono<Order> createBasket() {
        Order basket = new Order();
        basket.setStatus(OrderStatus.PRE_ORDER);
        basket.setTotalPrice(BigDecimal.ZERO);
        return orderRepository.save(basket);
    }

    @Transactional
    public Mono<Void> setPaidFor(Long order_id) {
        return orderRepository.findById(order_id)
                .flatMap(order1 -> {
                    order1.setStatus(OrderStatus.PAID);
                    return  orderRepository.save(order1);
                })
                .then(createBasket())
                .then();
    }

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

    // забираем продукты из корзины и возвращаем
    public Flux<Product> getBasketProducts() {
        return getProductsByOrder(getBasket());
    }

    // забираем продукты из корзины и возвращаем
    public BigDecimal getBasketPrice() {
        Order basket = getBasket().block();
        return basket.getTotalPrice();
    }

    public BigDecimal getOrderTotalPriceBy(Long order_id) {
        Order order = orderRepository.findById(order_id).block();
        return order.getTotalPrice();
    }
}
