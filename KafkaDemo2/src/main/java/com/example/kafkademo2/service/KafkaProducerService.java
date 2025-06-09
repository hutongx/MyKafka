package com.example.kafkademo2.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
//import org.springframework.util.concurrent.ListenableFutureCallback;

import java.time.LocalDateTime;
import java.util.AbstractMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Slf4j
@Service
public class KafkaProducerService {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${kafka.topics.user-events}")
    private String userEventsTopic;

    @Value("${kafka.topics.order-events}")
    private String orderEventsTopic;

    // Asynchronous send with callback
    @Retryable(value = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public void sendUserEventAsync(String key, Object message) {
        try {
            String jsonMessage = objectMapper.writeValueAsString(message);
            ProducerRecord<String, String> record = new ProducerRecord<>(userEventsTopic, key, jsonMessage);
            record.headers().add("timestamp", LocalDateTime.now().toString().getBytes());
            record.headers().add("source", "user-service".getBytes());

            /**
            kafkaTemplate.send(record).addCallback(new ListenableFutureCallback<SendResult<String, String>>() {
                @Override
                public void onSuccess(SendResult<String, String> result) {
                    RecordMetadata metadata = result.getRecordMetadata();
                    log.info("Message sent successfully - Topic: {}, Partition: {}, Offset: {}, Key: {}",
                            metadata.topic(), metadata.partition(), metadata.offset(), key);
                }

                @Override
                public void onFailure(Throwable ex) {
                    log.error("Failed to send message with key: {}, Error: {}", key, ex.getMessage(), ex);
                }
            });*/
            // Asynchronous send with CompletableFuture
            kafkaTemplate.send(record).whenComplete((result, ex) -> {
                if (ex == null) {
                    RecordMetadata metadata = result.getRecordMetadata();
                    log.info("Message sent successfully - Topic: {}, Partition: {}, Offset: {}, Key: {}",
                            metadata.topic(), metadata.partition(), metadata.offset(), record.key());
                } else {
                    log.error("Failed to send message with key: {}, Error: {}", record.key(), ex.getMessage(), ex);
                }
            });
        } catch (JsonProcessingException e) {
            log.error("Error serializing message: {}", e.getMessage(), e);
            throw new RuntimeException("Message serialization failed", e);
        }
    }

    // Synchronous send
    public SendResult<String, String> sendOrderEventSync(String key, Object message)
            throws ExecutionException, InterruptedException {
        try {
            String jsonMessage = objectMapper.writeValueAsString(message);
            ProducerRecord<String, String> record = new ProducerRecord<>(orderEventsTopic, key, jsonMessage);
            record.headers().add("timestamp", LocalDateTime.now().toString().getBytes());
            record.headers().add("source", "order-service".getBytes());

            CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(record);
            SendResult<String, String> result = future.get();

            RecordMetadata metadata = result.getRecordMetadata();
            log.info("Message sent synchronously - Topic: {}, Partition: {}, Offset: {}, Key: {}",
                    metadata.topic(), metadata.partition(), metadata.offset(), key);

            return result;
        } catch (JsonProcessingException e) {
            log.error("Error serializing message: {}", e.getMessage(), e);
            throw new RuntimeException("Message serialization failed", e);
        }
    }

    // Send with specific partition
    public void sendToPartition(String topic, Integer partition, String key, Object message) {
        try {
            String jsonMessage = objectMapper.writeValueAsString(message);
            /**
            kafkaTemplate.send(topic, partition, key, jsonMessage)
                    .addCallback(
                            result -> log.info("Message sent to partition {} successfully", partition),
                            failure -> log.error("Failed to send message to partition {}: {}", partition, failure.getMessage())
                    );*/
            // Asynchronous send with CompletableFuture
            kafkaTemplate.send(topic, partition, key, jsonMessage).whenComplete((result, ex) -> {
                if (ex == null) {
                    RecordMetadata metadata = result.getRecordMetadata();
                    log.info("Message sent to partition {} successfully - Topic: {}, Partition: {}, Offset: {}, Key: {}",
                            partition, metadata.topic(), metadata.partition(), metadata.offset(), key);
                } else {
                    log.error("Failed to send message to partition {}: {}", partition, ex.getMessage(), ex);
                }
            });
        } catch (JsonProcessingException e) {
            log.error("Error serializing message: {}", e.getMessage(), e);
            throw new RuntimeException("Message serialization failed", e);
        }
    }

    // Batch send example
    /**
    public void sendBatch(String topic, java.util.List<org.springframework.util.Pair<String, Object>> messages) {
        messages.forEach(pair -> {
            String key = pair.getFirst();
            Object message = pair.getSecond();
            sendUserEventAsync(key, message);
        });
        kafkaTemplate.flush(); // Force send all batched messages
    }*/
    public void sendBatch(String topic, java.util.List<AbstractMap.SimpleEntry<String, Object>> messages) {
        messages.forEach(entry -> {
            String key = entry.getKey();
            Object message = entry.getValue();
            sendUserEventAsync(key, message);
        });
        kafkaTemplate.flush(); // Force send all batched messages
    }
}
