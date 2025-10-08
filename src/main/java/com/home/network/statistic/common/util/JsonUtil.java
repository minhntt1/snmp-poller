package com.home.network.statistic.common.util;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.SneakyThrows;

public class JsonUtil {
    private static final ObjectMapper mapper = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            .build();

    private JsonUtil() {}

    @SneakyThrows
    public static String toJson(Object o) {
        return mapper.writeValueAsString(o);
    }

    @SneakyThrows
    public static <T> T fromJson(String json, Class<T> clazz) {
        return mapper.readValue(json, clazz);
    }
}
