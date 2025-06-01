package ru.mityunin.myShop.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

@Service
public class PayService {
    private OrderService orderService;

    public PayService(OrderService orderService) {
        this.orderService = orderService;
    }

    @Transactional
    public Mono<Void> setPaidFor(Long order_id) {
        return orderService.setPaidFor(order_id);
    }
}
