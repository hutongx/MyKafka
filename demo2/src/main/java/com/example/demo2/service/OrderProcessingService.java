//package com.example.demo2.service;
//
//import com.example.demo2.domain.Order;
//import io.micrometer.core.instrument.Counter;
//import io.micrometer.core.instrument.MeterRegistry;
//import io.micrometer.core.instrument.Timer;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.time.LocalDateTime;
//import java.util.Map;
//import java.util.concurrent.CompletableFuture;
//
//@Service
//public class OrderProcessingService {
//
//    private static final Logger logger = LoggerFactory.getLogger(OrderProcessingService.class);
//
//    private final OrderRepository orderRepository;
//    private final PaymentService paymentService;
//    private final InventoryService inventoryService;
//    private final NotificationService notificationService;
//
//    // Metrics
//    private final Counter processedOrdersCounter;
//    private final Counter failedOrdersCounter;
//    private final Timer processingTimer;
//
//    @Autowired
//    public OrderProcessingService(
//            OrderRepository orderRepository,
//            PaymentService paymentService,
//            InventoryService inventoryService,
//            NotificationService notificationService,
//            MeterRegistry meterRegistry) {
//
//        this.orderRepository = orderRepository;
//        this.paymentService = paymentService;
//        this.inventoryService = inventoryService;
//        this.notificationService = notificationService;
//
//        // Initialize metrics
//        this.processedOrdersCounter = Counter.builder("orders.processed")
//                .description("Number of successfully processed orders")
//                .register(meterRegistry);
//
//        this.failedOrdersCounter = Counter.builder("orders.failed")
//                .description("Number of failed order processing attempts")
//                .register(meterRegistry);
//
//        this.processingTimer = Timer.builder("orders.processing.time")
//                .description("Order processing time")
//                .register(meterRegistry);
//    }
//
//    /**
//     * Main order processing method
//     */
//    @Transactional
//    public void processOrder(Order order, Map<String, String> headers) {
//        Timer.Sample sample = Timer.start();
//
//        try {
//            logger.info("Starting order processing: orderId={}", order.getOrderId());
//
//            // Validate order doesn't already exist
//            validateOrderNotExists(order.getOrderId());
//
//            // Set processing timestamp
//            order.setProcessedAt(LocalDateTime.now());
//            order.setStatus("PROCESSING");
//
//            // Save initial order state
//            Order savedOrder = orderRepository.save(order);
//            logger.debug("Order saved with ID: {}", savedOrder.getId());
//
//            // Process order steps
//            processOrderSteps(order, headers);
//
//            // Update final status
//            order.setStatus("COMPLETED");
//            order.setCompletedAt(LocalDateTime.now());
//            orderRepository.save(order);
//
//            // Send completion notification
//            notificationService.sendOrderCompletionNotification(order);
//
//            processedOrdersCounter.increment();
//
//            logger.info("Order processing completed successfully: orderId={}", order.getOrderId());
//
//        } catch (Exception ex) {
//            logger.error("Order processing failed: orderId={}, error={}", order.getOrderId(), ex.getMessage(), ex);
//
//            // Update order status to failed
//            updateOrderStatus(order.getOrderId(), "FAILED", ex.getMessage());
//
//            failedOrdersCounter.increment();
//            throw ex;
//
//        } finally {
//            sample.stop(processingTimer);
//        }
//    }
//
//    /**
//     * Process order events (status updates, cancellations, etc.)
//     */
//    public void processOrderEvent(Object eventData, String eventType, Map<String, String> headers) {
//        try {
//            logger.info("Processing order event: eventType={}", eventType);
//
//            switch (eventType) {
//                case "ORDER_CANCELLED":
//                    handleOrderCancellation(eventData, headers);
//                    break;
//                case "PAYMENT_UPDATED":
//                    handlePaymentUpdate(eventData, headers);
//                    break;
//                case "INVENTORY_UPDATED":
//                    handleInventoryUpdate(eventData, headers);
//                    break;
//                default:
//                    logger.warn("Unknown event type: {}", eventType);
//            }
//
//        } catch (Exception ex) {
//            logger.error("Failed to process order event: eventType={}, error={}", eventType, ex.getMessage(), ex);
//            throw ex;
//        }
//    }
//
//    /**
//     * Handle messages from Dead Letter Topic
//     */
//    public void handleDeadLetterMessage(Object messageData, Map<String, String> headers) {
//        try {
//            logger.warn("Processing dead letter message: headers={}", headers);
//
//            String originalTopic = headers.get("kafka_original-topic");
//            String failureReason = headers.get("kafka_exception-message");
//            String orderId = headers.get("orderId");
//
//            // Store for manual review
//            // You could save to a separate table for manual intervention
//
//            // Send alert
//            notificationService.sendDeadLetterAlert(orderId, originalTopic, failureReason);
//
//            logger.info("Dead letter message processed: orderId={}, reason={}", orderId, failureReason);
//
//        } catch (Exception ex) {
//            logger.error("Failed to handle dead letter message: {}", ex.getMessage(), ex);
//        }
//    }
//
//    private void processOrderSteps(Order order, Map<String, String> headers) {
//        // Step 1: Reserve inventory
//        reserveInventory(order);
//
//        // Step 2: Process payment
//        processPayment(order);
//
//        // Step 3: Additional business logic
//        performAdditionalProcessing(order, headers);
//    }
//
//    private void reserveInventory(Order order) {
//        logger.debug("Reserving inventory for order: {}", order.getOrderId());
//
//        try {
//            boolean reservationSuccess = inventoryService.reserveItems(order.getItems());
//
//            if (!reservationSuccess) {
//                throw new BusinessException("Failed to reserve inventory for order: " + order.getOrderId());
//            }
//
//            logger.info("Inventory reserved successfully for order: {}", order.getOrderId());
//
//        } catch (Exception ex) {
//            logger.error("Inventory reservation failed for order: {}", order.getOrderId(), ex);
//            throw new BusinessException("Inventory reservation failed", ex);
//        }
//    }
//
//    private void processPayment(Order order) {
//        logger.debug("Processing payment for order: {}", order.getOrderId());
//
//        try {
//            String paymentResult = paymentService.processPayment(
//                    order.getCustomerId(),
//                    order.getTotalAmount(),
//                    order.getOrderId()
//            );
//
//            if (!"SUCCESS".equals(paymentResult)) {
//                throw new BusinessException("Payment processing failed for order: " + order.getOrderId());
//            }
//
//            logger.info("Payment processed successfully for order: {}", order.getOrderId());
//
//        } catch (Exception ex) {
//            logger.error("Payment processing failed for order: {}", order.getOrderId(), ex);
//
//            // Compensate: release reserved inventory
//            try {
//                inventoryService.releaseReservation(order.getItems());
//            } catch (Exception compensationEx) {
//                logger.error("Failed to release inventory reservation during payment failure compensation", compensationEx);
//            }
//
//            throw new BusinessException("Payment processing failed", ex);
//        }
//    }
//
//    private void performAdditionalProcessing(Order order, Map<String, String> headers) {
//        // Additional business logic based on order type, customer segment, etc.
//        String orderType = headers.get("orderType");
//        String customerSegment = headers.get("customerSegment");
//
//        if ("PRIORITY".equals(orderType)) {
//            logger.info("Processing priority order: {}", order.getOrderId());
//            // Priority order logic
//        }
//
//        if ("VIP".equals(customerSegment)) {
//            logger.info("Processing VIP customer order: {}", order.getOrderId());
//            // VIP customer logic
//        }
//    }
//
//    private void validateOrderNotExists(String orderId) {
//        if (orderRepository.existsByOrderId(orderId)) {
//            throw new DuplicateOrderException("Order already exists: " + orderId);
//        }
//    }
//
//    private void updateOrderStatus(String orderId, String status, String errorMessage) {
//        try {
//            Order order = orderRepository.findByOrderId(orderId);
//            if (order != null) {
//                order.setStatus(status);
//                order.setErrorMessage(errorMessage);
//                orderRepository.save(order);
//            }
//        } catch (Exception ex) {
//            logger.error("Failed to update order status: orderId={}, status={}", orderId, status, ex);
//        }
//    }
//
//    private void handleOrderCancellation(Object eventData, Map<String, String> headers) {
//        String orderId = headers.get("orderId");
//        logger.info("Handling order cancellation: orderId={}", orderId);
//
//        // Implementation for order cancellation
//        Order order = orderRepository.findByOrderId(orderId);
//        if (order != null && !"COMPLETED".equals(order.getStatus())) {
//            order.setStatus("CANCELLED");
//            orderRepository.save(order);
//
//            // Release inventory if reserved
//            inventoryService.releaseReservation(order.getItems());
//
//            // Process refund if payment was processed
//            paymentService.processRefund(order.getOrderId(), order.getTotalAmount());
//        }
//    }
//
//    private void handlePaymentUpdate(Object eventData, Map<String, String> headers) {
//        String orderId = headers.get("orderId");
//        logger.info("Handling payment update: orderId={}", orderId);
//
//        // Implementation for payment status updates
//    }
//
//    private void handleInventoryUpdate(Object eventData, Map<String, String> headers) {
//        String orderId = headers.get("orderId");
//        logger.info("Handling inventory update: orderId={}", orderId);
//
//        // Implementation for inventory updates
//    }
//
//    // Exception classes
//    public static class BusinessException extends RuntimeException {
//        public BusinessException(String message) {
//            super(message);
//        }
//
//        public BusinessException(String message, Throwable cause) {
//            super(message, cause);
//        }
//    }
//
//    public static class DuplicateOrderException extends BusinessException {
//        public DuplicateOrderException(String message) {
//            super(message);
//        }
//    }
//}
