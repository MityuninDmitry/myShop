package ru.mityunin.service;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ru.mityunin.server.domain.BalancePostRequest;
import ru.mityunin.server.domain.PaymentPostRequest;


@Service
public class PaymentService {

    private float balance = 50.00F;


    public Mono<Float> getBalance() {
        return Mono.just(balance);
    }

    public Mono<Float> increaseBalance(Mono<BalancePostRequest> balancePostRequestMono) {
        return balancePostRequestMono.flatMap(request -> {
            balance += request.getAmount();
            return Mono.just(balance);
        });
    }

    public Mono<Boolean> processPayment(Mono<PaymentPostRequest> paymentPostRequest) {
        return paymentPostRequest.flatMap(request -> {
            if (balance < request.getAmount()) {
                return Mono.just(false);
            } else {
                balance -= request.getAmount();
                return Mono.just(true);
            }
        });
    }
}
