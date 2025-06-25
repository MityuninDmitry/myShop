package ru.mityunin.myShop.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Table(name = "orders")
public class Order {
    @Id
    private Long id;

    @Column("create_date_time")
    private LocalDateTime createDateTime = LocalDateTime.now();

    @Column("total_price")
    private BigDecimal totalPrice;

    @Column("status")
    private OrderStatus status;

    @Column("ordered_products")
    private List<OrderedProduct> orderedProducts = new ArrayList<>();

    @Column("username")
    private String username;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getCreateDateTime() {
        return createDateTime;
    }

    public void setCreateDateTime(LocalDateTime createDateTime) {
        this.createDateTime = createDateTime;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public List<OrderedProduct> getOrderedProducts() {
        return orderedProducts;
    }

    public void setOrderedProducts(List<OrderedProduct> orderedProducts) {
        this.orderedProducts = orderedProducts;
    }

    public Integer countInOrderedProductWith(Long productId) {
        return orderedProducts.stream()
                .filter(orderedProduct -> orderedProduct.getProduct_id().equals(productId))
                .findFirst()
                .orElseGet(() -> {
            OrderedProduct orderedProduct = new OrderedProduct();
            orderedProduct.setCount(0);
            return orderedProduct;
        }).getCount();
    }
}
