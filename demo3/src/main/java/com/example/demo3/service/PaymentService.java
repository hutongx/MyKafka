package com.example.demo3.service;

import com.example.demo3.dao.OrderRepository;
import com.example.demo3.dao.PaymentRepository;
import com.example.demo3.enums.OrderStatus;
import com.example.demo3.enums.PaymentStatus;
import com.example.demo3.model.dto.PaymentResponse;
import com.example.demo3.model.dto.ProcessPaymentRequest;
import com.example.demo3.model.entity.OrderEntity;
import com.example.demo3.model.entity.OrderEvent;
import com.example.demo3.model.entity.PaymentEntity;
import com.example.demo3.model.entity.PaymentEvent;
import com.example.demo3.service.producer.KafkaProducerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final KafkaProducerService kafkaProducerService;

    public PaymentResponse processPaymentRequest(ProcessPaymentRequest request) {
        String paymentId = "PAY-" + UUID.randomUUID().toString();
        String traceId = request.getTraceId() != null ? request.getTraceId() : UUID.randomUUID().toString();

        // Verify order exists
        Optional<OrderEntity> orderOpt = orderRepository.findById(request.getOrderId());
        if (!orderOpt.isPresent()) {
            throw new IllegalArgumentException("Order not found: " + request.getOrderId());
        }

        PaymentEntity paymentEntity = PaymentEntity.builder()
                .paymentId(paymentId)
                .orderId(request.getOrderId())
                .amount(request.getAmount())
                .method(request.getMethod())
                .status(PaymentStatus.PENDING)
                .traceId(traceId)
                .transactionId("TXN-" + UUID.randomUUID().toString())
                .build();

        PaymentEntity savedPayment = paymentRepository.save(paymentEntity);

        // Send Kafka event
        PaymentEvent paymentEvent = PaymentEvent.builder()
                .paymentId(savedPayment.getPaymentId())
                .orderId(savedPayment.getOrderId())
                .amount(savedPayment.getAmount())
                .method(savedPayment.getMethod())
                .status(savedPayment.getStatus())
                .processedAt(savedPayment.getProcessedAt())
                .traceId(savedPayment.getTraceId())
                .build();

        kafkaProducerService.sendPaymentEvent(paymentEvent);

        return mapToPaymentResponse(savedPayment);
    }

    public void processPayment(PaymentEvent paymentEvent) {
        log.info("Processing payment: {}", paymentEvent.getPaymentId());

        Optional<PaymentEntity> paymentOpt = paymentRepository.findById(paymentEvent.getPaymentId());
        if (!paymentOpt.isPresent()) {
            log.warn("Payment not found: {}", paymentEvent.getPaymentId());
            return;
        }

        PaymentEntity payment = paymentOpt.get();

        try {
            // Simulate payment processing
            boolean paymentSuccess = simulatePaymentProcessing(payment);

            PaymentStatus newStatus = paymentSuccess ? PaymentStatus.SUCCESS : PaymentStatus.FAILED;
            payment.setStatus(newStatus);
            paymentRepository.save(payment);

            // Update order status if payment successful
            if (paymentSuccess) {
                orderRepository.updateOrderStatus(payment.getOrderId(), OrderStatus.PAID);

                // Send order status update event
                OrderEvent orderEvent = OrderEvent.builder()
                        .orderId(payment.getOrderId())
                        .status(OrderStatus.PAID)
                        .traceId(payment.getTraceId())
                        .build();

                kafkaProducerService.sendOrderEvent(orderEvent);
            }

            log.info("Payment processed: paymentId={}, status={}",
                    payment.getPaymentId(), payment.getStatus());

        } catch (Exception e) {
            log.error("Payment processing failed: paymentId={}", payment.getPaymentId(), e);
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
        }
    }

    private boolean simulatePaymentProcessing(PaymentEntity payment) {
        // Simulate external payment gateway call
        // In real implementation, integrate with actual payment provider
        return Math.random() > 0.1; // 90% success rate
    }

    public List<PaymentResponse> getPaymentsByOrderId(String orderId) {
        return paymentRepository.findByOrderIdOrderByProcessedAtDesc(orderId)
                .stream()
                .map(this::mapToPaymentResponse)
                .collect(Collectors.toList());
    }

    private PaymentResponse mapToPaymentResponse(PaymentEntity entity) {
        return PaymentResponse.builder()
                .paymentId(entity.getPaymentId())
                .orderId(entity.getOrderId())
                .amount(entity.getAmount())
                .method(entity.getMethod())
                .status(entity.getStatus())
                .processedAt(entity.getProcessedAt())
                .transactionId(entity.getTransactionId())
                .build();
    }
}
