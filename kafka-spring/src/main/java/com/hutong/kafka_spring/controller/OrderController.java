package com.hutong.kafka_spring.controller;

import com.hutong.kafka_spring.domain.OrderMessage;
import com.hutong.kafka_spring.producer.OrderProducer;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderProducer producer;
    public OrderController(OrderProducer producer) {
        this.producer = producer;
    }

    @PostMapping
    public ResponseEntity<String> createOrder(@RequestBody OrderMessage order) {
        producer.sendOrder(order);
        return ResponseEntity.ok("Order sent: " + order.getOrderId());
    }
}

