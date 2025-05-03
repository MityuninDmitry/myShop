package ru.mityunin.myShop.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mityunin.myShop.model.FilterRequest;
import ru.mityunin.myShop.model.Product;
import ru.mityunin.myShop.repository.ProductRepository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class ProductService {
    @Autowired
    private ProductRepository productRepository;

    public List<Product> findAll(FilterRequest filterRequest) {
        createTestProducts();
        List<Product> products = new ArrayList<>();
        Sort sort = Sort.by(Sort.Direction.fromString(filterRequest.sortDirection()), filterRequest.sortBy());
        Pageable pageable = PageRequest.of(filterRequest.page(), filterRequest.size(), sort);
        if (filterRequest.textFilter() == null || filterRequest.textFilter().isBlank()) {
            products = productRepository.findAll(pageable).stream().toList();
        } else  {
            products = productRepository.findByNameOrDescriptionContainingIgnoreCase(filterRequest.textFilter(),filterRequest.textFilter(), pageable).stream().toList();
        }

        return products.size() > 0 ? products : new ArrayList<>();
    }

    @Transactional
    public void createTestProducts() {
        productRepository.deleteAll();
        for (int i = 0; i < 50; i++) {
            Product product = new Product();
            product.setName("Name " + i);
            product.setPrice(BigDecimal.valueOf(i));
            product.setDescription("Some desc " + i);
            product.setImageUrl("https://images.hdqwalls.com/download/sunset-ronin-ghost-of-tsushima-40-2880x1800.jpg");

            productRepository.save(product);
        }
    }
}
