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

    public PayService(OrderService orderService, DefaultApi paymentApi) {
        this.orderService = orderService;
        this.paymentApi = paymentApi;
    }

    @Transactional
    public Mono<PaymentResponse> setPaidFor(Long order_id) {
        return orderService.getOrderTotalPriceBy(order_id)
                .flatMap(orderPrice -> {
                    PaymentPostRequest request = new PaymentPostRequest();
                    request.setAmount(orderPrice.floatValue());
                    log.info("request to payment {}", request);
                    return paymentApi.paymentPost(request);
                });
    }

    public Mono<Float> getCurrentBalance() {
        return paymentApi.balanceGet().flatMap(response -> {
            return Mono.just(response.getBalance());
        }).onErrorResume(e -> {
            return Mono.just(-1F);
        });
    }
}
