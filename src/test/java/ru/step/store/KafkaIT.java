package ru.step.store;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.utils.KafkaTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

public class KafkaIT extends AbstractIntegrationTest {
    @Autowired
    KafkaProperties kafkaProperties;

    @Autowired
    Properties baseStreamConfig;

    @Autowired
    NewTopic orderTopic;

    @Test
    void consumerTest() {
        final Consumer<String, String> consumer = createConsumer();
        consumer.subscribe(List.of(orderTopic.name()));
        while (true) {
            final ConsumerRecords<String, String> records = KafkaTestUtils.getRecords(consumer);
            if (records.isEmpty()) break;
            records.forEach(System.out::println);
        }
    }

    @Test
    void kStreamStorageTest() throws InterruptedException {
        StreamsBuilder builder = new StreamsBuilder();
        final String storeName = "test-store";
        builder.table(orderTopic.name(), Materialized.as(storeName));
        KafkaStreams streams = new KafkaStreams(builder.build(), baseStreamConfig);
        streams.start();
        Thread.sleep(5_000);
        final Object o = streams.store(storeName, QueryableStoreTypes.keyValueStore())
                .get("kafka-1");
        System.out.println(o);
        streams.close();
        assertThat(o).isNotNull();
    }

    @NotNull
    private Consumer<String, String> createConsumer() {
        final Map<String, Object> properties = kafkaProperties.buildConsumerProperties();
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, "test");
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return new DefaultKafkaConsumerFactory<>(
                properties,
                StringDeserializer::new,
                StringDeserializer::new
        ).createConsumer();
    }
}
