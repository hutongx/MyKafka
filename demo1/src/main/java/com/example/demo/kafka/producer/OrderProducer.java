package com.example.demo.kafka.producer;

import com.example.demo.kafka.model.Order;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
public class OrderProducer {

    private static final Logger logger = LoggerFactory.getLogger(OrderProducer.class);

    private final KafkaTemplate<String, Order> kafkaTemplate;

    // 构造函数注入 KafkaTemplate
    public OrderProducer(KafkaTemplate<String, Order> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * 将 Order 对象作为消息发送到指定的 topic
     * @param topic  目标 topic
     * @param order  要发送的订单对象
     */
//    public void sendOrder(String topic, Order order) {
//        // 这里我们直接用 order.getOrderId() 作为 key，以保证相同 orderId 将路由到同一个 partition
//        String key = order.getOrderId();
//        // 构造 ProducerRecord 可以更灵活地控制 partition、timestamp、headers 等
//        ProducerRecord<String, Order> record = new ProducerRecord<>(topic, key, order);
//
//        kafkaTemplate.send(record).addCallback(new ListenableFutureCallback<SendResult<String, Order>>() {
//            @Override
//            public void onSuccess(SendResult<String, Order> result) {
//                RecordMetadata metadata = result.getRecordMetadata();
//                logger.info("✅ 发送成功：topic={}, partition={}, offset={}, timestamp={}，order={}",
//                        metadata.topic(), metadata.partition(), metadata.offset(), metadata.timestamp(), order);
//            }
//
//            @Override
//            public void onFailure(Throwable ex) {
//                // 失败时你可以执行重试、记录到 DB、告警等
//                logger.error("❌ 发送失败，order={}, exception={}", order, ex.getMessage());
//            }
//        });
//    }
    public void sendOrder(String topic, Order order) {
        // 这里我们直接用 order.getOrderId() 作为 key，以保证相同 orderId 将路由到同一个 partition
        String key = order.getOrderId();
        // 构造 ProducerRecord 可以更灵活地控制 partition、timestamp、headers 等
        ProducerRecord<String, Order> record = new ProducerRecord<>(topic, key, order);

        // 假设 record 和 order 都已准备好
        CompletableFuture<SendResult<String, Order>> future = kafkaTemplate.send(record);
            // 方式 1：whenComplete
            future.whenComplete((result, ex) -> {
                if (ex != null) {
                    // 发送失败
                    logger.error("❌ 发送失败，order={}, exception={}", order, ex.getMessage());
                } else {
                    RecordMetadata metadata = result.getRecordMetadata();
                    logger.info("✅ 发送成功：topic={}, partition={}, offset={}, timestamp={}，order={}",
                            metadata.topic(), metadata.partition(), metadata.offset(), metadata.timestamp(), order);
                }
            });
    }
}

