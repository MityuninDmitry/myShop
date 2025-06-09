package ru.mityunin.myShop;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import ru.mityunin.client.ApiClient;
import ru.mityunin.client.api.DefaultApi;


@TestConfiguration
@Profile("test")
public class TestApiClientConfig {

    @Bean
    @Primary
    public ApiClient apiClient() {
        ApiClient apiClient = new ApiClient();
        apiClient.setBasePath("http://localhost:8089"); // Локальный URL (можно заменить на тестовый)
        return apiClient;
    }

    @Bean
    @Primary
    public DefaultApi paymentApi(ApiClient apiClient) {
        return new DefaultApi(apiClient);
    }
}
