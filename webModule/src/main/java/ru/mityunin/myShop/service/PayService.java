package ru.mityunin.myShop.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import ru.mityunin.client.ApiClient;
import ru.mityunin.client.api.DefaultApi;
import ru.mityunin.client.domain.PaymentPostRequest;
import ru.mityunin.client.domain.PaymentResponse;

import java.math.BigDecimal;

@Service
public class PayService {
    private static final Logger log = LoggerFactory.getLogger(PayService.class);
    private final OrderService orderService;
    private final DefaultApi paymentApi;
    private final AuthService authService;

    public PayService(OrderService orderService, DefaultApi paymentApi, AuthService authService) {
        this.orderService = orderService;
        this.paymentApi = paymentApi;
        this.authService = authService;
    }

    @Transactional
    public Mono<PaymentResponse> setPaidFor(Long order_id) {
        return authService.getCurrentUsername()
                .flatMap(username ->
                        orderService.getOrderTotalPriceBy(order_id)
                                .flatMap(orderPrice -> {
                                    PaymentPostRequest request = new PaymentPostRequest();
                                    request.setAmount(orderPrice.floatValue());
                                    log.info("Payment request for user: {}, order: {}, amount: {}",
                                            username, order_id, request.getAmount());
                                    return paymentApi.paymentPost(username, request);
                                })
                );
    }

    public Mono<Float> getCurrentBalance() {
        return authService.getCurrentUsername()
                .flatMap(username ->
                        paymentApi.balanceGet(username)
                                .flatMap(response -> Mono.just(response.getBalance()))
                                .onErrorResume(e -> Mono.just(-1F))
                );
    }
}
