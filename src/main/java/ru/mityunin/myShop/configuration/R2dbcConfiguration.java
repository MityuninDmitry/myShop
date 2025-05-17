package ru.mityunin.myShop.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.r2dbc.spi.ConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.config.AbstractR2dbcConfiguration;
import ru.mityunin.myShop.model.converter.OrderedProductReadConverter;
import ru.mityunin.myShop.model.converter.OrderedProductWriteConverter;

import java.util.List;

@Configuration
public class R2dbcConfiguration  {
    // Используем стандартный ObjectMapper
    private final ObjectMapper objectMapper;

    public R2dbcConfiguration(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    @Bean
    protected List<Object> getCustomConverters() {
        // Регистрируем конвертеры
        return List.of(
                new OrderedProductWriteConverter(objectMapper),
                new OrderedProductReadConverter(objectMapper)
        );
    }
}
