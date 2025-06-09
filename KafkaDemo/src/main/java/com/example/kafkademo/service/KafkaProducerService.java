package com.example.kafkademo.service;

import com.example.kafkademo.model.UserEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
public class KafkaProducerService {

    private static final Logger logger = LoggerFactory.getLogger(KafkaProducerService.class);
    private static final String USER_EVENTS_TOPIC = "user-events";
    private static final String ORDER_EVENTS_TOPIC = "order-events";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    public KafkaProducerService(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Send user event asynchronously with callback
     */
    public void sendUserEvent(UserEvent userEvent) {
        try {
            String key = userEvent.getUserId();

//            ListenableFuture<SendResult<String, Object>> future =
//                    kafkaTemplate.send(USER_EVENTS_TOPIC, key, userEvent);
//
//            future.addCallback(new ListenableFutureCallback<SendResult<String, Object>>() {
//                @Override
//                public void onSuccess(SendResult<String, Object> result) {
//                    logger.info("User event sent successfully: topic={}, partition={}, offset={}, key={}",
//                            result.getRecordMetadata().topic(),
//                            result.getRecordMetadata().partition(),
//                            result.getRecordMetadata().offset(),
//                            key);
//                }
//
//                @Override
//                public void onFailure(Throwable ex) {
//                    logger.error("Failed to send user event: key={}, error={}", key, ex.getMessage(), ex);
//                    handleFailure(userEvent, ex);
//                }
//            });
            CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(USER_EVENTS_TOPIC, key, userEvent);
            future.whenComplete((result, ex) -> {
                if (ex != null) {
                    logger.error("Failed to send user event: key={}, error={}", key, ex.getMessage(), ex);
                    handleFailure(userEvent, ex);
                } else {
                    logger.info("User event sent successfully: topic={}, partition={}, offset={}, key={}",
                            result.getRecordMetadata().topic(),
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset(),
                            key);
                }
            });

        } catch (Exception e) {
            logger.error("Exception occurred while sending user event: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to send user event", e);
        }
    }

    /**
     * Send event synchronously with timeout
     */
    public SendResult<String, Object> sendUserEventSync(UserEvent userEvent, long timeoutMs) {
        try {
            String key = userEvent.getUserId();

            CompletableFuture<SendResult<String, Object>> future =
                    kafkaTemplate.send(USER_EVENTS_TOPIC, key, userEvent);

            SendResult<String, Object> result = future.get(timeoutMs, TimeUnit.MILLISECONDS);

            logger.info("User event sent synchronously: topic={}, partition={}, offset={}, key={}",
                    result.getRecordMetadata().topic(),
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset(),
                    key);

            return result;

        } catch (Exception e) {
            logger.error("Failed to send user event synchronously: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to send user event synchronously", e);
        }
    }

    /**
     * Send to specific partition
     */
    public void sendToPartition(String topic, String key, Object message, int partition) {
        try {
//            ListenableFuture<SendResult<String, Object>> future =
//                    kafkaTemplate.send(topic, partition, key, message);
//
//            future.addCallback(
//                    result -> logger.info("Message sent to partition {}: offset={}",
//                            partition, result.getRecordMetadata().offset()),
//                    ex -> logger.error("Failed to send to partition {}: {}", partition, ex.getMessage(), ex)
//            );

            CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(topic, partition, key, message);

            future.whenComplete((result, ex) -> {
                if (ex != null) {
                    logger.error("Failed to send to partition {}: {}", partition, ex.getMessage(), ex);
                } else {
                    logger.info("Message sent to partition {}: offset={}",
                            partition, result.getRecordMetadata().offset());
                }
            });

        } catch (Exception e) {
            logger.error("Exception sending to partition {}: {}", partition, e.getMessage(), e);
            throw new RuntimeException("Failed to send to partition", e);
        }
    }

    /**
     * Send batch of events
     */
    public void sendBatchUserEvents(java.util.List<UserEvent> events) {
        events.parallelStream().forEach(this::sendUserEvent);
    }

    private void handleFailure(UserEvent userEvent, Throwable ex) {
        // Implement retry logic, dead letter queue, or alerting
        logger.error("Event send failure handling for user: {}, correlation: {}",
                userEvent.getUserId(), userEvent.getCorrelationId());

        // Could implement:
        // 1. Retry with exponential backoff
        // 2. Send to dead letter topic
        // 3. Store in database for retry
        // 4. Send alert/notification
    }

    /**
     * Flush all pending messages
     */
    public void flush() {
        kafkaTemplate.flush();
        logger.info("Kafka producer flushed all pending messages");
    }
}
