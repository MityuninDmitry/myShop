package ru.mityunin.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ru.mityunin.server.domain.BalancePostRequest;
import ru.mityunin.server.domain.PaymentPostRequest;


@Service
public class PaymentService {
    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
    @Value("${payment.balance.on_start}")
    private float balance;


    public Mono<Float> getBalance() {
        return Mono.just(balance);
    }

    public void setBalance(float balance) {
        this.balance = balance;
    }

    public Mono<Float> increaseBalance(Mono<BalancePostRequest> balancePostRequestMono) {
        return balancePostRequestMono.flatMap(request -> {
            balance += request.getAmount();
            return Mono.just(balance);
        });
    }

    public Mono<Boolean> processPayment(Mono<PaymentPostRequest> paymentPostRequest) {
        log.info("into to process payment {} ", balance);
        return paymentPostRequest.flatMap(request -> {
            log.info("data {} {}", balance, request.getAmount());
            if (balance < request.getAmount()) {
                log.info("less");
                return Mono.just(false);
            } else {
                log.info("more");
                balance -= request.getAmount();
                return Mono.just(true);
            }
        }).doOnError(e -> log.error("Error processing payment", e));
    }
}
