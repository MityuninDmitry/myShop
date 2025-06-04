package ru.mityunin.myShop.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import ru.mityunin.client.ApiClient;
import ru.mityunin.client.api.DefaultApi;

@Configuration
public class ApiClientConfig {

    @Value("${payment.service.url:http://localhost:8081}")
    private String paymentServiceUrl;

    @Bean
    public ApiClient apiClient() {
        WebClient webClient = WebClient.builder()
                .baseUrl(paymentServiceUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        ApiClient apiClient = new ApiClient(webClient);
        apiClient.setBasePath(paymentServiceUrl);

        return apiClient;
    }

    @Bean
    public DefaultApi paymentApi(ApiClient apiClient) {
        return new DefaultApi(apiClient);
    }
}

