package com.example.demo.controller;

import com.example.demo.domain.OrderEvent;
import com.example.demo.domain.OrderMessage;
import com.example.demo.producer.OrderProducer;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderProducer producer;
    public OrderController(OrderProducer producer) {
        this.producer = producer;
    }

    @PostMapping("/test1")
    public ResponseEntity<String> createOrder(@RequestBody OrderMessage order) throws JsonProcessingException {
        producer.sendOrder(order);
        return ResponseEntity.ok("Order sent: " + order.getOrderId());
    }

    @PostMapping("/createOrderEvent")
    public ResponseEntity<String> createOrderEvent(@RequestBody OrderEvent orderEvent) throws JsonProcessingException {
        producer.sendOrder(orderEvent);
        return ResponseEntity.ok("Order sent: " + orderEvent.getOrderId());
    }
}
