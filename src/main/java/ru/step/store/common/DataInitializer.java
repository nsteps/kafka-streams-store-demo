package ru.step.store.common;

//@Component
//@RequiredArgsConstructor
//@Slf4j
//class DataInitializer {
//    private final KafkaTemplate<String, String> kafkaTemplate;
//    private final NewTopic orderTopic;
//
//    @EventListener(ApplicationStartedEvent.class)
//    public void produce() {
//        LongStream.range(0, 10).forEach(i -> {
//            var key = "kafka-" + i;
//            var value = "kafka" + LocalDateTime.now();
//            kafkaTemplate.send(orderTopic.name(), key, value).addCallback(result -> {
//                if (result != null) {
//                    final RecordMetadata recordMetadata = result.getRecordMetadata();
//                    log.info("produced to {}, {}, {}", recordMetadata.topic(), recordMetadata.partition(),
//                            recordMetadata.offset());
//                }
//            }, ex -> {
//
//            });
//        });
//        kafkaTemplate.flush();
//    }
//}