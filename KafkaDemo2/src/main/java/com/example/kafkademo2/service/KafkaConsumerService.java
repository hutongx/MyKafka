package com.example.kafkademo2.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
//import org.apache.kafka.common.header.Header;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
public class KafkaConsumerService {

    @Autowired
    private ObjectMapper objectMapper;

    // Single message consumer with manual acknowledgment
    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 1000, multiplier = 2.0),
            autoCreateTopics = "true",
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE,
            dltStrategy = org.springframework.kafka.retrytopic.DltStrategy.FAIL_ON_ERROR,
            include = {Exception.class}
    )
    @KafkaListener(topics = "${kafka.topics.user-events}", groupId = "user-events-consumer-group")
    public void consumeUserEvent(
            @Payload String message,
            ConsumerRecord<String, String> record,
            Acknowledgment acknowledgment,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        try {
            log.info("Consuming user event - Topic: {}, Partition: {}, Offset: {}, Key: {}",
                    topic, partition, offset, record.key());

            // Extract custom headers
            // Header timestampHeader = record.headers().lastHeader("timestamp");
            // Header sourceHeader = record.headers().lastHeader("source");
            org.apache.kafka.common.header.Header timestampHeader = record.headers().lastHeader("timestamp");
            org.apache.kafka.common.header.Header sourceHeader = record.headers().lastHeader("source");

            if (timestampHeader != null) {
                String timestamp = new String(timestampHeader.value(), StandardCharsets.UTF_8);
                log.info("Message timestamp: {}", timestamp);
            }

            if (sourceHeader != null) {
                String source = new String(sourceHeader.value(), StandardCharsets.UTF_8);
                log.info("Message source: {}", source);
            }

            // Process message (replace with actual business logic)
            processUserEvent(message, record.key());

            // Manual acknowledgment after successful processing
            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("Error processing user event - Key: {}, Message: {}, Error: {}",
                    record.key(), message, e.getMessage(), e);
            throw e; // Rethrow to trigger retry mechanism
        }
    }

    // Batch message consumer
    @KafkaListener(
            topics = "${kafka.topics.order-events}",
            groupId = "order-events-batch-consumer-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeOrderEventsBatch(
            List<ConsumerRecord<String, String>> records,
            Acknowledgment acknowledgment) {

        try {
            log.info("Consuming batch of {} order events", records.size());

            // Process all records in batch
            for (ConsumerRecord<String, String> record : records) {
                log.info("Processing order event - Key: {}, Partition: {}, Offset: {}",
                        record.key(), record.partition(), record.offset());
                processOrderEvent(record.value(), record.key());
            }

            // Acknowledge entire batch after successful processing
            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("Error processing order events batch: {}", e.getMessage(), e);
            throw e;
        }
    }

    // Consumer with specific group and concurrent processing
    @KafkaListener(
            topics = "${kafka.topics.user-events}",
            groupId = "user-events-analytics-group",
            concurrency = "2"
    )
    public void consumeUserEventForAnalytics(
            ConsumerRecord<String, String> record,
            Acknowledgment acknowledgment) {

        CompletableFuture.runAsync(() -> {
            try {
                log.info("Analytics processing - Key: {}, Partition: {}, Offset: {}",
                        record.key(), record.partition(), record.offset());

                // Simulate analytics processing
                processUserEventAnalytics(record.value(), record.key());
                acknowledgment.acknowledge();

            } catch (Exception e) {
                log.error("Error in analytics processing: {}", e.getMessage(), e);
            }
        });
    }

    // Dead Letter Topic consumer
    @KafkaListener(topics = "${kafka.topics.user-events}-dlt", groupId = "dlt-consumer-group")
    public void consumeFromDLT(
            ConsumerRecord<String, String> record,
            Acknowledgment acknowledgment) {

        log.error("Message received in DLT - Key: {}, Value: {}, Headers: {}",
                record.key(), record.value(), record.headers());

        // Handle failed messages (e.g., send to monitoring, manual review queue, etc.)
        handleFailedMessage(record);
        acknowledgment.acknowledge();
    }

    // Business logic methods
    private void processUserEvent(String message, String key) {
        try {
            // Parse JSON message
            Object userEvent = objectMapper.readValue(message, Object.class);
            log.info("Processing user event for key: {} with data: {}", key, userEvent);

            // Add your business logic here
            Thread.sleep(100); // Simulate processing time

        } catch (Exception e) {
            log.error("Failed to process user event: {}", e.getMessage(), e);
            throw new RuntimeException("User event processing failed", e);
        }
    }

    private void processOrderEvent(String message, String key) {
        try {
            Object orderEvent = objectMapper.readValue(message, Object.class);
            log.info("Processing order event for key: {} with data: {}", key, orderEvent);

            // Add your business logic here
            Thread.sleep(50); // Simulate processing time

        } catch (Exception e) {
            log.error("Failed to process order event: {}", e.getMessage(), e);
            throw new RuntimeException("Order event processing failed", e);
        }
    }

    private void processUserEventAnalytics(String message, String key) {
        try {
            Object analyticsData = objectMapper.readValue(message, Object.class);
            log.info("Processing analytics for key: {} with data: {}", key, analyticsData);

            // Add analytics processing logic here
            Thread.sleep(200); // Simulate analytics processing time

        } catch (Exception e) {
            log.error("Failed to process analytics: {}", e.getMessage(), e);
        }
    }

    private void handleFailedMessage(ConsumerRecord<String, String> record) {
        // Implement dead letter handling logic
        // Examples: store in database, send alert, forward to manual review queue
        log.warn("Handling failed message - implementing custom DLT logic for key: {}", record.key());
    }
}
