package ru.mityunin.myShop.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.result.view.RedirectView;
import org.springframework.web.reactive.result.view.Rendering;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.mityunin.myShop.model.Order;
import ru.mityunin.myShop.model.OrderStatus;
import ru.mityunin.myShop.service.OrderService;
import ru.mityunin.myShop.service.PayService;

@Controller
@RequestMapping("/pay")
public class PayController {

    private PayService payService;

    public PayController(PayService payService) {
        this.payService = payService;
    }

    @PostMapping
    public Mono<RedirectView> payOrder(ServerWebExchange exchange) {
        return exchange.getFormData().flatMap(formData -> {
            Long order_id = Long.parseLong(formData.getFirst("order_id"));
            return payService.setPaidFor(order_id).thenReturn(new RedirectView("/order/basket"));
        });

    }
}
