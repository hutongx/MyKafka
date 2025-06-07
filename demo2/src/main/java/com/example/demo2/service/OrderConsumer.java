package com.example.demo2.service;

import com.example.demo2.domain.Order;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.listener.ConsumerSeekAware;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class OrderConsumer implements ConsumerSeekAware {

    private static final Logger logger = LoggerFactory.getLogger(OrderConsumer.class);

    private final OrderProcessingService orderProcessingService;

    // Metrics
    private final AtomicLong processedCount = new AtomicLong(0);
    private final AtomicLong errorCount = new AtomicLong(0);
    private final ConcurrentHashMap<String, AtomicLong> partitionOffsets = new ConcurrentHashMap<>();

    @Autowired
    public OrderConsumer(OrderProcessingService orderProcessingService) {
        this.orderProcessingService = orderProcessingService;
    }

    /**
     * Main order consumer with batch processing and manual offset management
     */
    @KafkaListener(
            topics = "${spring.kafka.topics.orders.name:orders}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "orderKafkaListenerContainerFactory"
    )
//    public void consumeOrders(
//            @Payload List<ConsumerRecord<String, Order>> records,
//            Acknowledgment acknowledgment,
//            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
//            @Header(KafkaHeaders.RECEIVED_PARTITION_ID) List<Integer> partitions,
//            @Header(KafkaHeaders.OFFSET) List<Long> offsets) {
    public void consumeOrders(
        @Payload List<ConsumerRecord<String, Order>> records,
        Acknowledgment acknowledgment,
        String topic,
        List<Integer> partitions,
        List<Long> offsets) {

        if (records == null || records.isEmpty()) {
            logger.debug("No records received");
            return;
        }

        logger.info("Received batch of {} records from topic: {}", records.size(), topic);

        try {
            // Process records by partition to maintain ordering
            processRecordsByPartition(records);

            // Acknowledge all messages after successful processing
            acknowledgment.acknowledge();

            logger.info("Successfully processed and acknowledged {} records", records.size());

        } catch (Exception ex) {
            logger.error("Error processing batch of {} records: {}", records.size(), ex.getMessage(), ex);
            errorCount.addAndGet(records.size());

            // Don't acknowledge on error - messages will be redelivered
            throw ex; // Let error handler deal with it
        }
    }

    /**
     * Process records grouped by partition to maintain ordering
     */
    private void processRecordsByPartition(List<ConsumerRecord<String, Order>> records) {
        // Group records by partition
        Map<TopicPartition, List<ConsumerRecord<String, Order>>> partitionGroups = groupByPartition(records);

        // Process each partition's records in order
        for (Map.Entry<TopicPartition, List<ConsumerRecord<String, Order>>> entry : partitionGroups.entrySet()) {
            TopicPartition partition = entry.getKey();
            List<ConsumerRecord<String, Order>> partitionRecords = entry.getValue();

            logger.debug("Processing {} records from partition: {}", partitionRecords.size(), partition);

            processPartitionRecords(partition, partitionRecords);
        }
    }

    /**
     * Process records from a single partition in order
     */
    private void processPartitionRecords(TopicPartition partition, List<ConsumerRecord<String, Order>> records) {
        for (ConsumerRecord<String, Order> record : records) {
            try {
                processSingleRecord(record);

                // Update partition offset tracking
                updatePartitionOffset(partition, record.offset());

            } catch (Exception ex) {
                logger.error("Failed to process record from partition {}, offset {}: {}",
                        partition, record.offset(), ex.getMessage(), ex);
                throw ex; // Propagate error to batch level
            }
        }
    }

    /**
     * Process a single order record
     */
    private void processSingleRecord(ConsumerRecord<String, Order> record) {
        Order order = record.value();

        // Set up MDC for better logging context
        try {
            setupMDC(record, order);

            logger.info("Processing order: orderId={}, customerId={}, partition={}, offset={}",
                    order.getOrderId(), order.getCustomerId(), record.partition(), record.offset());

            // Extract headers for additional context
            Map<String, String> headers = extractHeaders(record);

            // Validate order
            validateOrder(order);

            // Process the order
            long startTime = System.currentTimeMillis();
            orderProcessingService.processOrder(order, headers);
            long processingTime = System.currentTimeMillis() - startTime;

            logger.info("Order processed successfully: orderId={}, processingTime={}ms",
                    order.getOrderId(), processingTime);

            processedCount.incrementAndGet();

        } finally {
            // Clean up MDC
            MDC.clear();
        }
    }

    /**
     * Order event consumer for status updates
     */
    @KafkaListener(
            topics = "${spring.kafka.topics.order-events.name:order-events}",
            groupId = "${spring.kafka.consumer.group-id}-events"
    )
    public void consumeOrderEvents(
            ConsumerRecord<String, Object> record,
            Acknowledgment acknowledgment) {

        try {
            logger.info("Received order event: key={}, partition={}, offset={}",
                    record.key(), record.partition(), record.offset());

            // Extract headers
            Map<String, String> headers = extractHeaders(record);
            String eventType = headers.get("eventType");
            String orderId = headers.get("orderId");

            // Process event
            orderProcessingService.processOrderEvent(record.value(), eventType, headers);

            acknowledgment.acknowledge();

            logger.info("Order event processed successfully: orderId={}, eventType={}", orderId, eventType);

        } catch (Exception ex) {
            logger.error("Failed to process order event: {}", ex.getMessage(), ex);
            throw ex;
        }
    }

    /**
     * Dead Letter Topic consumer for failed messages
     */
    @KafkaListener(
            topics = "${spring.kafka.topics.dead-letter.name:order-dlt}",
            groupId = "${spring.kafka.consumer.group-id}-dlt"
    )
    public void consumeDeadLetterMessages(
            ConsumerRecord<String, Object> record,
            Acknowledgment acknowledgment) {

        try {
            logger.warn("Processing message from Dead Letter Topic: key={}, partition={}, offset={}",
                    record.key(), record.partition(), record.offset());

            // Extract headers to understand why message failed
            Map<String, String> headers = extractHeaders(record);

            // Handle dead letter message (alert, store for manual review, etc.)
            orderProcessingService.handleDeadLetterMessage(record.value(), headers);

            acknowledgment.acknowledge();

        } catch (Exception ex) {
            logger.error("Failed to process dead letter message: {}", ex.getMessage(), ex);
            // Still acknowledge to prevent infinite loop
            acknowledgment.acknowledge();
        }
    }

    private Map<TopicPartition, List<ConsumerRecord<String, Order>>> groupByPartition(
            List<ConsumerRecord<String, Order>> records) {

        Map<TopicPartition, List<ConsumerRecord<String, Order>>> groups = new HashMap<>();

        for (ConsumerRecord<String, Order> record : records) {
            TopicPartition partition = new TopicPartition(record.topic(), record.partition());
            groups.computeIfAbsent(partition, k -> new ArrayList<>()).add(record);
        }

        // Sort each partition's records by offset to ensure ordering
        groups.values().forEach(list ->
                list.sort(Comparator.comparing(ConsumerRecord::offset)));

        return groups;
    }

    private void updatePartitionOffset(TopicPartition partition, long offset) {
        String key = partition.topic() + "-" + partition.partition();
        partitionOffsets.computeIfAbsent(key, k -> new AtomicLong(0)).set(offset);
    }

    private void setupMDC(ConsumerRecord<String, Order> record, Order order) {
        MDC.put("orderId", order.getOrderId());
        MDC.put("customerId", order.getCustomerId());
        MDC.put("partition", String.valueOf(record.partition()));
        MDC.put("offset", String.valueOf(record.offset()));
        MDC.put("timestamp", String.valueOf(record.timestamp()));
    }

    private Map<String, String> extractHeaders(ConsumerRecord<?, ?> record) {
        Map<String, String> headers = new HashMap<>();

        if (record.headers() != null) {
            for (Header header : record.headers()) {
                String value = new String(header.value(), StandardCharsets.UTF_8);
                headers.put(header.key(), value);
            }
        }

        return headers;
    }

    private void validateOrder(Order order) {
        if (order == null) {
            throw new IllegalArgumentException("Order cannot be null");
        }

        if (order.getOrderId() == null || order.getOrderId().trim().isEmpty()) {
            throw new IllegalArgumentException("Order ID cannot be null or empty");
        }

        if (order.getCustomerId() == null || order.getCustomerId().trim().isEmpty()) {
            throw new IllegalArgumentException("Customer ID cannot be null or empty");
        }

        if (order.getItems() == null || order.getItems().isEmpty()) {
            throw new IllegalArgumentException("Order must have at least one item");
        }

        if (order.getTotalAmount() == null || order.getTotalAmount().signum() <= 0) {
            throw new IllegalArgumentException("Order total amount must be positive");
        }
    }

    // ConsumerSeekAware implementation for offset management
    @Override
    public void registerSeekCallback(ConsumerSeekCallback callback) {
        // Store callback for potential use in error recovery
    }

    @Override
    public void onPartitionsAssigned(Map<TopicPartition, Long> assignments, ConsumerSeekCallback callback) {
        logger.info("Partitions assigned: {}", assignments.keySet());

        // Custom logic for partition assignment (e.g., seek to specific offset)
        for (Map.Entry<TopicPartition, Long> entry : assignments.entrySet()) {
            TopicPartition partition = entry.getKey();
            Long offset = entry.getValue();

            // Example: seek to beginning for specific scenarios
            // callback.seekToBeginning(partition.topic(), partition.partition());

            logger.info("Assigned partition: {}, current offset: {}", partition, offset);
        }
    }

    @Override
    public void onIdleContainer(Map<TopicPartition, Long> assignments, ConsumerSeekCallback callback) {
        logger.debug("Container idle, assignments: {}", assignments.keySet());
    }

    // Metrics getters
    public long getProcessedCount() {
        return processedCount.get();
    }

    public long getErrorCount() {
        return errorCount.get();
    }

    public Map<String, Long> getPartitionOffsets() {
        Map<String, Long> result = new HashMap<>();
        partitionOffsets.forEach((k, v) -> result.put(k, v.get()));
        return result;
    }
}
