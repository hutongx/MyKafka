package com.example.demo2.service.impl;

import com.example.demo2.domain.Order;
import com.example.demo2.service.OrderProducer;
import com.example.demo2.service.OrderService;
import com.example.demo2.utils.OrderRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderProducer orderProducer;

    @Value("${app.order.min-amount:0.01}")
    private BigDecimal minOrderAmount;

    @Value("${app.order.max-amount:10000.00}")
    private BigDecimal maxOrderAmount;

    @Override
    public CompletableFuture<String> createOrder(OrderRequest orderRequest) {
        log.info("Creating order for customer: {}", orderRequest.getCustomerId());

        // Convert request to domain model
        Order order = Order.builder()
                .orderId(UUID.randomUUID().toString())
                .customerId(orderRequest.getCustomerId())
//                .productId(orderRequest.getProductId())
//                .quantity(orderRequest.getQuantity())
//                .price(orderRequest.getPrice())
//                .totalAmount(orderRequest.getPrice().multiply(BigDecimal.valueOf(orderRequest.getQuantity())))
//                .status("PENDING")
//                .orderDate(LocalDateTime.now())
                .shippingAddress(orderRequest.getShippingAddress())
                .build();

        // Validate order
        if (!validateOrder(order)) {
            return CompletableFuture.completedFuture("VALIDATION_FAILED");
        }

        // Send to Kafka
        return orderProducer.sendOrder(order)
                .thenApply(result -> {
                    log.info("Order {} sent successfully to partition {} with offset {}",
                            order.getOrderId(), result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                    return "ORDER_CREATED";
                })
                .exceptionally(throwable -> {
                    log.error("Failed to send order {}: {}", order.getOrderId(), throwable.getMessage());
                    return "ORDER_FAILED";
                });
    }

    @Override
    public void processOrder(Order order) {
        try {
            log.info("Processing order: {} for customer: {}", order.getOrderId(), order.getCustomerId());

            // Simulate business logic processing
            Thread.sleep(100); // Simulate processing time

            // Update order status based on business logic
            if (order.getTotalAmount().compareTo(BigDecimal.valueOf(1000)) > 0) {
//                order.setStatus("REQUIRES_APPROVAL");
                log.info("Order {} requires approval due to high value", order.getOrderId());
            } else {
//                order.setStatus("CONFIRMED");
                log.info("Order {} confirmed and ready for fulfillment", order.getOrderId());
            }

            // Here you would typically:
            // 1. Save to database
            // 2. Call external services
            // 3. Send notifications
            // 4. Update inventory

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Order processing interrupted for order: {}", order.getOrderId());
//            order.setStatus("PROCESSING_FAILED");
        } catch (Exception e) {
            log.error("Error processing order {}: {}", order.getOrderId(), e.getMessage(), e);
//            order.setStatus("PROCESSING_FAILED");
        }
    }

    @Override
    public boolean validateOrder(Order order) {
        if (order == null) {
            log.warn("Order is null");
            return false;
        }

        if (order.getCustomerId() == null || order.getCustomerId().trim().isEmpty()) {
            log.warn("Invalid customer ID for order: {}", order.getOrderId());
            return false;
        }

//        if (order.getProductId() == null || order.getProductId().trim().isEmpty()) {
//            log.warn("Invalid product ID for order: {}", order.getOrderId());
//            return false;
//        }
//
//        if (order.getQuantity() <= 0) {
//            log.warn("Invalid quantity {} for order: {}", order.getQuantity(), order.getOrderId());
//            return false;
//        }
//
//        if (order.getPrice() == null || order.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
//            log.warn("Invalid price {} for order: {}", order.getPrice(), order.getOrderId());
//            return false;
//        }

        if (order.getTotalAmount().compareTo(minOrderAmount) < 0 ||
                order.getTotalAmount().compareTo(maxOrderAmount) > 0) {
            log.warn("Order amount {} is outside allowed range [{}, {}] for order: {}",
                    order.getTotalAmount(), minOrderAmount, maxOrderAmount, order.getOrderId());
            return false;
        }

        return true;
    }
}