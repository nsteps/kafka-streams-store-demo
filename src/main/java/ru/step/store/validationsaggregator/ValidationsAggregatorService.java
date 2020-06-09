package ru.step.store.validationsaggregator;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.*;
import org.springframework.kafka.config.KafkaStreamsConfiguration;
import org.springframework.stereotype.Service;
import ru.step.store.common.AbstractKStreamService;
import ru.step.store.common.model.Order;
import ru.step.store.common.model.OrderValidation;

import java.time.Duration;
import java.util.UUID;

import static ru.step.store.common.Schemas.Topics.ORDERS;
import static ru.step.store.common.Schemas.Topics.ORDER_VALIDATIONS;
import static ru.step.store.common.model.OrderValidation.OrderValidationResult.FAIL;
import static ru.step.store.common.model.OrderValidation.OrderValidationResult.PASS;

@Service
@Slf4j
public class ValidationsAggregatorService extends AbstractKStreamService {
    private static final String APP_ID = "validations-aggregator-service";

    public ValidationsAggregatorService(KafkaStreamsConfiguration baseStreamConfig) {
        super(baseStreamConfig, APP_ID);
    }

    @Override
    public StreamsBuilder processStream(StreamsBuilder builder) {
        final var validationsCount = OrderValidation.OrderValidationType.values().length;
        final KStream<UUID, Order> orders = builder
                .stream(ORDERS.getName(), Consumed.with(ORDERS.getKeySerde(), ORDERS.getValueSerde()))
                .filter((id, order) -> order.getStatus().equals(Order.Status.CREATED));
        final KStream<UUID, OrderValidation> validations = builder
                .stream(ORDER_VALIDATIONS.getName(), Consumed
                        .with(ORDER_VALIDATIONS.getKeySerde(), ORDER_VALIDATIONS.getValueSerde()));

        validations
                .groupByKey(Grouped.with(ORDER_VALIDATIONS.getKeySerde(), ORDER_VALIDATIONS.getValueSerde()))
                .windowedBy(SessionWindows.with(Duration.ofMillis(5)))
                .aggregate(
                        () -> 0L,
                        (id, result, total) -> PASS.equals(result.getResult()) ? total + 1 : total,
                        (k, a, b) -> b == null ? a : b,
                        Materialized.with(null, Serdes.Long())
                )
                .toStream((key, total) -> key.key())
                .filter((id, total) -> total != null)
                .filter((id, total) -> total >= validationsCount)
                .join(
                        orders,
                        (id, order) -> {
                            order.setStatus(Order.Status.VALIDATED);
                            return order;
                        },
                        JoinWindows.of(Duration.ofMinutes(5)),
                        StreamJoined.with(ORDERS.getKeySerde(), Serdes.Long(), ORDERS.getValueSerde())
                )
                .to(ORDERS.getName(), Produced.with(ORDERS.getKeySerde(), ORDERS.getValueSerde()));

        validations
                .filter((id, result) -> FAIL.equals(result.getResult()))
                .join(
                        orders,
                        (id, order) -> {
                            order.setStatus(Order.Status.FAILED);
                            return order;
                        },
                        JoinWindows.of(Duration.ofMinutes(5)),
                        StreamJoined
                                .with(ORDERS.getKeySerde(), ORDER_VALIDATIONS.getValueSerde(), ORDERS.getValueSerde())
                )
                .groupByKey(Grouped.with(ORDERS.getKeySerde(), ORDERS.getValueSerde()))
                .reduce((a, b) -> a)
                .toStream()
                .to(ORDERS.getName(), Produced.with(ORDERS.getKeySerde(), ORDERS.getValueSerde()));

        return builder;
    }
}
