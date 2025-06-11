package com.example.kafkademo3;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.MessageListenerContainer;

import org.springframework.stereotype.Component;
import java.util.List;

/**
 * Kafka错误处理器
 */
@Component
public class KafkaErrorHandler extends DefaultErrorHandler {

    private static final Logger logger = LoggerFactory.getLogger(KafkaErrorHandler.class);

    @Override
    public void handleRemaining(Exception thrownException, List<ConsumerRecord<?, ?>> records,
                                Consumer<?, ?> consumer, MessageListenerContainer container,
                                boolean batchListener) {
        for (ConsumerRecord<?, ?> record : records) {
            logger.error("Kafka listener error - Record: {}, Exception: {}",
                    record, thrownException.getMessage(), thrownException);
        }

        // 调用父类实现以便遵循默认的失败处理策略
        super.handleRemaining(thrownException, records, consumer, container, batchListener);
    }
}
