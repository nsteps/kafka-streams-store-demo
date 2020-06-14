package ru.step.store.warehouse;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.*;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.StoreBuilder;
import org.apache.kafka.streams.state.Stores;
import org.springframework.kafka.config.KafkaStreamsConfiguration;
import org.springframework.stereotype.Service;
import ru.step.store.common.AbstractKStreamService;
import ru.step.store.common.model.Order;
import ru.step.store.common.model.OrderValidation;

import java.util.UUID;

import static ru.step.store.common.Schemas.Topics.*;
import static ru.step.store.common.model.OrderValidation.OrderValidationResult.FAIL;
import static ru.step.store.common.model.OrderValidation.OrderValidationResult.PASS;
import static ru.step.store.common.model.OrderValidation.OrderValidationType.INVENTORY_CHECK;

@Service
@Slf4j
public class WarehouseService extends AbstractKStreamService {
    private static final String APP_ID = "warehouse-service";
    public static final String RESERVED_PRODUCTS_STORE = "reserved-products-store";


    public WarehouseService(KafkaStreamsConfiguration baseStreamConfig) {
        super(baseStreamConfig, APP_ID);
    }

    @Override
    public StreamsBuilder processStream(StreamsBuilder builder) {
        final KTable<Order.Product, Integer> warehouseInventory = builder
                .table(WAREHOUSE_INVENTORY.getName(),
                        Consumed.with(WAREHOUSE_INVENTORY.getKeySerde(), WAREHOUSE_INVENTORY.getValueSerde()));
        final KStream<UUID, Order> orders = builder
                .stream(ORDERS.getName(), Consumed.with(ORDERS.getKeySerde(), ORDERS.getValueSerde()));

        final StoreBuilder<KeyValueStore<Order.Product, Integer>> store = Stores
                .keyValueStoreBuilder(Stores.persistentKeyValueStore(RESERVED_PRODUCTS_STORE),
                        WAREHOUSE_INVENTORY.getKeySerde(), Serdes.Integer());
        builder.addStateStore(store);

        orders
                .selectKey((id, order) -> order.getProduct())
                .filter((id, order) -> Order.Status.CREATED.equals(order.getStatus()))
                .join(
                        warehouseInventory,
                        KeyValue::new,
                        Joined.with(WAREHOUSE_INVENTORY.getKeySerde(), ORDERS.getValueSerde(), Serdes.Integer()))
                .transform(WarehouseValidator::new, RESERVED_PRODUCTS_STORE)
                .to(ORDER_VALIDATIONS.getName(),
                        Produced.with(ORDER_VALIDATIONS.getKeySerde(), ORDER_VALIDATIONS.getValueSerde()));

        return builder;
    }

    private static class WarehouseValidator implements
            Transformer<Order.Product, KeyValue<Order, Integer>, KeyValue<UUID, OrderValidation>> {
        private KeyValueStore<Order.Product, Integer> reservedStore;

        @Override
        @SuppressWarnings("unchecked")
        public void init(ProcessorContext context) {
            reservedStore = (KeyValueStore<Order.Product, Integer>) context.getStateStore(RESERVED_PRODUCTS_STORE);
        }

        @Override
        public KeyValue<UUID, OrderValidation> transform(Order.Product key, KeyValue<Order, Integer> value) {
            final Order order = value.key;
            final Integer count = value.value;

            Integer reserved = reservedStore.get(order.getProduct());
            if (reserved == null) reserved = 0;

            final OrderValidation validationResult;
            if (count - reserved - order.getQuantity() >= 0) {
                reservedStore.put(order.getProduct(), reserved + order.getQuantity());
                validationResult = new OrderValidation(order.getId(), INVENTORY_CHECK, PASS);
            } else {
                validationResult = new OrderValidation(order.getId(), INVENTORY_CHECK, FAIL);
            }
            return KeyValue.pair(validationResult.getOrderId(), validationResult);
        }

        @Override
        public void close() { }
    }
}
