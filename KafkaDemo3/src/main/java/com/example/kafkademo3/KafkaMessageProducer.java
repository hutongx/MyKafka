package com.example.kafkademo3;

import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Kafka生产者 - 生产级别实现
 * 包含完整的配置、错误处理、监控和资源管理
 */
public class KafkaMessageProducer {

    private static final Logger logger = LoggerFactory.getLogger(KafkaMessageProducer.class);

    private final KafkaProducer<String, String> producer;
    private final String topicName;

    public KafkaMessageProducer(String bootstrapServers, String topicName) {
        this.topicName = topicName;
        this.producer = new KafkaProducer<>(createProducerConfig(bootstrapServers));

        // 添加关闭钩子，确保优雅关闭
        Runtime.getRuntime().addShutdownHook(new Thread(this::close));

        logger.info("Kafka Producer initialized for topic: {}", topicName);
    }

    /**
     * 生产级别的Producer配置
     */
    private Properties createProducerConfig(String bootstrapServers) {
        Properties props = new Properties();

        // 基础连接配置
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        // 性能优化配置
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384); // 16KB批次大小
        props.put(ProducerConfig.LINGER_MS_CONFIG, 10); // 等待10ms收集更多消息
        props.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432); // 32MB缓冲区
        props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy"); // 压缩算法

        // 可靠性配置
        props.put(ProducerConfig.ACKS_CONFIG, "all"); // 等待所有副本确认
        props.put(ProducerConfig.RETRIES_CONFIG, Integer.MAX_VALUE); // 无限重试
        props.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 1); // 保证消息顺序
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true); // 启用幂等性

        // 超时配置
        props.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 30000); // 30秒请求超时
        props.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 120000); // 2分钟交付超时

        // 监控配置
        props.put(ProducerConfig.METRIC_REPORTER_CLASSES_CONFIG, "");

        return props;
    }

    /**
     * 异步发送消息（推荐用于高吞吐量场景）
     */
    public void sendAsync(String key, String message) {
        ProducerRecord<String, String> record = new ProducerRecord<>(topicName, key, message);

        producer.send(record, new Callback() {
            @Override
            public void onCompletion(RecordMetadata metadata, Exception exception) {
                if (exception != null) {
                    logger.error("Failed to send message with key: {}, error: {}", key, exception.getMessage());
                    handleSendError(key, message, exception);
                } else {
                    logger.debug("Message sent successfully - Topic: {}, Partition: {}, Offset: {}, Timestamp: {}",
                            metadata.topic(), metadata.partition(), metadata.offset(), metadata.timestamp());
                }
            }
        });
    }

    /**
     * 同步发送消息（用于需要确认发送结果的场景）
     */
    public RecordMetadata sendSync(String key, String message) throws InterruptedException, ExecutionException, TimeoutException {
        ProducerRecord<String, String> record = new ProducerRecord<>(topicName, key, message);

        try {
            Future<RecordMetadata> future = producer.send(record);
            RecordMetadata metadata = future.get(30, TimeUnit.SECONDS); // 30秒超时

            logger.debug("Message sent synchronously - Topic: {}, Partition: {}, Offset: {}",
                    metadata.topic(), metadata.partition(), metadata.offset());

            return metadata;
        } catch (Exception e) {
            logger.error("Failed to send message synchronously with key: {}, error: {}", key, e.getMessage());
            throw e;
        }
    }

    /**
     * 带有重试机制的发送方法
     */
    public boolean sendWithRetry(String key, String message, int maxRetries) {
        int attempts = 0;

        while (attempts < maxRetries) {
            try {
                sendSync(key, message);
                return true;
            } catch (Exception e) {
                attempts++;
                logger.warn("Send attempt {} failed for key: {}, retrying... Error: {}",
                        attempts, key, e.getMessage());

                if (attempts >= maxRetries) {
                    logger.error("Failed to send message after {} attempts, key: {}", maxRetries, key);
                    return false;
                }

                try {
                    Thread.sleep(1000 * attempts); // 指数退避
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        }
        return false;
    }

    /**
     * 错误处理方法
     */
    private void handleSendError(String key, String message, Exception exception) {
        // 根据异常类型进行不同处理
        if (exception instanceof org.apache.kafka.common.errors.RetriableException) {
            logger.warn("Retriable exception occurred for key: {}, will retry automatically", key);
        } else {
            logger.error("Non-retriable exception occurred for key: {}, message may be lost", key);
            // 可以在这里实现死信队列或告警机制
        }
    }

    /**
     * 刷新缓冲区，确保所有消息都被发送
     */
    public void flush() {
        producer.flush();
        logger.debug("Producer buffer flushed");
    }

    /**
     * 获取生产者指标
     */
    public void logMetrics() {
        producer.metrics().forEach((name, metric) -> {
            if (name.name().contains("record-send-rate") ||
                    name.name().contains("record-error-rate") ||
                    name.name().contains("request-latency-avg")) {
                logger.info("Metric - {}: {}", name.name(), metric.metricValue());
            }
        });
    }

    /**
     * 优雅关闭生产者
     */
    public void close() {
        if (producer != null) {
            logger.info("Closing Kafka producer...");
            // producer.close(30, TimeUnit.SECONDS);
            producer.close();
            logger.info("Kafka producer closed successfully");
        }
    }
}
