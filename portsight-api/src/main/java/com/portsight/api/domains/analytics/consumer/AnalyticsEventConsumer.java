package com.portsight.api.domains.analytics.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumes Kafka events relevant to analytics.
 *
 * Current behaviour:
 * - transaction-created → logs that analytics recalculation is needed
 * - portfolio-created → logs new portfolio registered
 *
 * The nightly MarketScheduler already handles actual recalculation at 02:00.
 * This consumer is the hook point for future real-time analytics triggers.
 */
@Component
public class AnalyticsEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsEventConsumer.class);

    private final ObjectMapper objectMapper;

    public AnalyticsEventConsumer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "transaction-created", groupId = "portsight-analytics-group", containerFactory = "kafkaListenerContainerFactory")
    public void onTransactionCreated(String message) {
        try {
            JsonNode node = objectMapper.readTree(message);
            String portfolioId = node.path("portfolioId").asText();
            String type = node.path("type").asText();
            String quantity = node.path("quantity").asText();
            log.info("[Analytics] Transaction event received — portfolioId={} type={} qty={}",
                    portfolioId, type, quantity);
            // Future: trigger real-time snapshot recalculation here
        } catch (Exception e) {
            log.error("[Analytics] Failed to process transaction-created event: {}", e.getMessage());
        }
    }

    @KafkaListener(topics = "portfolio-created", groupId = "portsight-analytics-group", containerFactory = "kafkaListenerContainerFactory")
    public void onPortfolioCreated(String message) {
        try {
            JsonNode node = objectMapper.readTree(message);
            String portfolioId = node.path("portfolioId").asText();
            String portfolioName = node.path("portfolioName").asText();
            log.info("[Analytics] Portfolio created event received — id={} name={}",
                    portfolioId, portfolioName);
        } catch (Exception e) {
            log.error("[Analytics] Failed to process portfolio-created event: {}", e.getMessage());
        }
    }
}