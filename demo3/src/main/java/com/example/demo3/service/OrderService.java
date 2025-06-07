package com.example.demo3.service;

import com.example.demo3.dao.OrderRepository;
import com.example.demo3.enums.PaymentMethod;
import com.example.demo3.model.dto.CreateOrderRequest;
import com.example.demo3.model.dto.OrderResponse;
import com.example.demo3.model.entity.OrderEntity;
import com.example.demo3.model.entity.OrderEvent;
import com.example.demo3.model.entity.PaymentEvent;
import com.example.demo3.enums.OrderStatus;
import com.example.demo3.enums.PaymentStatus;
import com.example.demo3.service.producer.KafkaProducerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final KafkaProducerService kafkaProducerService;

    public OrderResponse createOrder(CreateOrderRequest request) {
        String orderId = "ORDER-" + UUID.randomUUID().toString();
        String traceId = request.getTraceId() != null ? request.getTraceId() : UUID.randomUUID().toString();

        OrderEntity orderEntity = OrderEntity.builder()
                .orderId(orderId)
                .userId(request.getUserId())
                .productId(request.getProductId())
                .amount(request.getAmount())
                .status(OrderStatus.CREATED)
                .traceId(traceId)
                .build();

        OrderEntity savedOrder = orderRepository.save(orderEntity);

        // Send Kafka event
        OrderEvent orderEvent = OrderEvent.builder()
                .orderId(savedOrder.getOrderId())
                .userId(savedOrder.getUserId())
                .productId(savedOrder.getProductId())
                .amount(savedOrder.getAmount())
                .status(savedOrder.getStatus())
                .createdAt(savedOrder.getCreatedAt())
                .traceId(savedOrder.getTraceId())
                .build();

        kafkaProducerService.sendOrderEvent(orderEvent);

        return mapToOrderResponse(savedOrder);
    }

    public void processOrder(OrderEvent orderEvent) {
        log.info("Processing order: {}", orderEvent.getOrderId());

        Optional<OrderEntity> orderOpt = orderRepository.findById(orderEvent.getOrderId());
        if (!orderOpt.isPresent()) {
            log.warn("Order not found: {}", orderEvent.getOrderId());
            return;
        }

        OrderEntity order = orderOpt.get();

        // Update order status based on event
        if (order.getStatus() != orderEvent.getStatus()) {
            order.setStatus(orderEvent.getStatus());
            orderRepository.save(order);

            log.info("Order status updated: orderId={}, status={}",
                    order.getOrderId(), order.getStatus());
        }

        // Trigger payment processing for paid orders
        if (orderEvent.getStatus() == OrderStatus.PAID) {
            PaymentEvent paymentEvent = PaymentEvent.builder()
                    .paymentId("PAY-" + UUID.randomUUID().toString())
                    .orderId(orderEvent.getOrderId())
                    .amount(orderEvent.getAmount())
                    .method(PaymentMethod.CREDIT_CARD)
                    .status(PaymentStatus.PENDING)
                    .processedAt(LocalDateTime.now())
                    .traceId(orderEvent.getTraceId())
                    .build();

            kafkaProducerService.sendPaymentEvent(paymentEvent);
        }
    }

    public List<OrderResponse> getOrdersByUserId(String userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::mapToOrderResponse)
                .collect(Collectors.toList());
    }

    public Optional<OrderResponse> getOrder(String orderId) {
        return orderRepository.findById(orderId)
                .map(this::mapToOrderResponse);
    }

    private OrderResponse mapToOrderResponse(OrderEntity entity) {
        return OrderResponse.builder()
                .orderId(entity.getOrderId())
                .userId(entity.getUserId())
                .productId(entity.getProductId())
                .amount(entity.getAmount())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
