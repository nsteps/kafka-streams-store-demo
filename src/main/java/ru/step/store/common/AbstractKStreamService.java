package ru.step.store.common;

import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.config.KafkaStreamsConfiguration;

import javax.annotation.PreDestroy;

public abstract class AbstractKStreamService {
    final String APP_ID;
    final KafkaStreamsConfiguration baseStreamConfig;
    KafkaStreams kafkaStreams;

    public abstract StreamsBuilder processStream(StreamsBuilder builder);

    @EventListener(ApplicationStartedEvent.class)
    public void process() {
        final var builder = processStream(new StreamsBuilder());
        final var props = baseStreamConfig.asProperties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, APP_ID);
        kafkaStreams = new KafkaStreams(builder.build(), props);
        kafkaStreams.start();
    }

    @PreDestroy
    public void cleanUp() { kafkaStreams.close(); }

    public AbstractKStreamService(KafkaStreamsConfiguration baseStreamConfig, String APP_ID) {
        this.APP_ID = APP_ID;
        this.baseStreamConfig = baseStreamConfig;
    }
}
