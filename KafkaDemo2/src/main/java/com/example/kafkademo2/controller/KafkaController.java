package com.example.kafkademo2.controller;

import com.example.kafkademo2.service.KafkaProducerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.support.SendResult;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

@Slf4j
@RestController
@RequestMapping("/api/kafka")
public class KafkaController {

    @Autowired
    private KafkaProducerService kafkaProducerService;

    // Send user event asynchronously
    @PostMapping("/user-event")
    public ResponseEntity<Map<String, String>> sendUserEvent(
            @RequestParam(required = false) String key,
            @RequestBody Map<String, Object> eventData) {

        try {
            String messageKey = key != null ? key : UUID.randomUUID().toString();

            // Add metadata to the event
            Map<String, Object> enrichedEvent = new HashMap<>(eventData);
            enrichedEvent.put("eventId", UUID.randomUUID().toString());
            enrichedEvent.put("eventType", "USER_EVENT");
            enrichedEvent.put("timestamp", System.currentTimeMillis());

            kafkaProducerService.sendUserEventAsync(messageKey, enrichedEvent);

            Map<String, String> response = new HashMap<>();
            response.put("status", "Message sent asynchronously");
            response.put("key", messageKey);
            response.put("type", "USER_EVENT");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error sending user event: {}", e.getMessage(), e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to send message");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    // Send order event synchronously
    @PostMapping("/order-event")
    public ResponseEntity<Map<String, Object>> sendOrderEvent(
            @RequestParam(required = false) String key,
            @RequestBody Map<String, Object> eventData) {

        try {
            String messageKey = key != null ? key : UUID.randomUUID().toString();

            // Add metadata to the event
            Map<String, Object> enrichedEvent = new HashMap<>(eventData);
            enrichedEvent.put("eventId", UUID.randomUUID().toString());
            enrichedEvent.put("eventType", "ORDER_EVENT");
            enrichedEvent.put("timestamp", System.currentTimeMillis());

            SendResult<String, String> result = kafkaProducerService.sendOrderEventSync(messageKey, enrichedEvent);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "Message sent successfully");
            response.put("key", messageKey);
            response.put("type", "ORDER_EVENT");
            response.put("topic", result.getRecordMetadata().topic());
            response.put("partition", result.getRecordMetadata().partition());
            response.put("offset", result.getRecordMetadata().offset());

            return ResponseEntity.ok(response);

        } catch (ExecutionException | InterruptedException e) {
            log.error("Error sending order event: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to send message");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    // Send to specific partition
    @PostMapping("/send-to-partition")
    public ResponseEntity<Map<String, String>> sendToPartition(
            @RequestParam String topic,
            @RequestParam Integer partition,
            @RequestParam(required = false) String key,
            @RequestBody Map<String, Object> eventData) {

        try {
            String messageKey = key != null ? key : UUID.randomUUID().toString();

            Map<String, Object> enrichedEvent = new HashMap<>(eventData);
            enrichedEvent.put("eventId", UUID.randomUUID().toString());
            enrichedEvent.put("timestamp", System.currentTimeMillis());
            enrichedEvent.put("targetPartition", partition);

            kafkaProducerService.sendToPartition(topic, partition, messageKey, enrichedEvent);

            Map<String, String> response = new HashMap<>();
            response.put("status", "Message sent to partition " + partition);
            response.put("key", messageKey);
            response.put("topic", topic);
            response.put("partition", partition.toString());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error sending to partition: {}", e.getMessage(), e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to send message to partition");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    // Health check endpoint
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "Kafka service is running");
        response.put("timestamp", String.valueOf(System.currentTimeMillis()));
        return ResponseEntity.ok(response);
    }
}
