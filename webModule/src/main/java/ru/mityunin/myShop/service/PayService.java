package ru.mityunin.myShop.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import ru.mityunin.client.ApiClient;
import ru.mityunin.client.api.DefaultApi;
import ru.mityunin.client.domain.PaymentPostRequest;
import ru.mityunin.client.domain.PaymentResponse;

@Service
public class PayService {
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
                    return paymentApi.paymentPost(request);
                });
    }
}
