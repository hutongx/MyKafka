package com.example.demo.consumer;

import com.example.demo.domain.OrderMessage;
import org.slf4j.*;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

@Service
public class OrderConsumer {
    private static final Logger log = LoggerFactory.getLogger(OrderConsumer.class);

    @KafkaListener(
            topics = "#{@appKafkaConsumerProperties.topics['orders']}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onMessage(OrderMessage order, Acknowledgment ack) {
        log.info("[Consumer] Received order={}, amount={}",
                order.getOrderId(), order.getAmount());
        try {
            // 1. 执行业务处理
            // orderService.process(order);

            // 2. 手动提交 offset
            ack.acknowledge();
        } catch (Exception ex) {
            // 抛出后由 DefaultErrorHandler 处理（重试→死信）
            throw ex;
        }
    }
}



