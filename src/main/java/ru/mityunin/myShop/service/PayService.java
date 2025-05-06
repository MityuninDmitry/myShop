package ru.mityunin.myShop.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mityunin.myShop.model.Order;
import ru.mityunin.myShop.model.OrderStatus;
import ru.mityunin.myShop.repository.OrderRepository;

@Service
public class PayService {
    @Autowired
    private OrderService orderService;

    @Transactional
    public void setPaidFor(Long order_id) {
        orderService.setPaidFor(order_id);
    }
}
