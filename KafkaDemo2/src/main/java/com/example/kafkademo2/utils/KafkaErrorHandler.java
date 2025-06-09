package com.example.kafkademo2.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.listener.ConsumerAwareListenerErrorHandler;
import org.springframework.kafka.listener.ListenerExecutionFailedException;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class KafkaErrorHandler implements ConsumerAwareListenerErrorHandler {

    @Override
    public Object handleError(Message<?> message, ListenerExecutionFailedException exception, Consumer<?, ?> consumer) {
        log.error("Error in Kafka listener: {}", exception.getMessage(), exception);

        if (message.getPayload() instanceof ConsumerRecord) {
            ConsumerRecord<?, ?> record = (ConsumerRecord<?, ?>) message.getPayload();
            log.error("Error processing record - Topic: {}, Partition: {}, Offset: {}, Key: {}",
                    record.topic(), record.partition(), record.offset(), record.key());

            // Handle specific error scenarios
            handleSpecificError(record, exception);
        }

        return null;
    }

    private void handleSpecificError(ConsumerRecord<?, ?> record, Exception exception) {
        // Implement specific error handling logic
        // Examples:
        // - Send to dead letter queue
        // - Log to monitoring system
        // - Send alerts
        // - Store failed records for manual processing

        if (exception instanceof org.springframework.kafka.support.serializer.DeserializationException) {
            log.error("Deserialization error for record: {}", record);
            // Handle deserialization errors
        } else if (exception instanceof org.springframework.dao.DataAccessException) {
            log.error("Database error while processing record: {}", record);
            // Handle database errors
        } else {
            log.error("General processing error for record: {}", record);
            // Handle other errors
        }
    }
}
