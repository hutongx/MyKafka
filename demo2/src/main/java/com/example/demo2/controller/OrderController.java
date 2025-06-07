package com.example.demo2.controller;

import com.example.demo2.domain.Order;
import com.example.demo2.service.OrderProcessingService;
import com.example.demo2.service.OrderProducer;
import com.example.demo2.utils.OrderRequest;
//import com.example.demo2.utils.OrderSendCallback;
import io.micrometer.core.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.support.SendResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/orders")
@Validated
public class OrderController {

    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);

    private final OrderProducer orderProducer;
    private final OrderProcessingService orderProcessingService;

    @Autowired
    public OrderController(OrderProducer orderProducer, OrderProcessingService orderProcessingService) {
        this.orderProducer = orderProducer;
        this.orderProcessingService = orderProcessingService;
    }

    /**
     * Create and submit a new order asynchronously
     */
    @PostMapping
    @Timed(value = "order.creation.time", description = "Time taken to create an order")
//    public ResponseEntity<Map<String, Object>> createOrder(@Valid @RequestBody OrderRequest orderRequest) {
    public ResponseEntity<Map<String, Object>> createOrder(@RequestBody OrderRequest orderRequest) {
        try {
            logger.info("Received order creation request: customerId={}", orderRequest.getCustomerId());

            // Generate order ID
            String orderId = generateOrderId();

            // Convert request to Order entity
            Order order = convertToOrder(orderRequest, orderId);

            // Prepare headers for additional context
            Map<String, String> headers = prepareHeaders(orderRequest);

            // Send order to Kafka asynchronously
//            CompletableFuture<Void> sendFuture = orderProducer.sendOrderAsync(order, headers);
            CompletableFuture<SendResult<String, Object>> sendFuture = orderProducer.sendOrder(order);

            // Prepare response
            Map<String, Object> response = new HashMap<>();
            response.put("orderId", orderId);
            response.put("status", "SUBMITTED");
            response.put("message", "Order submitted for processing");
            response.put("timestamp", LocalDateTime.now());

            logger.info("Order submitted successfully: orderId={}", orderId);

            return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);

        } catch (Exception ex) {
            logger.error("Failed to create order: {}", ex.getMessage(), ex);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Order creation failed");
            errorResponse.put("message", ex.getMessage());
            errorResponse.put("timestamp", LocalDateTime.now());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Create and submit order with synchronous confirmation
     */
    @PostMapping("/sync")
    @Timed(value = "order.creation.sync.time", description = "Time taken to create an order synchronously")
//    public ResponseEntity<Map<String, Object>> createOrderSync(@Valid @RequestBody OrderRequest orderRequest) {
    public ResponseEntity<Map<String, Object>> createOrderSync(@RequestBody OrderRequest orderRequest) {
        try {
            logger.info("Received synchronous order creation request: customerId={}", orderRequest.getCustomerId());

            // Generate order ID
            String orderId = generateOrderId();

            // Convert request to Order entity
            Order order = convertToOrder(orderRequest, orderId);

            // Prepare headers
            Map<String, String> headers = prepareHeaders(orderRequest);

            // Send order to Kafka synchronously and wait for confirmation
//            orderProducer.sendOrderSync(order, headers);
            orderProducer.sendOrder(order);

            // Prepare response
            Map<String, Object> response = new HashMap<>();
            response.put("orderId", orderId);
            response.put("status", "CONFIRMED");
            response.put("message", "Order confirmed and submitted for processing");
            response.put("timestamp", LocalDateTime.now());

            logger.info("Order confirmed successfully: orderId={}", orderId);

            return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);

        } catch (Exception ex) {
            logger.error("Failed to create order synchronously: {}", ex.getMessage(), ex);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Order creation failed");
            errorResponse.put("message", ex.getMessage());
            errorResponse.put("timestamp", LocalDateTime.now());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Cancel an existing order
     */
    @PostMapping("/{orderId}/cancel")
    @Timed(value = "order.cancellation.time", description = "Time taken to cancel an order")
    public ResponseEntity<Map<String, Object>> cancelOrder(
            @PathVariable String orderId,
            @RequestBody(required = false) Map<String, String> cancellationRequest) {

        try {
            logger.info("Received order cancellation request: orderId={}", orderId);

            // Prepare cancellation event
            Map<String, String> headers = new HashMap<>();
            headers.put("eventType", "ORDER_CANCELLED");
            headers.put("orderId", orderId);
            headers.put("timestamp", LocalDateTime.now().toString());

            if (cancellationRequest != null && cancellationRequest.containsKey("reason")) {
                headers.put("cancellationReason", cancellationRequest.get("reason"));
            }

            // Send cancellation event
//            orderProducer.sendOrderEvent(orderId, "ORDER_CANCELLED", headers);
            orderProducer.sendOrderEvent(Order.builder().orderId(orderId).build(), "ORDER_CANCELLED");

            // Prepare response
            Map<String, Object> response = new HashMap<>();
            response.put("orderId", orderId);
            response.put("status", "CANCELLATION_SUBMITTED");
            response.put("message", "Order cancellation submitted for processing");
            response.put("timestamp", LocalDateTime.now());

            logger.info("Order cancellation submitted: orderId={}", orderId);

            return ResponseEntity.ok(response);

        } catch (Exception ex) {
            logger.error("Failed to cancel order: orderId={}, error={}", orderId, ex.getMessage(), ex);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Order cancellation failed");
            errorResponse.put("message", ex.getMessage());
            errorResponse.put("timestamp", LocalDateTime.now());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Get health metrics
     */
    @GetMapping("/health/metrics")
    public ResponseEntity<Map<String, Object>> getHealthMetrics() {
        try {
            Map<String, Object> metrics = new HashMap<>();
            metrics.put("timestamp", LocalDateTime.now());

            // Add processing service metrics if available
            if (orderProcessingService != null) {
//                metrics.put("processedCount", orderProcessingService.getProcessedCount());
//                metrics.put("errorCount", orderProcessingService.getErrorCount());
            }

            return ResponseEntity.ok(metrics);

        } catch (Exception ex) {
            logger.error("Failed to get health metrics: {}", ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private String generateOrderId() {
        return "ORD-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private Order convertToOrder(OrderRequest request, String orderId) {
        Order order = new Order();
        order.setOrderId(orderId);
        order.setCustomerId(request.getCustomerId());
        order.setItems(request.getItems());
        order.setTotalAmount(request.getTotalAmount());
        order.setShippingAddress(request.getShippingAddress());
//        order.setBillingAddress(request.getBillingAddress());
//        order.setPaymentMethod(request.getPaymentMethod());
//        order.setSpecialInstructions(request.getSpecialInstructions());
        order.setCreatedAt(LocalDateTime.now());
//        order.setStatus("PENDING");

        return order;
    }

    private Map<String, String> prepareHeaders(OrderRequest request) {
        Map<String, String> headers = new HashMap<>();
        headers.put("customerId", request.getCustomerId());
        headers.put("orderType", determineOrderType(request));
        headers.put("customerSegment", determineCustomerSegment(request.getCustomerId()));
        headers.put("source", "REST_API");
        headers.put("timestamp", LocalDateTime.now().toString());

        // Add priority header if applicable
        if (request.getTotalAmount().compareTo(new java.math.BigDecimal("1000")) > 0) {
            headers.put("priority", "HIGH");
        }

        return headers;
    }

    private String determineOrderType(OrderRequest request) {
        // Business logic to determine order type
        if (request.getTotalAmount().compareTo(new java.math.BigDecimal("500")) > 0) {
            return "PRIORITY";
        }
        return "STANDARD";
    }

    private String determineCustomerSegment(String customerId) {
        // Business logic to determine customer segment
        // This could involve database lookup, external service call, etc.
        if (customerId.startsWith("VIP")) {
            return "VIP";
        } else if (customerId.startsWith("PREMIUM")) {
            return "PREMIUM";
        }
        return "STANDARD";
    }

    /**
     * Exception handler for validation errors
     */
    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(
            org.springframework.web.bind.MethodArgumentNotValidException ex) {

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", "Validation failed");
        errorResponse.put("timestamp", LocalDateTime.now());

        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                fieldErrors.put(error.getField(), error.getDefaultMessage())
        );
        errorResponse.put("fieldErrors", fieldErrors);

        logger.warn("Order validation failed: {}", fieldErrors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Generic exception handler
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericError(Exception ex) {
        logger.error("Unexpected error in OrderController: {}", ex.getMessage(), ex);

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", "Internal server error");
        errorResponse.put("message", "An unexpected error occurred");
        errorResponse.put("timestamp", LocalDateTime.now());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}
