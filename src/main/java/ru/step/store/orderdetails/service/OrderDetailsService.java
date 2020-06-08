package ru.step.store.orderdetails.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Produced;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.config.KafkaStreamsConfiguration;
import org.springframework.stereotype.Service;
import ru.step.store.common.Schemas;
import ru.step.store.common.model.Order;
import ru.step.store.common.model.OrderValidation;

import java.util.UUID;

import static ru.step.store.common.model.OrderValidation.OrderValidationResult.FAIL;
import static ru.step.store.common.model.OrderValidation.OrderValidationResult.PASS;
import static ru.step.store.common.model.OrderValidation.OrderValidationType.ORDER_DETAILS_CHECK;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderDetailsService {
    private static final String APP_ID = "order-details-service";

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
        final var ordersTopic = Schemas.Topics.ORDERS;
        final KStream<UUID, Order> orders = builder
                .stream(ordersTopic.getName(), Consumed.with(ordersTopic.getKeySerde(), ordersTopic.getValueSerde()))
                .filter((id, order) -> order.getStatus().equals(Order.Status.CREATED));

        @SuppressWarnings("unchecked")
        final KStream<UUID, Order>[] forks = orders.branch(
                (id, order) -> isValid(order),
                (id, order) -> !isValid(order)
        );

        forks[0].mapValues(
                order -> new OrderValidation(order.getId(), ORDER_DETAILS_CHECK, PASS))
                .to(Schemas.Topics.ORDER_VALIDATIONS.getName(), Produced
                        .with(Schemas.Topics.ORDER_VALIDATIONS.getKeySerde(),
                                Schemas.Topics.ORDER_VALIDATIONS.getValueSerde()));



        forks[1].mapValues(
                order -> new OrderValidation(order.getId(), ORDER_DETAILS_CHECK, FAIL))
                .to(Schemas.Topics.ORDER_VALIDATIONS.getName(), Produced
                        .with(Schemas.Topics.ORDER_VALIDATIONS.getKeySerde(),
                                Schemas.Topics.ORDER_VALIDATIONS.getValueSerde()));

        return builder;
    }

    private boolean isValid(Order order) {
        return order.getCustomerId() != null
                && order.getQuantity() > 0
                && order.getPrice() > 0
                && order.getProduct() != null;
    }
}
