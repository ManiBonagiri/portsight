package com.portsight.api.config;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    // ── Kafka Admin (non-fatal if broker unavailable) ─────────────────────────

    @Bean
    public KafkaAdmin kafkaAdmin() {
        KafkaAdmin admin = new KafkaAdmin(
                Map.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers));
        admin.setFatalIfBrokerNotAvailable(false);
        return admin;
    }

    // ── Topic Definitions ─────────────────────────────────────────────────────

    @Bean
    public NewTopic portfolioCreatedTopic() {
        return TopicBuilder.name("portfolio-created").partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic transactionCreatedTopic() {
        return TopicBuilder.name("transaction-created").partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic transactionReversedTopic() {
        return TopicBuilder.name("transaction-reversed").partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic reportGeneratedTopic() {
        return TopicBuilder.name("report-generated").partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic riskCalculatedTopic() {
        return TopicBuilder.name("risk-calculated").partitions(1).replicas(1).build();
    }

    // ── Producer Factory ──────────────────────────────────────────────────────

    @Bean
    public ProducerFactory<String, String> producerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.ACKS_CONFIG, "1");
        config.put(ProducerConfig.RETRIES_CONFIG, 3);
        config.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, 1000);
        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public KafkaTemplate<String, String> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}