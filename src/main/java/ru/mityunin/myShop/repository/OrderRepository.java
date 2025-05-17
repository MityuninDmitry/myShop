package ru.mityunin.myShop.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import ru.mityunin.myShop.model.Order;
import ru.mityunin.myShop.model.OrderStatus;

public interface OrderRepository extends R2dbcRepository<Order, Long> {
    Flux<Order> findByStatus(OrderStatus orderStatus);
}
