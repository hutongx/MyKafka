package com.example.kafkademo.service;

import com.example.kafkademo.model.UserEvent;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.atomic.AtomicLong;

@Service
public class KafkaConsumerService {

    private static final Logger logger = LoggerFactory.getLogger(KafkaConsumerService.class);
    private final AtomicLong messageCount = new AtomicLong(0);

    /**
     * Basic consumer with manual acknowledgment
     */
    @KafkaListener(
            topics = "user-events",
            groupId = "user-service-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeUserEvent(
            @Payload UserEvent userEvent,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {

        try {
            logger.info("Consuming user event: topic={}, partition={}, offset={}, key={}, user={}",
                    topic, partition, offset, key, userEvent.getUserId());

            // Process the event
            processUserEvent(userEvent);

            // Manual acknowledgment
            acknowledgment.acknowledge();

            long count = messageCount.incrementAndGet();
            if (count % 100 == 0) {
                logger.info("Processed {} messages", count);
            }

        } catch (Exception e) {
            logger.error("Error processing user event: key={}, offset={}, error={}",
                    key, offset, e.getMessage(), e);
            // Don't acknowledge on error - message will be redelivered
            throw e;
        }
    }

    /**
     * Consumer with retry mechanism
     */
    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 1000, multiplier = 2.0),
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE,
            dltStrategy = org.springframework.kafka.retrytopic.DltStrategy.FAIL_ON_ERROR,
            include = {Exception.class}
    )
    @KafkaListener(topics = "order-events", groupId = "order-service-group")
    public void consumeOrderEventWithRetry(@Payload String orderData,
                                           ConsumerRecord<String, String> record) {
        try {
            logger.info("Processing order event: key={}, partition={}, offset={}",
                    record.key(), record.partition(), record.offset());

            processOrderEvent(orderData);

        } catch (Exception e) {
            logger.error("Error processing order event: key={}, error={}",
                    record.key(), e.getMessage(), e);
            throw e; // Trigger retry
        }
    }

    /**
     * Batch consumer
     */
    @KafkaListener(
            topics = "user-events",
            groupId = "batch-consumer-group",
            containerFactory = "batchKafkaListenerContainerFactory"
    )
    public void consumeBatchUserEvents(
            java.util.List<ConsumerRecord<String, UserEvent>> records,
            Acknowledgment acknowledgment) {

        try {
            logger.info("Processing batch of {} user events", records.size());

            for (ConsumerRecord<String, UserEvent> record : records) {
                UserEvent userEvent = record.value();
                logger.debug("Batch processing: key={}, user={}",
                        record.key(), userEvent.getUserId());

                processUserEvent(userEvent);
            }

            acknowledgment.acknowledge();
            logger.info("Batch processed successfully");

        } catch (Exception e) {
            logger.error("Error processing batch: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Consumer with transaction support
     */
    @KafkaListener(topics = "user-events", groupId = "transactional-group")
    @Transactional
    public void consumeUserEventTransactional(@Payload UserEvent userEvent,
                                              @Header(KafkaHeaders.RECEIVED_KEY) String key) {
        try {
            logger.info("Processing user event transactionally: key={}, user={}",
                    key, userEvent.getUserId());

            // Transactional processing
            processUserEventTransactional(userEvent);

        } catch (Exception e) {
            logger.error("Transaction failed for user event: key={}, error={}",
                    key, e.getMessage(), e);
            throw e; // Rollback transaction
        }
    }

    /**
     * DLT (Dead Letter Topic) handler
     */
    @KafkaListener(topics = "order-events-dlt", groupId = "dlt-handler-group")
    public void handleDltEvent(@Payload String orderData,
                               @Header(KafkaHeaders.RECEIVED_KEY) String key,
                               @Header(KafkaHeaders.EXCEPTION_MESSAGE) String exceptionMessage) {

        logger.error("Processing DLT message: key={}, exception={}", key, exceptionMessage);

        // Handle failed messages - could:
        // 1. Log to monitoring system
        // 2. Save to database for manual review
        // 3. Send notification to operations team
        // 4. Attempt alternative processing

        handleFailedMessage(orderData, key, exceptionMessage);
    }

    private void processUserEvent(UserEvent userEvent) {
        // Simulate processing time
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Business logic processing
        logger.debug("User event processed: type={}, correlation={}",
                userEvent.getEventType(), userEvent.getCorrelationId());
    }

    private void processOrderEvent(String orderData) {
        // Simulate processing
        if (orderData.contains("invalid")) {
            throw new IllegalArgumentException("Invalid order data");
        }

        logger.debug("Order event processed: {}", orderData);
    }

    private void processUserEventTransactional(UserEvent userEvent) {
        // Database operations within transaction
        logger.debug("Transactional processing for user: {}", userEvent.getUserId());

        // If any operation fails here, the transaction will rollback
        // and the Kafka message will not be committed
    }

    private void handleFailedMessage(String data, String key, String error) {
        // Implement DLT handling logic
        logger.warn("DLT message handled: key={}, data={}, error={}", key, data, error);
    }
}
