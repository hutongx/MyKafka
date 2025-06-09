package com.example.kafkademo.controller;

import com.example.kafkademo.model.DTOs.SendEventResponse;
import com.example.kafkademo.model.DTOs.UserEventRequest;
import com.example.kafkademo.model.UserEvent;
import com.example.kafkademo.service.KafkaProducerService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.support.SendResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/kafka")
public class KafkaController {

    private static final Logger logger = LoggerFactory.getLogger(KafkaController.class);
    private final KafkaProducerService producerService;

    @Autowired
    public KafkaController(KafkaProducerService producerService) {
        this.producerService = producerService;
    }

    /**
     * Send user event asynchronously
     */
    @PostMapping("/user-events")
    public ResponseEntity<String> sendUserEvent(@Valid @RequestBody UserEventRequest request) {
        try {
            UserEvent userEvent = new UserEvent(
                    request.getUserId(),
                    request.getEventType(),
                    request.getEventData(),
                    UUID.randomUUID().toString()
            );

            producerService.sendUserEvent(userEvent);

            logger.info("User event sent async: user={}, type={}",
                    request.getUserId(), request.getEventType());

            return ResponseEntity.ok("Event sent successfully");

        } catch (Exception e) {
            logger.error("Failed to send user event: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body("Failed to send event: " + e.getMessage());
        }
    }

    /**
     * Send user event synchronously
     */
    @PostMapping("/user-events/sync")
    public ResponseEntity<SendEventResponse> sendUserEventSync(@Valid @RequestBody UserEventRequest request) {
        try {
            UserEvent userEvent = new UserEvent(
                    request.getUserId(),
                    request.getEventType(),
                    request.getEventData(),
                    UUID.randomUUID().toString()
            );

            SendResult<String, Object> result = producerService.sendUserEventSync(userEvent, 5000);

            SendEventResponse response = new SendEventResponse(
                    "Event sent successfully",
                    result.getRecordMetadata().topic(),
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset(),
                    userEvent.getCorrelationId()
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Failed to send user event sync: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(new SendEventResponse("Failed to send event: " + e.getMessage()));
        }
    }

    /**
     * Send batch of user events
     */
    @PostMapping("/user-events/batch")
    public ResponseEntity<String> sendBatchUserEvents(@Valid @RequestBody List<UserEventRequest> requests) {
        try {
            List<UserEvent> userEvents = requests.stream()
                    .map(req -> new UserEvent(
                            req.getUserId(),
                            req.getEventType(),
                            req.getEventData(),
                            UUID.randomUUID().toString()
                    ))
                    .collect(java.util.stream.Collectors.toList());

            producerService.sendBatchUserEvents(userEvents);

            logger.info("Batch of {} user events sent", userEvents.size());
            return ResponseEntity.ok("Batch events sent successfully");

        } catch (Exception e) {
            logger.error("Failed to send batch user events: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body("Failed to send batch events: " + e.getMessage());
        }
    }

    /**
     * Send to specific partition
     */
    @PostMapping("/user-events/partition/{partition}")
    public ResponseEntity<String> sendToPartition(
            @PathVariable int partition,
            @Valid @RequestBody UserEventRequest request) {
        try {
            UserEvent userEvent = new UserEvent(
                    request.getUserId(),
                    request.getEventType(),
                    request.getEventData(),
                    UUID.randomUUID().toString()
            );

            producerService.sendToPartition("user-events", request.getUserId(), userEvent, partition);

            return ResponseEntity.ok("Event sent to partition " + partition);

        } catch (Exception e) {
            logger.error("Failed to send to partition {}: {}", partition, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body("Failed to send to partition: " + e.getMessage());
        }
    }

    /**
     * Flush producer
     */
    @PostMapping("/flush")
    public ResponseEntity<String> flushProducer() {
        try {
            producerService.flush();
            return ResponseEntity.ok("Producer flushed successfully");
        } catch (Exception e) {
            logger.error("Failed to flush producer: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body("Failed to flush producer: " + e.getMessage());
        }
    }
}
