package com.example.demo2.utils;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.support.ProducerListener;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class KafkaProducerListener implements ProducerListener<String, Object> {

    private static final Logger logger = LoggerFactory.getLogger(KafkaProducerListener.class);

    // Metrics
    private final AtomicLong successCount = new AtomicLong(0);
    private final AtomicLong errorCount = new AtomicLong(0);
    private final ConcurrentHashMap<String, AtomicLong> topicMetrics = new ConcurrentHashMap<>();

    // Micrometer metrics (if available)
    private Counter successCounter;
    private Counter errorCounter;
    private Timer sendTimer;

    @Autowired(required = false)
    public void setMeterRegistry(MeterRegistry meterRegistry) {
        if (meterRegistry != null) {
            this.successCounter = Counter.builder("kafka.producer.send.success")
                    .description("Number of successful message sends")
                    .register(meterRegistry);

            this.errorCounter = Counter.builder("kafka.producer.send.error")
                    .description("Number of failed message sends")
                    .register(meterRegistry);

            this.sendTimer = Timer.builder("kafka.producer.send.duration")
                    .description("Time taken to send messages")
                    .register(meterRegistry);
        }
    }

    @Override
    public void onSuccess(ProducerRecord<String, Object> producerRecord, RecordMetadata recordMetadata) {
        // Update metrics
        successCount.incrementAndGet();
        updateTopicMetrics(producerRecord.topic(), true);

        // Update Micrometer metrics
        if (successCounter != null) {
            successCounter.increment();
        }

        // Log success with detailed information
        logger.info("Message sent successfully: topic={}, partition={}, offset={}, key={}, timestamp={}, serializedKeySize={}, serializedValueSize={}",
                recordMetadata.topic(),
                recordMetadata.partition(),
                recordMetadata.offset(),
                producerRecord.key(),
                recordMetadata.timestamp(),
                recordMetadata.serializedKeySize(),
                recordMetadata.serializedValueSize());

        // Log partition and leader information for monitoring
        logger.debug("Message details: topic={}, partition={}, offset={}, key={}, headers={}",
                recordMetadata.topic(),
                recordMetadata.partition(),
                recordMetadata.offset(),
                producerRecord.key(),
                producerRecord.headers());
    }

    @Override
    public void onError(ProducerRecord<String, Object> producerRecord, RecordMetadata recordMetadata, Exception exception) {
        // Update metrics
        errorCount.incrementAndGet();
        updateTopicMetrics(producerRecord.topic(), false);

        // Update Micrometer metrics
        if (errorCounter != null) {
            errorCounter.increment();
        }

        // Log error with detailed information
        logger.error("Failed to send message: topic={}, partition={}, key={}, value={}, error={}",
                producerRecord.topic(),
                producerRecord.partition(),
                producerRecord.key(),
                producerRecord.value(),
                exception.getMessage(),
                exception);

        // Log additional context for debugging
        logger.error("Producer record details: headers={}, timestamp={}",
                producerRecord.headers(),
                producerRecord.timestamp());

        // Handle specific error types
        handleSpecificErrors(exception, producerRecord);
    }

    private void updateTopicMetrics(String topic, boolean success) {
        String metricKey = topic + (success ? ".success" : ".error");
        topicMetrics.computeIfAbsent(metricKey, k -> new AtomicLong(0)).incrementAndGet();
    }

    private void handleSpecificErrors(Exception exception, ProducerRecord<String, Object> producerRecord) {
        String errorType = exception.getClass().getSimpleName();

        switch (errorType) {
            case "TimeoutException":
                logger.warn("Timeout occurred while sending message to topic: {}, key: {}",
                        producerRecord.topic(), producerRecord.key());
                break;

            case "NotLeaderForPartitionException":
                logger.warn("Not leader for partition error - topic: {}, partition: {}, key: {}",
                        producerRecord.topic(), producerRecord.partition(), producerRecord.key());
                break;

            case "RecordTooLargeException":
                logger.error("Record too large - topic: {}, key: {}, consider reducing message size",
                        producerRecord.topic(), producerRecord.key());
                break;

            case "InvalidTopicException":
                logger.error("Invalid topic - topic: {}, key: {}",
                        producerRecord.topic(), producerRecord.key());
                break;

            case "SerializationException":
                logger.error("Serialization error - topic: {}, key: {}, check message format",
                        producerRecord.topic(), producerRecord.key());
                break;

            default:
                logger.error("Unexpected error type: {} - topic: {}, key: {}",
                        errorType, producerRecord.topic(), producerRecord.key());
        }
    }

    // Metrics getters for monitoring endpoints
    public long getSuccessCount() {
        return successCount.get();
    }

    public long getErrorCount() {
        return errorCount.get();
    }

    public double getSuccessRate() {
        long total = successCount.get() + errorCount.get();
        return total > 0 ? (double) successCount.get() / total * 100.0 : 0.0;
    }

    public ConcurrentHashMap<String, AtomicLong> getTopicMetrics() {
        return new ConcurrentHashMap<>(topicMetrics);
    }

    public void resetMetrics() {
        successCount.set(0);
        errorCount.set(0);
        topicMetrics.clear();
        logger.info("Producer metrics reset at {}", LocalDateTime.now());
    }

    // Health check method
    public boolean isHealthy() {
        long total = successCount.get() + errorCount.get();
        if (total == 0) return true; // No messages sent yet

        double errorRate = (double) errorCount.get() / total;
        return errorRate < 0.05; // Less than 5% error rate is considered healthy
    }

    // Get metrics summary for monitoring
    public String getMetricsSummary() {
        long total = successCount.get() + errorCount.get();
        return String.format("Producer Metrics - Total: %d, Success: %d, Errors: %d, Success Rate: %.2f%%",
                total, successCount.get(), errorCount.get(), getSuccessRate());
    }
}
