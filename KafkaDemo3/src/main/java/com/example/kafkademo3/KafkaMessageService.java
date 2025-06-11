package com.example.kafkademo3;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Spring Boot Kafka服务层示例
 */
@Service
class KafkaMessageService {

    private static final Logger logger = LoggerFactory.getLogger(KafkaMessageService.class);

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    /**
     * 发送消息到指定主题
     */
    public void sendMessage(String topic, String key, String message) {
        try {
            /**
            kafkaTemplate.send(topic, key, message)
                    .addCallback(
                            result -> logger.info("Message sent successfully - Topic: {}, Key: {}", topic, key),
                            failure -> logger.error("Failed to send message - Topic: {}, Key: {}, Error: {}",
                                    topic, key, failure.getMessage())
                    );*/
            kafkaTemplate.send(topic, key, message).whenComplete((result, ex) -> {
                if (ex == null) {
                    logger.info("Message sent successfully - Topic: {}, Key: {}, Partition: {}, Offset: {}",
                            topic, key, result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
                } else {
                    logger.error("Failed to send message - Topic: {}, Key: {}, Error: {}", topic, key, ex.getMessage());
                }
            });
        } catch (Exception e) {
            logger.error("Error sending message to topic: {}, key: {}, error: {}", topic, key, e.getMessage());
        }
    }

    /**
     * 监听消息
     */
    @KafkaListener(topics = "user-events", groupId = "user-event-processors")
    public void handleUserEvent(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {
        try {
            logger.info("Received user event - Key: {}, Value: {}, Partition: {}, Offset: {}",
                    record.key(), record.value(), record.partition(), record.offset());

            // 处理业务逻辑
            processUserEvent(record.key(), record.value());

            // 手动确认消息
            acknowledgment.acknowledge();

        } catch (Exception e) {
            logger.error("Error processing user event - Key: {}, Error: {}", record.key(), e.getMessage(), e);
            // 不确认消息，让其重试或进入死信队列
        }
    }

    /**
     * 批量消息监听
     */
    @KafkaListener(topics = "batch-events", groupId = "batch-processors")
    public void handleBatchEvents(List<ConsumerRecord<String, String>> records, Acknowledgment acknowledgment) {
        try {
            logger.info("Received batch of {} events", records.size());

            for (ConsumerRecord<String, String> record : records) {
                processUserEvent(record.key(), record.value());
            }

            // 批量确认
            acknowledgment.acknowledge();

        } catch (Exception e) {
            logger.error("Error processing batch events: {}", e.getMessage(), e);
        }
    }

    private void processUserEvent(String userId, String eventData) {
        // 实现具体的业务逻辑
        logger.debug("Processing event for user: {}", userId);
    }
}
