package com.hutong.kafka_spring.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hutong.kafka_spring.domain.OrderMessage;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class OrderProducer {
    private static final Logger log = LoggerFactory.getLogger(OrderProducer.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String topic;

    public OrderProducer(KafkaTemplate<String, String> kafkaTemplate,
                         ObjectMapper objectMapper,
                         @Value("${app.kafka.topics.orders}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.topic = topic;
    }

    public void sendOrder(OrderMessage order) {
        String key = order.getOrderId();
        String payload;
        try {
            payload = objectMapper.writeValueAsString(order);
        } catch (Exception e) {
            log.error("Serialize failed", e);
            return;
        }
        kafkaTemplate.send(topic, key, payload);
        // 全局 ProducerListener 会处理成功/失败日志
    }
}

