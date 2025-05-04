package ru.mityunin.myShop.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.mityunin.myShop.model.Order;
import ru.mityunin.myShop.repository.OrderedProductRepository;

@Service
public class OrderedProductService {
    @Autowired
    private OrderedProductRepository orderedProductRepository;

}
