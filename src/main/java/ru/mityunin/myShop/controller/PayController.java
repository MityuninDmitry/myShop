package ru.mityunin.myShop.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import ru.mityunin.myShop.service.OrderService;
import ru.mityunin.myShop.service.PayService;

@Controller
@RequestMapping("/pay")
public class PayController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private PayService payService;

    @PostMapping
    public String payOrder(Model model, @RequestParam Long order_id) {
        payService.setPaidFor(order_id);
        model.addAttribute("products", orderService.getProductsByOrderId(order_id));
        model.addAttribute("totalPrice", orderService.getOrderTotalPriceBy(order_id));
        return "redirect:/order/" + order_id;
    }
}
