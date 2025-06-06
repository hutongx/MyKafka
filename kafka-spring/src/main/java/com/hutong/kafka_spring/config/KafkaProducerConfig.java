package com.hutong.kafka_spring.config;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.kafka.core.*;
import org.springframework.kafka.support.ProducerListener;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaProducerConfig {
    private static final Logger log = LoggerFactory.getLogger(KafkaProducerConfig.class);

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public ProducerFactory<String, String> producerFactory() {
        Map<String, Object> props = new HashMap<>();
        // 连接
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        // 序列化
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        // 可靠性
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, 5);
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        props.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 1);
        // 批量 & 延迟 & 压缩
        props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");
        props.put(ProducerConfig.LINGER_MS_CONFIG, 20);
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, 64 * 1024);
        // 超时
        props.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 120_000);
        props.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 30_000);

        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, String> kafkaTemplate(ProducerFactory<String, String> pf,
                                                       ProducerListener<String, String> listener) {
        KafkaTemplate<String, String> template = new KafkaTemplate<>(pf);
        template.setProducerListener(listener);
        return template;
    }

    @Bean
    public ProducerListener<String, String> producerListener() {
        return new ProducerListener<String, String>() {
            @Override
            public void onSuccess(org.apache.kafka.clients.producer.ProducerRecord<String, String> record,
                                  org.apache.kafka.clients.producer.RecordMetadata metadata) {
                log.info("[ProducerListener] topic={} partition={} offset={}",
                        metadata.topic(), metadata.partition(), metadata.offset());
            }
            @Override
            public void onError(ProducerRecord<String, String> record,RecordMetadata recordMetadata,
                                Exception ex) {
                System.out.println(recordMetadata.topic());
                log.error("[ProducerListener] failed sending record {}", record, ex);
            }
        };
    }
}


