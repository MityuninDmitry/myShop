package ru.mityunin.myShop.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mityunin.myShop.model.*;
import ru.mityunin.myShop.repository.OrderRepository;
import ru.mityunin.myShop.repository.ProductRepository;

import java.math.BigDecimal;
import java.util.*;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    public List<Order> findOrdersBy(OrderStatus orderStatus) {
        Sort sort = Sort.by(Sort.Direction.fromString("desc"), "createDateTime");
        Pageable pageable = PageRequest.of(0,1000, sort);
        return orderRepository.findAllByStatus(orderStatus, pageable).toList();
    }

    public BigDecimal getTotalPriceOrders(List<Order> orders) {
        BigDecimal totalPrice = BigDecimal.ZERO;
        for (Order order: orders) {
            totalPrice = totalPrice.add(order.getTotalPrice());
        }
        return totalPrice;
    }

    public List<Product> getProductsByOrder(Order order) {
        List<Product> products = new ArrayList<>();
        for (OrderedProduct orderedProduct: order.getOrderedProducts()) {
            Product product = orderedProduct.getProduct();
            product.setCountInBasket(orderedProduct.getCount());
            products.add(orderedProduct.getProduct());
        }
        Collections.sort(products, new Comparator<Product>() {
            @Override
            public int compare(Product o1, Product o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        return products;
    }
    public List<Product> getProductsByOrderId(Long order_id) {
        Order order = orderRepository.findById(order_id).get();
        return getProductsByOrder(order);
    }
    @Transactional
    public Order getBasket() {
        // ищем заказы с типом корзина (должен быт всегда 1 штука, а если нет, то создать)
        List<Order> orders = findOrdersBy(OrderStatus.PRE_ORDER);
        Order basket;
        if (!orders.isEmpty()) { // если есть, то получаем корзину
            basket = orders.getFirst();
        } else  { // если нет такого в БД, то создаем
            basket = createBasket();
        }
        return basket;
    }

    @Transactional
    public Order createBasket() {
        Order basket = new Order();
        basket.setStatus(OrderStatus.PRE_ORDER);
        basket.setTotalPrice(BigDecimal.ZERO);
        orderRepository.save(basket);
        return basket;
    }

    @Transactional
    public void setPaidFor(Long order_id) {
        Order order = orderRepository.findById(order_id).get();
        order.setStatus(OrderStatus.PAID);
        orderRepository.save(order);
        createBasket();
    }

    @Transactional
    public void updateProductInBasketBy(Long product_id, ActionWithProduct actionWithProduct) {
        // ищем заказы с типом корзина (должен быт всегда 1 штука, а если нет, то создать)
        Order basket = getBasket();
        // ищем связанные продукты корзины и обновляем их количество в зависимости от actionWithProduct
        if (orderContainsOrderedProductBy(product_id, basket)) { // если есть продукт в корзине
            OrderedProduct orderedProduct = getOrderedProductBy(product_id, basket);
            if (actionWithProduct == ActionWithProduct.INCREASE) {
                orderedProduct.setCount(orderedProduct.getCount() + 1);
            } else {
                if(orderedProduct.getCount() == 1) { // удаляем из корзины, если остался последний
                    basket.getOrderedProducts().remove(orderedProduct);
                } else { // иначе уменьшаем количество товаров в корзине
                    orderedProduct.setCount(orderedProduct.getCount() - 1);
                }
            }
        } else  { // если нет продукта в корзине и действие добавления, то создаем продукт в корзине
            if (actionWithProduct == ActionWithProduct.INCREASE) {
                Product product = productRepository.findById(product_id).get(); // находим продукт
                // создаем товар в корзине и привязываем к товару и корзине
                OrderedProduct orderedProduct = new OrderedProduct();
                orderedProduct.setProduct(product);
                orderedProduct.setOrder(basket);
                orderedProduct.setCount(1);
                basket.getOrderedProducts().add(orderedProduct);
            }
        }

        // расчет итоговой цены корзины и сохраняем
        BigDecimal totalPrice = BigDecimal.ZERO;
        for (OrderedProduct orderedProduct : basket.getOrderedProducts()) {
            BigDecimal productPrice = orderedProduct.getProduct().getPrice();
            BigDecimal count = BigDecimal.valueOf(orderedProduct.getCount());
            BigDecimal productTotal = productPrice.multiply(count);
            totalPrice = totalPrice.add(productTotal); // Сохраняем результат!
        }
        basket.setTotalPrice(totalPrice);
        orderRepository.save(basket);
    }

    // проверка, что заказ содержит товар по его ид
    public boolean orderContainsOrderedProductBy(Long product_id, Order order) {
        List<OrderedProduct> orderedProducts =  order.getOrderedProducts();
        Optional<OrderedProduct> optionalOrderedProduct = orderedProducts.stream()
                .filter(p -> p.getProduct().getId().equals(product_id))
                .findFirst();
        return optionalOrderedProduct.isPresent();
    }

    // возвращаем конкретный товар по ид - НЕ ВЫЗЫВАТЬ без предварительной проверки orderContainsOrderedProductBy
    public OrderedProduct getOrderedProductBy(Long product_id, Order order) {
        List<OrderedProduct> orderedProducts =  order.getOrderedProducts();
        Optional<OrderedProduct> optionalOrderedProduct = orderedProducts.stream()
                .filter(p -> p.getProduct().getId().equals(product_id))
                .findFirst();
        return optionalOrderedProduct.get();
    }

    // забираем продукты из корзины и возвращаем
    public List<Product> getBasketProducts() {
        Order basket = getBasket();
        return getProductsByOrder(basket);
    }

    // забираем продукты из корзины и возвращаем
    public BigDecimal getBasketPrice() {
        Order basket = getBasket();
        return basket.getTotalPrice();
    }

    public BigDecimal getOrderTotalPriceBy(Long order_id) {
        Order order = orderRepository.findById(order_id).get();
        return order.getTotalPrice();
    }
}
