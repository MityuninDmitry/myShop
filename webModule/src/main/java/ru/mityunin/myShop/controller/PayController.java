package ru.mityunin.myShop.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.reactive.result.view.RedirectView;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import ru.mityunin.myShop.service.AuthService;
import ru.mityunin.myShop.service.OrderService;
import ru.mityunin.myShop.service.PayService;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Controller
@RequestMapping("/pay")
public class PayController {
    private static final Logger log = LoggerFactory.getLogger(PayController.class);
    private PayService payService;
    private OrderService orderService;
    private AuthService authService;

    public PayController(PayService payService, OrderService orderService, AuthService authService) {
        this.payService = payService;
        this.orderService = orderService;
        this.authService = authService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public Mono<RedirectView> pay(ServerWebExchange exchange) {

        return exchange.getFormData().flatMap(formData -> {
            Long orderId = Long.parseLong(formData.getFirst("order_id"));

            return payService.setPaidFor(orderId)
                    .flatMap(paymentResponse -> {
                        log.info("response {}",paymentResponse.getDescription());
                        if (paymentResponse.getProcessed()) {

                            log.info("processed response {}",paymentResponse.getProcessed());
                            return authService.getCurrentUsername()
                                    .flatMap(userName ->
                                            orderService.setPaidFor(userName, orderId))
                                    .thenReturn(new RedirectView("/order/basket"));
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
                        String errorMessage = "Ошибка обработки платежа: " + e.getMessage();
                        return Mono.just(new RedirectView("/order/basket?error=" +
                                URLEncoder.encode(errorMessage, StandardCharsets.UTF_8)));
                    });
        });

    }


}
