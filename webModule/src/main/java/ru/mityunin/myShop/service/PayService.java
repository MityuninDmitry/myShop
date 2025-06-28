package ru.mityunin.myShop.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import ru.mityunin.client.ApiClient;
import ru.mityunin.client.api.DefaultApi;
import ru.mityunin.client.domain.PaymentPostRequest;
import ru.mityunin.client.domain.PaymentResponse;

@Service
public class PayService {
    private static final Logger log = LoggerFactory.getLogger(PayService.class);
    private final OrderService orderService;
    private final DefaultApi paymentApi;
    private final AuthService authService;
    private ReactiveOAuth2AuthorizedClientManager manager;

    public PayService(OrderService orderService, DefaultApi paymentApi, AuthService authService, ReactiveOAuth2AuthorizedClientManager manager) {
        this.orderService = orderService;
        this.paymentApi = paymentApi;
        this.authService = authService;
        this.manager = manager;
    }

    private Mono<String> getAccessToken() {
        log.info("trying get access token");
        return  manager.authorize(OAuth2AuthorizeRequest
                        .withClientRegistrationId("keycloak")
                        .principal("payment-service")
                        .build())
                .map(authorizedClient -> authorizedClient.getAccessToken().getTokenValue())
                .doOnNext(value -> log.info("token is {}", value));
    }

    @Transactional
    public Mono<PaymentResponse> setPaidFor(Long order_id) {
        return getAccessToken().flatMap( token ->
                authService.getCurrentUsername()
                        .flatMap(username ->
                                orderService.getOrderTotalPriceBy(order_id)
                                        .flatMap(orderPrice -> {
                                            PaymentPostRequest request = new PaymentPostRequest();
                                            request.setAmount(orderPrice.floatValue());

                                            ApiClient apiClient = paymentApi.getApiClient();
                                            apiClient.addDefaultHeader("Authorization", "Bearer " + token);

                                            log.info("Payment request for user: {}, order: {}, amount: {}",
                                                    username, order_id, request.getAmount());
                                            return paymentApi.paymentPost(username, request);
                                        })
                        )
        ) ;
    }

    public Mono<Float> getCurrentBalance() {
        return getAccessToken().flatMap(token -> authService.getCurrentUsername()
            .flatMap(username -> {
                        // Добавляем токен в заголовок
                        ApiClient apiClient = paymentApi.getApiClient();
                        apiClient.addDefaultHeader("Authorization", "Bearer " + token);
                        log.info("api client toker {}", token);
                        return paymentApi.balanceGet(username)
                                .flatMap(response -> Mono.just(response.getBalance()))
                                .onErrorResume(e -> Mono.just(-1F));
                    }
            )
        );
    }
}
