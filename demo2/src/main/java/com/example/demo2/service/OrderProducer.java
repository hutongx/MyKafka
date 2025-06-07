package com.example.demo2.service;

import com.example.demo2.domain.Order;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
//import org.springframework.util.concurrent.ListenableFuture;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
public class OrderProducer {

    private static final Logger logger = LoggerFactory.getLogger(OrderProducer.class);

    @Value("${spring.kafka.topics.orders.name:orders}")
    private String ordersTopic;

    @Value("${spring.kafka.topics.order-events.name:order-events}")
    private String orderEventsTopic;

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    public OrderProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Send order message with callback handling
     * Uses CompletableFuture for modern async handling
     */
    public CompletableFuture<SendResult<String, Object>> sendOrder(Order order) {
        return sendOrderWithCallback(order, null);
    }

    /**
     * Send order message with custom callback
     */
    public CompletableFuture<SendResult<String, Object>> sendOrderWithCallback(
            Order order,
            OrderSendCallback callback) {

        if (order == null) {
            throw new IllegalArgumentException("Order cannot be null");
        }

        // Validate order
        validateOrder(order);

        // Create producer record with headers
        ProducerRecord<String, Object> record = createOrderRecord(order);

        logger.info("Sending order message: orderId={}, customerId={}, topic={}, key={}",
                order.getOrderId(), order.getCustomerId(), ordersTopic, record.key());

        // Send with CompletableFuture
//        ListenableFuture<SendResult<String, Object>> future = kafkaTemplate.send(record);
        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(record);

        return CompletableFuture.supplyAsync(() -> {
            try {
                SendResult<String, Object> result = future.get(30, TimeUnit.SECONDS);

                // Log success
                logger.info("Order sent successfully: orderId={}, topic={}, partition={}, offset={}",
                        order.getOrderId(),
                        result.getRecordMetadata().topic(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());

                // Execute custom callback if provided
                if (callback != null) {
                    callback.onSuccess(order, result);
                }

                return result;

            } catch (Exception ex) {
                logger.error("Failed to send order: orderId={}, error={}",
                        order.getOrderId(), ex.getMessage(), ex);

                // Execute custom callback if provided
                if (callback != null) {
                    callback.onFailure(order, ex);
                }

                throw new RuntimeException("Failed to send order message", ex);
            }
        });
    }

    /**
     * Send order event (for order status changes)
     */
    public CompletableFuture<SendResult<String, Object>> sendOrderEvent(Order order, String eventType) {
        if (order == null || eventType == null) {
            throw new IllegalArgumentException("Order and eventType cannot be null");
        }

        // Create event record
        ProducerRecord<String, Object> record = createOrderEventRecord(order, eventType);

        logger.info("Sending order event: orderId={}, eventType={}, status={}",
                order.getOrderId(), eventType, order.getStatus());

//        ListenableFuture<SendResult<String, Object>> future = kafkaTemplate.send(record);
        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(record);

        return CompletableFuture.supplyAsync(() -> {
            try {
                SendResult<String, Object> result = future.get(30, TimeUnit.SECONDS);

                logger.info("Order event sent successfully: orderId={}, eventType={}, partition={}, offset={}",
                        order.getOrderId(), eventType,
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());

                return result;

            } catch (Exception ex) {
                logger.error("Failed to send order event: orderId={}, eventType={}, error={}",
                        order.getOrderId(), eventType, ex.getMessage(), ex);
                throw new RuntimeException("Failed to send order event", ex);
            }
        });
    }

    /**
     * Send multiple orders in batch
     */
    public List<CompletableFuture<SendResult<String, Object>>> sendOrderBatch(List<Order> orders) {
        if (orders == null || orders.isEmpty()) {
            throw new IllegalArgumentException("Orders list cannot be null or empty");
        }

        logger.info("Sending batch of {} orders", orders.size());

        List<CompletableFuture<SendResult<String, Object>>> futures = new ArrayList<>();

        for (Order order : orders) {
            try {
                CompletableFuture<SendResult<String, Object>> future = sendOrder(order);
                futures.add(future);
            } catch (Exception ex) {
                logger.error("Failed to send order in batch: orderId={}, error={}",
                        order.getOrderId(), ex.getMessage());

                // Create a failed future
                CompletableFuture<SendResult<String, Object>> failedFuture = new CompletableFuture<>();
                failedFuture.completeExceptionally(ex);
                futures.add(failedFuture);
            }
        }

        return futures;
    }

    /**
     * Wait for all batch operations to complete
     */
    public CompletableFuture<Void> waitForBatchCompletion(List<CompletableFuture<SendResult<String, Object>>> futures) {
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        logger.error("Batch operation completed with errors: {}", ex.getMessage());
                    } else {
                        logger.info("All batch operations completed successfully");
                    }
                });
    }

    private ProducerRecord<String, Object> createOrderRecord(Order order) {
        // Use customerId as partition key for ordering guarantee
        String partitionKey = order.getPartitionKey();

        // Create headers
        List<Header> headers = createOrderHeaders(order, "ORDER_CREATED");

        return new ProducerRecord<>(
                ordersTopic,
                null, // Let Kafka determine partition based on key
                System.currentTimeMillis(),
                partitionKey,
                order,
                headers
        );
    }

    private ProducerRecord<String, Object> createOrderEventRecord(Order order, String eventType) {
        String partitionKey = order.getPartitionKey();

        // Create event payload
        OrderEvent event = new OrderEvent(order.getOrderId(), order.getCustomerId(),
                order.getStatus().toString(), eventType, LocalDateTime.now());

        // Create headers
        List<Header> headers = createOrderHeaders(order, eventType);

        return new ProducerRecord<>(
                orderEventsTopic,
                null,
                System.currentTimeMillis(),
                partitionKey,
                event,
                headers
        );
    }

    private List<Header> createOrderHeaders(Order order, String eventType) {
        List<Header> headers = new ArrayList<>();

        // Add correlation headers
        headers.add(new RecordHeader("correlationId", UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8)));
        headers.add(new RecordHeader("orderId", order.getOrderId().getBytes(StandardCharsets.UTF_8)));
        headers.add(new RecordHeader("customerId", order.getCustomerId().getBytes(StandardCharsets.UTF_8)));
        headers.add(new RecordHeader("eventType", eventType.getBytes(StandardCharsets.UTF_8)));
        headers.add(new RecordHeader("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME).getBytes(StandardCharsets.UTF_8)));
        headers.add(new RecordHeader("version", order.getVersion().toString().getBytes(StandardCharsets.UTF_8)));
        headers.add(new RecordHeader("source", "order-service".getBytes(StandardCharsets.UTF_8)));

        return headers;
    }

    private void validateOrder(Order order) {
        if (order.getOrderId() == null || order.getOrderId().trim().isEmpty()) {
            throw new IllegalArgumentException("Order ID cannot be null or empty");
        }

        if (order.getCustomerId() == null || order.getCustomerId().trim().isEmpty()) {
            throw new IllegalArgumentException("Customer ID cannot be null or empty");
        }

        if (order.getTotalAmount() == null || order.getTotalAmount().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Total amount must be greater than zero");
        }

        if (order.getItems() == null || order.getItems().isEmpty()) {
            throw new IllegalArgumentException("Order must have at least one item");
        }
    }

    // Callback interface for custom handling
    public interface OrderSendCallback {
        void onSuccess(Order order, SendResult<String, Object> result);
        void onFailure(Order order, Exception ex);
    }

    // Order event class for event messages
    public static class OrderEvent {
        private String orderId;
        private String customerId;
        private String status;
        private String eventType;
        private LocalDateTime timestamp;

        public OrderEvent() {}

        public OrderEvent(String orderId, String customerId, String status, String eventType, LocalDateTime timestamp) {
            this.orderId = orderId;
            this.customerId = customerId;
            this.status = status;
            this.eventType = eventType;
            this.timestamp = timestamp;
        }

        // Getters and setters
        public String getOrderId() { return orderId; }
        public void setOrderId(String orderId) { this.orderId = orderId; }

        public String getCustomerId() { return customerId; }
        public void setCustomerId(String customerId) { this.customerId = customerId; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getEventType() { return eventType; }
        public void setEventType(String eventType) { this.eventType = eventType; }

        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    }
}
