package ru.mityunin.myShop.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import ru.mityunin.myShop.model.OrderedProduct;

public interface OrderedProductRepository extends R2dbcRepository<OrderedProduct, Long> {
}
