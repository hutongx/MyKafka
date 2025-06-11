package com.example.kafkademo3;

import org.apache.kafka.clients.consumer.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.listener.ListenerExecutionFailedException;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

/**
 * Kafka错误处理器
 */
@Component
class KafkaErrorHandler implements org.springframework.kafka.listener.ConsumerAwareListenerErrorHandler {

    private static final Logger logger = LoggerFactory.getLogger(KafkaErrorHandler.class);

    @Override
    public Object handleError(Message<?> message, ListenerExecutionFailedException exception,
                              Consumer<?, ?> consumer) {
        logger.error("Kafka listener error - Message: {}, Exception: {}",
                message.getPayload(), exception.getMessage(), exception);

        // 实现错误处理逻辑
        // 例如：发送到死信队列、记录错误日志、发送告警等

        return null;
    }
}
