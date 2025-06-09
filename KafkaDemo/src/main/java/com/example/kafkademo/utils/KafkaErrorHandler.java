package com.example.kafkademo.utils;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.listener.ConsumerAwareListenerErrorHandler;
import org.springframework.kafka.listener.ListenerExecutionFailedException;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

@Component
public class KafkaErrorHandler implements ConsumerAwareListenerErrorHandler {

    private static final Logger logger = LoggerFactory.getLogger(KafkaErrorHandler.class);

    @Override
    public Object handleError(Message<?> message, ListenerExecutionFailedException exception,
                              Consumer<?, ?> consumer) {

        logger.error("Kafka listener error: topic={}, partition={}, offset={}, error={}",
                message.getHeaders().get("kafka_receivedTopic"),
                message.getHeaders().get("kafka_receivedPartitionId"),
                message.getHeaders().get("kafka_offset"),
                exception.getMessage(), exception);

        // Handle error - could implement:
        // 1. Send to DLT
        // 2. Log to monitoring system
        // 3. Send notification
        // 4. Store for retry

        return null; // or return processed result
    }
}
