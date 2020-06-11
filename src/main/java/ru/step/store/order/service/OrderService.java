package ru.step.store.order.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.springframework.kafka.config.KafkaStreamsConfiguration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import ru.step.store.common.AbstractKStreamService;
import ru.step.store.common.Schemas;
import ru.step.store.common.model.Order;
import ru.step.store.order.mapper.OrderMapper;
import ru.step.store.order.model.OrderCreateRequest;

import java.time.Duration;
import java.util.UUID;
import java.util.function.Predicate;

@Service
@Slf4j
public class OrderService extends AbstractKStreamService {
    private static final String APP_ID = "orders-service";
    private static final String ORDERS_STORE_NAME = "order-store";
    private static final Duration GET_TIMEOUT = Duration.ofSeconds(5);
    final KafkaTemplate<UUID, Order> kafkaTemplate;
    ReadOnlyKeyValueStore<UUID, Order> ordersStore;

    public OrderService(KafkaStreamsConfiguration baseStreamConfig,
                        KafkaTemplate<UUID, Order> kafkaTemplate) {
        super(baseStreamConfig, APP_ID);
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public StreamsBuilder processStream(StreamsBuilder builder) {
        final KTable<UUID, Order> orders = builder
                .table(Schemas.Topics.ORDERS.getName(),
                        Consumed.with(Schemas.Topics.ORDERS.getKeySerde(), Schemas.Topics.ORDERS.getValueSerde()),
                        Materialized.as(ORDERS_STORE_NAME));
        return builder;
    }

    public Order getOrderOrNull(UUID orderId, Predicate<Order> predicate) {
        initializeStoreIfEmpty();
        Order order = ordersStore.get(orderId);
        if (order == null || !predicate.test(order)) {
            try {
                Thread.sleep(GET_TIMEOUT.toMillis());
                order = ordersStore.get(orderId);
                return (order != null && predicate.test(order)) ? order : null;
            } catch (InterruptedException e) {
                order = ordersStore.get(orderId);
                return (order != null && predicate.test(order)) ? order : null;
            }
        }
        return order;
    }

    public void submitOrder(OrderCreateRequest request) {
        final var order = OrderMapper.toOrder(request);
        kafkaTemplate.send(
                Schemas.Topics.ORDERS.getName(), order.getId(), order
        ).addCallback(result -> {
            if (result != null) {
                final RecordMetadata recordMetadata = result.getRecordMetadata();
                log.info("order submitted to to {}, {}, {}", recordMetadata.topic(), recordMetadata.partition(),
                        recordMetadata.offset());
            }
        }, ex -> log.error("error on order submit", ex));
    }

    private void initializeStoreIfEmpty() {
        if (ordersStore == null)
            ordersStore = super.kafkaStreams.store(ORDERS_STORE_NAME, QueryableStoreTypes.keyValueStore());
    }
}
