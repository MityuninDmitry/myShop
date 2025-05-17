package ru.mityunin.myShop.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import ru.mityunin.myShop.model.Product;

public interface ProductRepository extends R2dbcRepository<Product, Long> {
    Flux<Product> findByNameOrDescriptionContainingIgnoreCase(String name, String description, Pageable pageable);
    default Flux<Product> findAll(Pageable pageable) {
        return findAll()
                .skip(pageable.getOffset())
                .take(pageable.getPageSize());
    }
}
