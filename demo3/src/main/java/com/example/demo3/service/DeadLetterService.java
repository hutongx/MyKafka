package com.example.demo3.service;

import com.example.demo3.model.entity.DeadLetterEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeadLetterService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendToDeadLetter(String dlqTopic, Object event, String errorMessage) {
        try {
            DeadLetterEvent dlqEvent = DeadLetterEvent.builder()
                    .originalEvent(event)
                    .errorMessage(errorMessage)
                    .timestamp(LocalDateTime.now())
                    .retryCount(0)
                    .build();

            kafkaTemplate.send(dlqTopic, dlqEvent);
            log.info("Sent event to dead letter queue: {}", dlqTopic);

        } catch (Exception e) {
            log.error("Failed to send event to dead letter queue: {}", dlqTopic, e);
        }
    }
}
