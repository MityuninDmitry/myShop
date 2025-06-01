package ru.mityunin.myShop.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import ru.mityunin.myShop.model.Product;

public interface ProductRepository extends R2dbcRepository<Product, Long> {
}
