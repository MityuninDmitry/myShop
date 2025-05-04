package ru.mityunin.myShop.model;

import jakarta.persistence.*;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "products")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "name")
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "price", precision = 19, scale = 2, nullable = false)
    private BigDecimal price;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderedProduct> orderedProducts = new ArrayList<>();

    @Transient
    private Integer countInBasket;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public List<OrderedProduct> getOrderedProducts() {
        return orderedProducts;
    }

    public void setOrderedProducts(List<OrderedProduct> orderedProducts) {
        this.orderedProducts = orderedProducts;
    }

    public Integer getCountInBasket() {
        return countInBasket;
    }

    public void setCountInBasket(Integer countInBasket) {
        this.countInBasket = countInBasket;
    }
}
