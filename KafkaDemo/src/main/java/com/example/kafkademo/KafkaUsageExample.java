package com.example.kafkademo;

import com.example.kafkademo.model.UserEvent;
import com.example.kafkademo.service.KafkaProducerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class KafkaUsageExample implements CommandLineRunner {

    @Autowired
    private KafkaProducerService producerService;

    @Override
    public void run(String... args) throws Exception {
        // Example 1: Send single event
        UserEvent userEvent = new UserEvent(
                "user123",
                "USER_CREATED",
                "{\"name\":\"John Doe\",\"email\":\"john@example.com\"}",
                UUID.randomUUID().toString()
        );
        producerService.sendUserEvent(userEvent);

        // Example 2: Send batch events
        List<UserEvent> events = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            events.add(new UserEvent(
                    "user" + i,
                    "USER_UPDATED",
                    "{\"action\":\"profile_update\"}",
                    UUID.randomUUID().toString()
            ));
        }
        producerService.sendBatchUserEvents(events);

        // Example 3: Send to specific partition
        producerService.sendToPartition("user-events", "user456", userEvent, 0);
    }
}
