package ru.mityunin.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ru.mityunin.server.domain.BalancePostRequest;
import ru.mityunin.server.domain.PaymentPostRequest;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PaymentService {
    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final Map<String, Float> userBalances = new ConcurrentHashMap<>();

    @Value("${payment.balance.on_start}")
    private float initialBalance;

    public Mono<Float> getBalance(String username) {
        return Mono.just(userBalances.computeIfAbsent(username, k -> initialBalance));
    }

    public Mono<Float> increaseBalance(String username, Mono<BalancePostRequest> balancePostRequestMono) {
        return balancePostRequestMono.flatMap(request -> {
            float currentBalance = userBalances.computeIfAbsent(username, k -> initialBalance);
            float newBalance = currentBalance + request.getAmount();
            userBalances.put(username, newBalance);
            return Mono.just(newBalance);
        });
    }

    public Mono<Boolean> processPayment(String username, Mono<PaymentPostRequest> paymentPostRequest) {
        return paymentPostRequest.flatMap(request -> {
            float currentBalance = userBalances.computeIfAbsent(username, k -> initialBalance);
            log.info("Processing payment for user: {}, current balance: {}, amount: {}",
                    username, currentBalance, request.getAmount());

            if (currentBalance < request.getAmount()) {
                log.info("Insufficient funds for user: {}", username);
                return Mono.just(false);
            } else {
                float newBalance = currentBalance - request.getAmount();
                userBalances.put(username, newBalance);
                log.info("Payment processed for user: {}, new balance: {}", username, newBalance);
                return Mono.just(true);
            }
        }).doOnError(e -> log.error("Error processing payment for user: " + username, e));
    }
}