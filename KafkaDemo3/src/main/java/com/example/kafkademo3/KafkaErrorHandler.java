package com.example.kafkademo3;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.listener.ConsumerAwareErrorHandler;
import org.springframework.stereotype.Component;

/**
 * Kafka错误处理器
 */
@Component
public class KafkaErrorHandler implements ConsumerAwareErrorHandler {

    private static final Logger logger = LoggerFactory.getLogger(KafkaErrorHandler.class);

    @Override
    public void handle(Exception thrownException, ConsumerRecord<?, ?> record, Consumer<?, ?> consumer) {
        logger.error("Kafka listener error - Record: {}, Exception: {}",
                record, thrownException.getMessage(), thrownException);

        // 实现错误处理逻辑
        // 例如：发送到死信队列、记录错误日志、发送告警等
    }
}
