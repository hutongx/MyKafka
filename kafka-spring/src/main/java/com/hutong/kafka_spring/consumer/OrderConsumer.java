package com.hutong.kafka_spring.consumer;

import com.hutong.kafka_spring.domain.OrderMessage;
import org.slf4j.*;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

@Service
public class OrderConsumer {
    private static final Logger log = LoggerFactory.getLogger(OrderConsumer.class);

    @KafkaListener(topics = "#{@appKafkaProperties.topics['orders']}",
            containerFactory = "kafkaListenerContainerFactory")
    public void onMessage(OrderMessage order, Acknowledgment ack) {
        log.info("Consuming order={}, amount={}", order.getOrderId(), order.getAmount());
        try {
            // 1. 业务逻辑，例如保存数据库
            // orderService.process(order);

            // 2. 手动提交 offset
            ack.acknowledge();
        } catch (Exception ex) {
            log.error("Processing failed, will retry or dead-letter", ex);
            throw ex; // 交给 DefaultErrorHandler
        }
    }
}


