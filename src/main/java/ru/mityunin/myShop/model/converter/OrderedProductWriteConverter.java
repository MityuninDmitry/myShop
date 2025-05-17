package ru.mityunin.myShop.model.converter;


import com.fasterxml.jackson.databind.ObjectMapper;
import io.r2dbc.postgresql.codec.Json;
import ru.mityunin.myShop.model.OrderedProduct;
import org.springframework.core.convert.converter.Converter;

import java.util.List;

public class OrderedProductWriteConverter implements Converter<List<OrderedProduct>, Json> {
    private final ObjectMapper objectMapper;

    public OrderedProductWriteConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Json convert(List<OrderedProduct> source) {
        try {
            byte[] converted = objectMapper.writeValueAsBytes(source);
            return Json.of(converted);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

}
