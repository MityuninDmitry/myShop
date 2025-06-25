package ru.mityunin.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import ru.mityunin.server.api.BalanceApi;
import ru.mityunin.server.domain.BalanceGet200Response;
import ru.mityunin.server.domain.BalancePost200Response;
import ru.mityunin.server.domain.BalancePostRequest;
import ru.mityunin.service.PaymentService;

@RestController
public class BalanceControllerImpl implements BalanceApi {

    private final PaymentService paymentService;

    public BalanceControllerImpl(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @Override
    public Mono<ResponseEntity<BalanceGet200Response>> balanceGet(
            @RequestParam String username,
            ServerWebExchange exchange) {
        return paymentService.getBalance(username)
                .map(balance -> {
                    BalanceGet200Response response = new BalanceGet200Response();
                    response.setBalance(balance);
                    return ResponseEntity.ok(response);
                });
    }

    @Override
    public Mono<ResponseEntity<BalancePost200Response>> balancePost(
            @RequestParam String username,
            Mono<BalancePostRequest> balancePostRequest,
            ServerWebExchange exchange) {
        return paymentService.increaseBalance(username, balancePostRequest)
                .map(balance -> {
                    BalancePost200Response response = new BalancePost200Response();
                    response.setNewBalance(balance);
                    return ResponseEntity.ok(response);
                });
    }
}