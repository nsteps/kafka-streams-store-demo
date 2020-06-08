package ru.step.store.fraud.service;

import com.fasterxml.jackson.databind.type.TypeFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.*;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.config.KafkaStreamsConfiguration;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerde;
import org.springframework.stereotype.Service;
import ru.step.store.common.model.Order;
import ru.step.store.common.model.OrderValidation;
import ru.step.store.fraud.model.OrderValue;

import java.time.Duration;
import java.util.UUID;

import static ru.step.store.common.model.OrderValidation.OrderValidationResult.FAIL;
import static ru.step.store.common.model.OrderValidation.OrderValidationResult.PASS;
import static ru.step.store.common.model.OrderValidation.OrderValidationType.FRAUD_CHECK;

@Service
@Slf4j
@RequiredArgsConstructor
public class FraudService {
    private static final int FRAUD_LIMIT = 2000;
    private static final String APP_ID = "fraud-app";

    final NewTopic orderTopic;
    final NewTopic orderValidationTopic;
    final KafkaStreamsConfiguration baseStreamConfig;

    @EventListener(ApplicationStartedEvent.class)
    public void process() {
        final var builder = processStream(new StreamsBuilder());
        final var props = baseStreamConfig.asProperties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, APP_ID);
        final KafkaStreams kafkaStreams = new KafkaStreams(builder.build(), props);
        kafkaStreams.start();
    }

    private StreamsBuilder processStream(StreamsBuilder builder) {
        final var orderSerde = createJsonSerde(Order.class);
        final var orderValueSerde = createJsonSerde(OrderValue.class);
        final var orderValidationSerde = createJsonSerde(OrderValidation.class);

        final KStream<String, Order> orders = builder
                .stream(orderTopic.name(), Consumed.with(Serdes.String(), orderSerde))
                .filter((id, order) -> order.getStatus().equals(Order.Status.CREATED));

        final KTable<Windowed<UUID>, OrderValue> aggregate = orders
                .groupBy((id, order) -> order.getCustomerId(), Grouped.with(Serdes.UUID(), orderSerde))
                .windowedBy(SessionWindows.with(Duration.ofHours(1)))
                .aggregate(
                        OrderValue::new,
                        (customerId, order, total) -> new OrderValue(order,
                                total.getTotalValue() + order.getPrice() * order.getQuantity()),
                        (aggKey, aggOne, aggTwo) -> new OrderValue(aggTwo.getOrder(),
                                (aggOne == null ? 0L : aggOne.getTotalValue() + aggTwo.getTotalValue())),
                        Materialized.with(null, orderValueSerde)
                );

        final KStream<UUID, OrderValue> ordersWithTotal = aggregate
                .toStream((k, v) -> k.key())
                .filter((k, v) -> v != null)
                .selectKey((id, orderValue) -> orderValue.getOrder().getId());

        final KStream<UUID, OrderValue>[] forks = ordersWithTotal.branch(
                (id, orderValue) -> orderValue.getTotalValue() >= FRAUD_LIMIT,
                (id, orderValue) -> orderValue.getTotalValue() < FRAUD_LIMIT);

        forks[0]
                .mapValues(orderValue -> new OrderValidation(orderValue.getOrder().getId(), FRAUD_CHECK, PASS))
                .to(orderValidationTopic.name(), Produced
                        .with(Serdes.UUID(), orderValidationSerde));

        forks[1]
                .mapValues(orderValue -> new OrderValidation(orderValue.getOrder().getId(), FRAUD_CHECK, FAIL))
                .to(orderValidationTopic.name(), Produced
                        .with(Serdes.UUID(), orderValidationSerde));

        return builder;
    }

    private <T> JsonSerde<T> createJsonSerde(Class<T> clazz) {
        final var serde = new JsonSerde<T>();
        ((JsonDeserializer<T>) serde.deserializer()).setTypeFunction((byte[] bytes1, Headers headers1) ->
                TypeFactory.defaultInstance().constructType(clazz));
        return serde;
    }
}
