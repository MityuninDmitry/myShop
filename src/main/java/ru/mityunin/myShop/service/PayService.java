package ru.mityunin.myShop.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import ru.mityunin.myShop.model.Order;
import ru.mityunin.myShop.model.OrderStatus;
import ru.mityunin.myShop.repository.OrderRepository;

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
