package com.portsight.api.shared.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class OutboxEventHelper {

    private static final Logger log = LoggerFactory.getLogger(OutboxEventHelper.class);

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public OutboxEventHelper(OutboxEventRepository outboxEventRepository, ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Write an outbox event for async Kafka publishing.
     *
     * @param aggregateType e.g. "Portfolio", "Transaction", "Report"
     * @param aggregateId   the entity UUID as string
     * @param eventType     e.g. "CREATED", "REVERSED", "GENERATED"
     * @param payload       map of fields to include in the event JSON
     *
     *                      Topic produced = (aggregateType + "-" +
     *                      eventType).toLowerCase()
     *                      e.g. "Portfolio" + "CREATED" -> "portfolio-created"
     */
    public void publish(String aggregateType, String aggregateId,
            String eventType, Map<String, Object> payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            OutboxEvent event = OutboxEvent.builder()
                    .aggregateType(aggregateType)
                    .aggregateId(aggregateId)
                    .eventType(eventType)
                    .payload(json)
                    .status("PENDING")
                    .build();
            outboxEventRepository.save(event);
            log.debug("Outbox event queued: {}-{} for id={}", aggregateType, eventType, aggregateId);
        } catch (Exception e) {
            // Non-fatal — log and continue. Event will be missing from Kafka
            // but the primary transaction (portfolio/tx save) must not roll back.
            log.error("Failed to write outbox event {}-{} for id={}: {}",
                    aggregateType, eventType, aggregateId, e.getMessage());
        }
    }
}