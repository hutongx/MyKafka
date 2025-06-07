package com.example.demo3.service.consumer;

import com.example.demo3.model.entity.OrderEvent;
import com.example.demo3.model.entity.PaymentEvent;
import com.example.demo3.service.DeadLetterService;
import com.example.demo3.service.OrderService;
import com.example.demo3.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaConsumerService {

    private final OrderService orderService;
    private final PaymentService paymentService;
    private final DeadLetterService deadLetterService;

    @KafkaListener(topics = "order-events", groupId = "order-processor-group")
    @Retryable(value = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public void consumeOrderEvent(
            @Payload OrderEvent orderEvent,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {

        try {
            log.info("Received order event: orderId={}, topic={}, partition={}, offset={}",
                    orderEvent.getOrderId(), topic, partition, offset);

            // Process business logic
            orderService.processOrder(orderEvent);

            // Manual acknowledgment
            acknowledgment.acknowledge();

            log.info("Successfully processed order event: orderId={}", orderEvent.getOrderId());

        } catch (Exception e) {
            log.error("Error processing order event: orderId={}", orderEvent.getOrderId(), e);

            // Send to dead letter queue after max retries
            deadLetterService.sendToDeadLetter("order-events-dlq", orderEvent, e.getMessage());

            // Acknowledge to prevent infinite reprocessing
            acknowledgment.acknowledge();
        }
    }

    @KafkaListener(topics = "payment-events", groupId = "payment-processor-group")
    @Retryable(value = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public void consumePaymentEvent(
            @Payload PaymentEvent paymentEvent,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {

        try {
            log.info("Received payment event: paymentId={}, topic={}, partition={}, offset={}",
                    paymentEvent.getPaymentId(), topic, partition, offset);

            // Process business logic
            paymentService.processPayment(paymentEvent);

            // Manual acknowledgment
            acknowledgment.acknowledge();

            log.info("Successfully processed payment event: paymentId={}", paymentEvent.getPaymentId());

        } catch (Exception e) {
            log.error("Error processing payment event: paymentId={}", paymentEvent.getPaymentId(), e);

            // Send to dead letter queue
            deadLetterService.sendToDeadLetter("payment-events-dlq", paymentEvent, e.getMessage());

            // Acknowledge to prevent infinite reprocessing
            acknowledgment.acknowledge();
        }
    }
}
