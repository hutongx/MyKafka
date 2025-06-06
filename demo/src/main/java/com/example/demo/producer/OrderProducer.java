package com.example.demo.producer;

import com.example.demo.domain.OrderEvent;
import com.example.demo.domain.OrderMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutionException;

@Service
public class OrderProducer {
    private static final Logger log = LoggerFactory.getLogger(OrderProducer.class);
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String topic;

    public OrderProducer(KafkaTemplate<String, String> kafkaTemplate,
                         ObjectMapper objectMapper,
                         @Value("${app.kafka.topics.orders}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.topic = topic;
    }

    public void sendOrder(OrderMessage order) throws JsonProcessingException {
        String payload = objectMapper.writeValueAsString(order);
        ProducerRecord<String, String> record =
                new ProducerRecord<>(topic, order.getOrderId(), payload);

        try {
            // 同步阻塞，抛出异常即为发送失败
            SendResult<String,String> result =
                    kafkaTemplate.send(record).get();
            RecordMetadata md = result.getRecordMetadata();
            log.info("[Producer] sent topic={} partition={} offset={}",
                    md.topic(), md.partition(), md.offset());
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            log.error("[Producer] interrupted sending record {}", record, ie);
        } catch (ExecutionException ee) {
            log.error("[Producer] failed sending record {}", record, ee.getCause());
        }
//        try {
//            String key = order.getOrderId();
//            String payload = objectMapper.writeValueAsString(order);
//            kafkaTemplate.send(topic, key, payload)
//                    .addCallback(new ListenableFutureCallback<>() {
//                        @Override
//                        public void onSuccess(var result) {
//                            log.info("[Producer] topic={} partition={} offset={}",
//                                    result.getRecordMetadata().topic(),
//                                    result.getRecordMetadata().partition(),
//                                    result.getRecordMetadata().offset());
//                        }
//                        @Override
//                        public void onFailure(Throwable ex) {
//                            log.error("[Producer] send failed, orderId={}", key, ex);
//                        }
//                    });
//        } catch (Exception e) {
//            log.error("[Producer] serialize failed: {}", order, e);
//        }
    }

    public void sendOrder(OrderEvent orderEvent) throws JsonProcessingException {
        String payload = objectMapper.writeValueAsString(orderEvent);
        ProducerRecord<String, String> record =
                new ProducerRecord<>(topic, orderEvent.getOrderId(), payload);

        try {
            // 同步阻塞，抛出异常即为发送失败
            SendResult<String,String> result =
                    kafkaTemplate.send(record).get();
            RecordMetadata md = result.getRecordMetadata();
            log.info("[Producer] sent topic={} partition={} offset={}",
                    md.topic(), md.partition(), md.offset());
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            log.error("[Producer] interrupted sending record {}", record, ie);
        } catch (ExecutionException ee) {
            log.error("[Producer] failed sending record {}", record, ee.getCause());
        }
    }
}


