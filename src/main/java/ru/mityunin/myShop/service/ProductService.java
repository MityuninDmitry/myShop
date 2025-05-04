package ru.mityunin.myShop.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mityunin.myShop.model.*;
import ru.mityunin.myShop.repository.OrderRepository;
import ru.mityunin.myShop.repository.OrderedProductRepository;
import ru.mityunin.myShop.repository.ProductRepository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ProductService {
    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderedProductRepository orderedProductRepository;
    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderService orderService;

    public List<Product> findAll(FilterRequest filterRequest) {
        List<Product> products;
        Sort sort = Sort.by(Sort.Direction.fromString(filterRequest.sortDirection()), filterRequest.sortBy());
        Pageable pageable = PageRequest.of(filterRequest.page(), filterRequest.size(), sort);
        if (filterRequest.textFilter() == null || filterRequest.textFilter().isBlank()) {
            products = productRepository.findAll(pageable).stream().toList();
        } else  {
            products = productRepository.findByNameOrDescriptionContainingIgnoreCase(filterRequest.textFilter(),filterRequest.textFilter(), pageable).stream().toList();
        }
        // проставляем количество
        for (Product product: products) {
            setCountInBasketFor(product);
        }
        return products.size() > 0 ? products : new ArrayList<>();
    }

    @Transactional
    public void createTestProducts() {
        productRepository.deleteAll();
        orderRepository.deleteAll();
        orderedProductRepository.deleteAll();

        for (int i = 0; i < 50; i++) {
            Product product = new Product();
            product.setName("Name " + i);
            product.setPrice(BigDecimal.valueOf(i));
            product.setDescription("Some desc " + i);
            product.setImageUrl("https://images.hdqwalls.com/download/sunset-ronin-ghost-of-tsushima-40-2880x1800.jpg");

            productRepository.save(product);
        }
    }

    // проставление свойства на товаре countInBasket - для отображения в интерфейсе (не для БД)
    public void setCountInBasketFor(Product product) {
        Order basket = orderService.getBasket();
        if (basket.getOrderedProducts() == null)  {
            product.setCountInBasket(0);
        } else {
            Optional<OrderedProduct> orderedProduct = basket.getOrderedProducts().stream()
                    .filter(p -> p.getProduct().getId().equals(product.getId()))
                    .findFirst();

            if (orderedProduct.isPresent()) {
                product.setCountInBasket(orderedProduct.get().getCount());
            } else  {
                product.setCountInBasket(0);
            }
        }
    }
}
