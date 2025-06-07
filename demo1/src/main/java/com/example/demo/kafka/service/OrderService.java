package com.example.demo.kafka.service;

import com.example.demo.kafka.model.Order;
import com.example.demo.kafka.producer.OrderProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class OrderService {

    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);

    private final OrderProducer orderProducer;

    public OrderService(OrderProducer orderProducer) {
        this.orderProducer = orderProducer;
    }

    /**
     * 创建一个 Order，然后发送到 Kafka
     *
     * @param userId    下单用户 ID
     * @param amount    订单金额
     */
    public void createAndSendOrder(String userId, Double amount) {
        // 生成一个随机的订单 ID
        String orderId = UUID.randomUUID().toString();
        Order order = new Order(orderId, userId, amount, LocalDateTime.now());

        logger.info("🔨 生成订单并发送到 Kafka：{}", order);
        orderProducer.sendOrder("orders", order);
    }
}

