package ru.mityunin.myShop.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import reactor.core.publisher.Flux;
import ru.mityunin.myShop.model.Order;
import ru.mityunin.myShop.model.OrderStatus;

public interface OrderRepository extends R2dbcRepository<Order, Long> {
    @Query("""
        SELECT id, create_date_time, total_price, status, ordered_products 
        FROM orders 
        WHERE status = :status
        ORDER BY create_date_time desc
        """)
    Flux<Order> findByStatus(@Param("status") OrderStatus status);
    @Query("""
        SELECT id, create_date_time, total_price, status, ordered_products, username 
        FROM orders 
        WHERE status = :status and username = :username
        ORDER BY create_date_time desc
        """)
    Flux<Order> findByUsernameAndStatus(@Param("username") String username,@Param("status") OrderStatus status);
}
