package com.example.demo.service;

import com.example.demo.domain.OrderEvent;
import com.example.demo.producer.OrderProducer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.Producer;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.util.UUID;

@Service
@Slf4j
public class OrderEventScheduler {

    private final OrderProducer producer;
    private final ObjectMapper objectMapper;
    private final Random random = new Random();

    public OrderEventScheduler(OrderProducer producer, ObjectMapper objectMapper) {
        this.producer = producer;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedRate = 5000)  // 上一次执行开始后 5000ms 再执行下一次
    public void sendOrderPeriodically() {
        try {
            OrderEvent order = buildRandomOrder();
            producer.sendOrder(order);
            // 你也可以在这里做一些日志打印
        } catch (JsonProcessingException e) {
            // 序列化失败
            log.error("生成 OrderEvent JSON 失败", e);
        }
    }

    private OrderEvent buildRandomOrder() {
        OrderEvent o = new OrderEvent();
        o.setOrderId(UUID.randomUUID().toString());
        o.setUserId("U" + random.nextInt(1_0000));
        o.setSkuId("SKU" + random.nextInt(1_0000));
        o.setOrderAmount(String.format("%.2f", random.nextDouble() * 1000));
        o.setOrderTime(LocalDateTime.now());
        o.setStatus("TO_PAY");
        return o;
    }
}