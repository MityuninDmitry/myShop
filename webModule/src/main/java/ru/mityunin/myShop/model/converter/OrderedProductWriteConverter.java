package ru.mityunin.myShop.model.converter;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.r2dbc.postgresql.codec.Json;
import org.springframework.data.convert.WritingConverter;
import ru.mityunin.myShop.model.OrderedProduct;
import org.springframework.core.convert.converter.Converter;

import java.util.List;

@WritingConverter
public class OrderedProductWriteConverter implements Converter<List<OrderedProduct>, Json> {
    private final ObjectMapper objectMapper;

    public OrderedProductWriteConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Json convert(List<OrderedProduct> source) {
        try {
            String json = objectMapper.writeValueAsString(source);
            return Json.of(json);
        } catch (JsonProcessingException e) {
            return Json.of("[]");
        }
    }
}
