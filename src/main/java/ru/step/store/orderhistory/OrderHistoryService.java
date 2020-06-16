package ru.step.store.orderhistory;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Transformer;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.springframework.kafka.config.KafkaStreamsConfiguration;
import org.springframework.stereotype.Service;
import ru.step.store.common.AbstractKStreamService;
import ru.step.store.common.Schemas;

import java.nio.charset.Charset;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OrderHistoryService extends AbstractKStreamService {
    private static final String APP_ID = "order-history-service";
    private static final String HISTORY_TOPIC = "orders-history";

    public OrderHistoryService(KafkaStreamsConfiguration baseStreamConfig) {
        super(baseStreamConfig, APP_ID);
    }

    @Override
    public StreamsBuilder processStream(StreamsBuilder builder) {
        final KStream<UUID, String> allTopics = builder.stream(
                Schemas.Topics.ALL.stream().map(Schemas.Topic::getName).collect(Collectors.toList()),
                Consumed.with(Serdes.UUID(), Serdes.String())
        );
        allTopics.transform(TopicDetailsTransformer::new)
                .to(HISTORY_TOPIC);

        return builder;
    }

    private static class TopicDetailsTransformer implements
            Transformer<UUID, String, KeyValue<String, String>> {

        private ProcessorContext context;

        @Override
        public void init(ProcessorContext context) {
            this.context = context;
        }

        @Override
        public KeyValue<String, String> transform(UUID key, String value) {
            context.headers().add(new RecordHeader("from", context.topic().getBytes(Charset.defaultCharset())));
            return KeyValue.pair(key.toString(), context.topic() + ": " + value);
        }

        @Override
        public void close() {
        }
    }
}
