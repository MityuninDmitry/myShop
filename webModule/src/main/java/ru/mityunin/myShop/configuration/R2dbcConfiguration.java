package ru.mityunin.myShop.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.r2dbc.spi.ConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.config.AbstractR2dbcConfiguration;
import org.springframework.data.r2dbc.convert.R2dbcCustomConversions;
import org.springframework.data.r2dbc.dialect.PostgresDialect;
import ru.mityunin.myShop.model.converter.OrderedProductReadConverter;
import ru.mityunin.myShop.model.converter.OrderedProductWriteConverter;

import java.util.List;

@Configuration
public class R2dbcConfiguration {

    @Bean
    public R2dbcCustomConversions r2dbcCustomConversions(ObjectMapper objectMapper) {
        return R2dbcCustomConversions.of(
                PostgresDialect.INSTANCE,
                List.of(
                        new OrderedProductReadConverter(objectMapper),
                        new OrderedProductWriteConverter(objectMapper)
                )
        );
    }
}
