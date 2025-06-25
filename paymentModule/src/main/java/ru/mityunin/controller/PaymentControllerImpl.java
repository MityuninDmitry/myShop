package ru.mityunin.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import ru.mityunin.server.api.PaymentApi;
import ru.mityunin.server.domain.PaymentPostRequest;
import ru.mityunin.server.domain.PaymentResponse;
import ru.mityunin.service.PaymentService;

@RestController
public class PaymentControllerImpl implements PaymentApi {
    private static final Logger log = LoggerFactory.getLogger(PaymentControllerImpl.class);

    private final PaymentService paymentService;

    public PaymentControllerImpl(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @Override
    public Mono<ResponseEntity<PaymentResponse>> paymentPost(
            @RequestParam String username,
            Mono<PaymentPostRequest> paymentPostRequest,
            ServerWebExchange exchange) {
        return paymentPostRequest.flatMap(request -> {
            if (request.getAmount() <= 0) {
                PaymentResponse response = new PaymentResponse();
                response.setDescription("Неверный запрос (некорректная сумма)");
                response.setProcessed(false);
                return Mono.just(ResponseEntity.ok().body(response));
            }
            try {
                log.info("Payment request for user: {}, amount: {}", username, request.getAmount());
                return paymentService.processPayment(username, Mono.just(request))
                        .flatMap(processed -> {
                            PaymentResponse response = new PaymentResponse();
                            response.setProcessed(processed);
                            if (processed) {
                                response.setDescription("Заказ успешно оплачен");
                                log.info("Payment successful for user: {}", username);
                                return Mono.just(ResponseEntity.ok().body(response));
                            } else {
                                response.setDescription("Недостаточно средств на балансе");
                                return Mono.just(ResponseEntity.ok().body(response));
                            }
                        });
            } catch (Exception e) {
                return Mono.just(ResponseEntity.internalServerError().build());
            }
        });
    }
}