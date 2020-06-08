package ru.step.store.order.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.streams.errors.InvalidStateStoreException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import ru.step.store.common.Schemas;
import ru.step.store.order.mapper.OrderMapper;
import ru.step.store.common.model.Order;
import ru.step.store.order.model.OrderCreateRequest;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderService {
    private static final String ORDERS_STORE_NAME = "order-store";
    final KafkaTemplate<UUID, Order> kafkaTemplate;
//    final KafkaStreams streams;

    public Order getOrder(UUID orderId) {
        log.info("get order" + orderId);
        try {
//            final Order order = ordersStore().get(orderId);
            final Order order = null;
            if (order == null) {
                log.info("delay request");
            } else {
                return order;
            }
        } catch (InvalidStateStoreException ex) {

        }
        throw new RuntimeException("");
    }

    public void submitOrder(OrderCreateRequest request) {
        log.info("submit order");
         var order = OrderMapper.toOrder(request);
         kafkaTemplate.send(
                 Schemas.Topics.ORDERS.getName(), order.getId(), order
         ).addCallback(result -> {
             if (result != null) {
                 final RecordMetadata recordMetadata = result.getRecordMetadata();
                 log.info("produced to {}, {}, {}", recordMetadata.topic(), recordMetadata.partition(),
                         recordMetadata.offset());
             }
         }, ex -> log.error("err", ex));
    }

//    private ReadOnlyKeyValueStore<UUID, Order> ordersStore() {
//        return streams.store(ORDERS_STORE_NAME, QueryableStoreTypes.keyValueStore());
//    }
}
