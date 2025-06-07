package com.example.demo.kafka.controller;

import com.example.demo.kafka.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 提供一个简单的 REST 接口，用于创建订单并发送到 Kafka
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * 创建订单接口 (POST /api/orders)
     * 请求参数示例 (JSON):
     * {
     *   "userId": "user-123",
     *   "amount": 99.9
     * }
     *
     * @param request  包含 userId 和 amount 的请求体
     * @return 200_OK 或者 400_BAD_REQUEST
     */
    @PostMapping
    public ResponseEntity<String> createOrder(@RequestBody CreateOrderRequest request) {
        if (request.getUserId() == null || request.getAmount() == null) {
            return ResponseEntity.badRequest().body("userId 和 amount 都不能为空");
        }

        logger.info("收到创建订单请求：userId={}, amount={}", request.getUserId(), request.getAmount());
        orderService.createAndSendOrder(request.getUserId(), request.getAmount());
        return ResponseEntity.ok("Order is being processed and sent to Kafka");
    }

    // 内部使用的请求参数类
    public static class CreateOrderRequest {
        private String userId;
        private Double amount;

        public String getUserId() {
            return userId;
        }
        public void setUserId(String userId) {
            this.userId = userId;
        }
        public Double getAmount() {
            return amount;
        }
        public void setAmount(Double amount) {
            this.amount = amount;
        }
    }
}

