package ru.step.store.common.config;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsConfig;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.Properties;

@Configuration
@RequiredArgsConstructor
class KafkaConfig {
    final KafkaProperties kafkaProperties;

    @Bean
    NewTopic orderTopic() {
        return new NewTopic("order-topic", 1, (short) 1);
    }

    @Bean
    NewTopic orderValidationTopic() {
        return new NewTopic("order-validations", 1, (short) 1);
    }

    @Bean
    NewTopic warehouseInventoryTopic() {
        return new NewTopic("warehouse-inventory", 1, (short) 1);
    }

    @Bean
    public Properties baseStreamConfig() {
        Properties props = new Properties();
        props.putAll(
                Map.of(
//                        StreamsConfig.APPLICATION_ID_CONFIG, appId,
                        StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootstrapServers(),
                        StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass(),
                        StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass()
                )
        );
        return props;
    }
}