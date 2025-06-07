package com.example.demo3.utils;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KafkaHealthIndicator implements HealthIndicator {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public Health health() {
        try {
            // Simple health check by getting metadata
            kafkaTemplate.getProducerFactory().createProducer().partitionsFor("health-check");
            return Health.up()
                    .withDetail("kafka", "Available")
                    .build();
        } catch (Exception e) {
            return Health.down()
                    .withDetail("kafka", "Unavailable")
                    .withException(e)
                    .build();
        }
    }
}
