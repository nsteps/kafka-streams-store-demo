package ru.step.store;

import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.lifecycle.Startables;

import java.time.Duration;
import java.util.Map;
import java.util.stream.Stream;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = AbstractIntegrationTest.Initializer.class)
abstract class AbstractIntegrationTest {
    static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>();
//                .withReuse(true);

        static KafkaContainer kafka = new KafkaContainer();
//                .withNetwork(null)
//                .withReuse(true);

        public static Map<String, String> getProperties() {
            Startables.deepStart(Stream.of(postgres, kafka)).join();
            return Map.of(
                    "spring.datasource.url", postgres.getJdbcUrl(),
                    "spring.datasource.username", postgres.getUsername(),
                    "spring.datasource.password",postgres.getPassword(),
                    "spring.kafka.bootstrap-servers", kafka.getBootstrapServers()
            );
        }

        @Override
        public void initialize(ConfigurableApplicationContext context) {
            var env = context.getEnvironment();
            env.getPropertySources().addFirst(new MapPropertySource(
                    "testcontainers",
                    (Map) getProperties()
            ));
            startKafdrop();
        }

        private void startKafdrop() {
            final GenericContainer<?> kafdrop = new GenericContainer<>("obsidiandynamics/kafdrop")
                    .withNetwork(kafka.getNetwork())
                    .withEnv("KAFKA_BROKERCONNECT", kafka.getNetworkAliases().get(0) + ":9092")
                    .withExposedPorts(9000)
                    .withStartupTimeout(Duration.ofSeconds(5));
            kafdrop.start();
            System.out.println("Kafka url: http://" + kafka.getHost() + ":" + kafka.getMappedPort(9092));
            System.out.println("Kafdrop url: http://" + kafdrop.getHost() + ":" + kafdrop.getMappedPort(9000));
        }
    }
}
