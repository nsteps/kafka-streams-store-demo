package ru.step.store.common.utils;

import com.fasterxml.jackson.databind.type.TypeFactory;
import org.apache.kafka.common.header.Headers;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerde;

public class SerdeUtils {
    public static <T> JsonSerde<T> createJsonSerde(Class<T> clazz) {
        final var serde = new JsonSerde<T>();
        ((JsonDeserializer<T>) serde.deserializer()).setTypeFunction(
                (byte[] bytes1, Headers headers1) -> TypeFactory.defaultInstance().constructType(clazz));
        return serde;
    }
}
