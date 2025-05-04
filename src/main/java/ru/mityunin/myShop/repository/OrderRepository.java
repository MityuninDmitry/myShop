package ru.mityunin.myShop.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.mityunin.myShop.model.Order;
import ru.mityunin.myShop.model.OrderStatus;

public interface OrderRepository extends JpaRepository<Order, Long> {
    Page<Order> findAllByStatus(OrderStatus orderStatus, Pageable pageable);
}
