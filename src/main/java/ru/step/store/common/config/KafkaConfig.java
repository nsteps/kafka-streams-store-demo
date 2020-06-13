package ru.step.store.common.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.UUIDSerializer;
import org.apache.kafka.streams.StreamsConfig;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import org.springframework.kafka.annotation.KafkaStreamsDefaultConfiguration;
import org.springframework.kafka.config.KafkaStreamsConfiguration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.converter.RecordMessageConverter;
import org.springframework.kafka.support.converter.StringJsonMessageConverter;
import org.springframework.kafka.support.serializer.JsonSerde;
import org.springframework.kafka.support.serializer.JsonSerializer;
import ru.step.store.common.Schemas;
import ru.step.store.common.model.Order;

import java.util.Map;
import java.util.UUID;

@Configuration
@RequiredArgsConstructor
@EnableKafka
@EnableKafkaStreams
@Slf4j
class KafkaConfig {
    final KafkaProperties kafkaProperties;

    @Bean
    NewTopic orderTopic() {
        return new NewTopic(Schemas.Topics.ORDERS.getName(), 1, (short) 1);
    }

    @Bean
    NewTopic orderValidationTopic() { return new NewTopic(Schemas.Topics.ORDER_VALIDATIONS.getName(), 1, (short) 1); }

    @Bean
    NewTopic warehouseInventoryTopic() { return new NewTopic(Schemas.Topics.WAREHOUSE_INVENTORY.getName(), 1, (short) 1); }

    @Bean
    ProducerFactory<UUID, Order> producerFactory() {
        final var props = kafkaProperties.buildConsumerProperties();
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, UUIDSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    KafkaTemplate<UUID, Order> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    @Bean(name = KafkaStreamsDefaultConfiguration.DEFAULT_STREAMS_CONFIG_BEAN_NAME)
    public KafkaStreamsConfiguration defaultKafkaStreamsConfig() {
        return new KafkaStreamsConfiguration(
                Map.of(
                        StreamsConfig.APPLICATION_ID_CONFIG, "change-me",
                        StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootstrapServers(),
                        StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass(),
                        StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, JsonSerde.class
                )
        );
    }

    @Bean
    RecordMessageConverter messageConverter() { return new StringJsonMessageConverter(); }
}