package ru.step.store.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import ru.step.store.common.model.Order;
import ru.step.store.common.model.OrderValidation;
import ru.step.store.common.utils.SerdeUtils;

import java.util.UUID;

public class Schemas {
    @Data
    @AllArgsConstructor
    public static class Topic<K, V> {
        private final String name;
        private final Serde<K> keySerde;
        private final Serde<V> valueSerde;
    }

    public static class Topics {
        public static Topic<UUID, Order> ORDERS;
        public static Topic<UUID, OrderValidation> ORDER_VALIDATIONS;
        public static Topic<Order.Product, Integer> WAREHOUSE_INVENTORY;

        static {
            createTopics();
        }

        private static void createTopics() {
            ORDERS = new Topic<>("orders", Serdes.UUID(), SerdeUtils.createJsonSerde(Order.class));
            ORDER_VALIDATIONS = new Topic<>("order-validations", Serdes.UUID(),
                    SerdeUtils.createJsonSerde(OrderValidation.class));
            WAREHOUSE_INVENTORY = new Topic<>("warehouse-inventory",
                    SerdeUtils.createJsonSerde(Order.Product.class), Serdes.Integer());
        }
    }
}
