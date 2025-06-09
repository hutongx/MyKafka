package com.example.kafkademo;

import com.example.kafkademo.model.UserEvent;
import com.example.kafkademo.service.KafkaProducerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;

import java.util.concurrent.TimeUnit;

@SpringBootTest
@DirtiesContext
@EmbeddedKafka(partitions = 1,
        brokerProperties = {"listeners=PLAINTEXT://localhost:9092", "port=9092"},
        topics = {"user-events", "order-events"})
public class KafkaIntegrationTest {

    @Autowired
    private KafkaProducerService producerService;

    @Test
    public void testSendUserEvent() throws Exception {
        UserEvent userEvent = new UserEvent(
                "testUser",
                "TEST_EVENT",
                "test data",
                "correlation-123"
        );

        producerService.sendUserEvent(userEvent);

        // Wait for async processing
        TimeUnit.SECONDS.sleep(2);

        // Verify event was processed (check logs or mock verification)
        // This would typically involve checking database state or mock calls
    }
}
