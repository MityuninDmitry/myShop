package ru.mityunin.myShop.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.result.view.RedirectView;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import ru.mityunin.myShop.service.PayService;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

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
            Long orderId = Long.parseLong(formData.getFirst("order_id"));

            return payService.setPaidFor(orderId)
                    .flatMap(paymentResponse -> {
                        if (paymentResponse.getProcessed()) {
                            return Mono.just(new RedirectView("/order/basket"));
                        } else {
                            // Добавляем параметр ошибки в URL
                            String errorMessage = "Оплата не прошла: " +
                                    (paymentResponse.getDescription() != null ?
                                            paymentResponse.getDescription() : "Непредвиденная ошибка");
                            return Mono.just(new RedirectView("/order/basket?error=" +
                                    URLEncoder.encode(errorMessage, StandardCharsets.UTF_8)));
                        }
                    })
                    .onErrorResume(e -> {
                        // Обработка других ошибок
                        String errorMessage = "Ошибка обработки платежа: " + e.getMessage();
                        return Mono.just(new RedirectView("/order/basket?error=" +
                                URLEncoder.encode(errorMessage, StandardCharsets.UTF_8)));
                    });
        });

    }
}
