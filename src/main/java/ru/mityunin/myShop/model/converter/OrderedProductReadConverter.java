package ru.mityunin.myShop.model.converter;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.r2dbc.postgresql.codec.Json;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import ru.mityunin.myShop.model.OrderedProduct;

import java.util.List;

@ReadingConverter
public class OrderedProductReadConverter implements Converter<Json, List<OrderedProduct>> {
    private final ObjectMapper objectMapper;

    public OrderedProductReadConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public List<OrderedProduct> convert(Json source) {
        try {
            return objectMapper.readValue(source.asString(), new TypeReference<>() {});
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize OrderedProduct list", e);
        }
    }
}
