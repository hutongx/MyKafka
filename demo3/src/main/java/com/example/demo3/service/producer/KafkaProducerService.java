package com.example.demo3.service.producer;

import com.example.demo3.model.entity.OrderEvent;
import com.example.demo3.model.entity.PaymentEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
//import org.springframework.util.concurrent.ListenableFuture;
//import org.springframework.util.concurrent.ListenableFutureCallback;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaProducerService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String ORDER_TOPIC = "order-events";
    private static final String PAYMENT_TOPIC = "payment-events";

    public CompletableFuture<SendResult<String, Object>> sendOrderEvent(OrderEvent orderEvent) {
        return sendEvent(ORDER_TOPIC, orderEvent.getOrderId(), orderEvent);
    }

    public CompletableFuture<SendResult<String, Object>> sendPaymentEvent(PaymentEvent paymentEvent) {
        return sendEvent(PAYMENT_TOPIC, paymentEvent.getPaymentId(), paymentEvent);
    }

    private CompletableFuture<SendResult<String, Object>> sendEvent(String topic, String key, Object event) {
        CompletableFuture<SendResult<String, Object>> future = new CompletableFuture<>();

        try {
//            ListenableFuture<SendResult<String, Object>> kafkaFuture = kafkaTemplate.send(topic, key, event);
//            kafkaFuture.addCallback(new ListenableFutureCallback<SendResult<String, Object>>() {
//                @Override
//                public void onSuccess(SendResult<String, Object> result) {
//                    log.info("Event sent successfully to topic [{}] with key [{}], offset: {}",
//                            topic, key, result.getRecordMetadata().offset());
//                    future.complete(result);
//                }
//
//                @Override
//                public void onFailure(Throwable ex) {
//                    log.error("Failed to send event to topic [{}] with key [{}]", topic, key, ex);
//                    future.completeExceptionally(ex);
//                }
//            });
            CompletableFuture<SendResult<String, Object>> kafkaFuture = kafkaTemplate.send(topic, key, event);
            kafkaFuture.whenComplete((result, ex) -> {
                if (ex != null) {
                    // 发送失败
                    log.error("Failed to send event to topic [{}] with key [{}]", topic, key, ex);
                    future.completeExceptionally(ex);
                } else {
                    log.info("Event sent successfully to topic [{}] with key [{}], offset: {}",
                            topic, key, result.getRecordMetadata().offset());
                    future.complete(result);
                }
            });
        } catch (Exception e) {
            log.error("Exception occurred while sending event to topic [{}]", topic, e);
            future.completeExceptionally(e);
        }
        return future;
    }

    public void sendEventAsync(String topic, String key, Object event) {
        sendEvent(topic, key, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Async send failed for topic [{}], key [{}]", topic, key, ex);
                        // Implement retry logic or dead letter queue
                    } else {
                        log.debug("Async send successful for topic [{}], key [{}]", topic, key);
                    }
                });
    }
}
