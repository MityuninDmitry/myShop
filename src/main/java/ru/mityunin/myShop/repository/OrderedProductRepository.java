package ru.mityunin.myShop.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.mityunin.myShop.model.OrderedProduct;

import java.util.List;

public interface OrderedProductRepository extends JpaRepository<OrderedProduct, Long> {
}
