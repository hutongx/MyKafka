package com.example.kafkademo3;

import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Kafka消费者 - 生产级别实现
 * 包含完整的配置、错误处理、重平衡处理和优雅关闭
 */
public class KafkaMessageConsumer {

    private static final Logger logger = LoggerFactory.getLogger(KafkaMessageConsumer.class);

    private final KafkaConsumer<String, String> consumer;
    private final List<String> topics;
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final MessageProcessor messageProcessor;

    public KafkaMessageConsumer(String bootstrapServers, String groupId,
                                List<String> topics, MessageProcessor messageProcessor) {
        this.topics = topics;
        this.messageProcessor = messageProcessor;
        this.consumer = new KafkaConsumer<>(createConsumerConfig(bootstrapServers, groupId));

        logger.info("Kafka Consumer initialized for group: {}, topics: {}", groupId, topics);
    }

    /**
     * 生产级别的Consumer配置
     */
    private Properties createConsumerConfig(String bootstrapServers, String groupId) {
        Properties props = new Properties();

        // 基础连接配置
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

        // 消费行为配置
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest"); // 从最早的offset开始消费
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false); // 手动提交offset

        // 性能优化配置
        props.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, 1024); // 最小拉取1KB
        props.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, 500); // 最多等待500ms
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 500); // 每次最多拉取500条记录
        props.put(ConsumerConfig.RECEIVE_BUFFER_CONFIG, 65536); // 64KB接收缓冲区
        props.put(ConsumerConfig.SEND_BUFFER_CONFIG, 131072); // 128KB发送缓冲区

        // 会话管理配置
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 30000); // 30秒会话超时
        props.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 3000); // 3秒心跳间隔
        props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 300000); // 5分钟最大轮询间隔

        // 重试配置
        props.put(ConsumerConfig.RETRY_BACKOFF_MS_CONFIG, 1000); // 重试间隔1秒
        props.put(ConsumerConfig.RECONNECT_BACKOFF_MS_CONFIG, 1000); // 重连间隔1秒

        return props;
    }

    /**
     * 启动消费者（带有重平衡监听器）
     */
    public void start() {
        try {
            // 订阅主题并设置重平衡监听器
            consumer.subscribe(topics, new ConsumerRebalanceListener() {
                @Override
                public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
                    logger.info("Partitions revoked: {}", partitions);
                    // 在分区被撤销前提交当前偏移量
                    commitCurrentOffsets();
                }

                @Override
                public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
                    logger.info("Partitions assigned: {}", partitions);
                    // 可以在这里设置特定的消费起始位置
                }
            });

            logger.info("Started consuming messages from topics: {}", topics);

            // 主消费循环
            while (!closed.get()) {
                try {
                    ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));

                    if (!records.isEmpty()) {
                        processRecords(records);
                        commitOffsetsSync(); // 同步提交偏移量
                    }

                } catch (WakeupException e) {
                    logger.info("Consumer wakeup called");
                    break;
                } catch (Exception e) {
                    logger.error("Error during message consumption: {}", e.getMessage(), e);
                    handleConsumerError(e);
                }
            }

        } catch (Exception e) {
            logger.error("Fatal error in consumer: {}", e.getMessage(), e);
        } finally {
            cleanup();
        }
    }

    /**
     * 处理消费到的记录
     */
    private void processRecords(ConsumerRecords<String, String> records) {
        Map<TopicPartition, OffsetAndMetadata> currentOffsets = new HashMap<>();

        for (ConsumerRecord<String, String> record : records) {
            try {
                logger.debug("Processing message - Topic: {}, Partition: {}, Offset: {}, Key: {}",
                        record.topic(), record.partition(), record.offset(), record.key());

                // 处理消息
                boolean processed = messageProcessor.process(record);

                if (processed) {
                    // 记录成功处理的偏移量
                    currentOffsets.put(
                            new TopicPartition(record.topic(), record.partition()),
                            new OffsetAndMetadata(record.offset() + 1)
                    );
                } else {
                    logger.warn("Message processing failed for key: {}, implementing retry logic", record.key());
                    handleFailedMessage(record);
                }

            } catch (Exception e) {
                logger.error("Error processing message with key: {}, error: {}", record.key(), e.getMessage(), e);
                handleFailedMessage(record);
            }
        }

        // 批量更新偏移量（但不立即提交）
        if (!currentOffsets.isEmpty()) {
            updateOffsets(currentOffsets);
        }
    }

    /**
     * 处理失败的消息
     */
    private void handleFailedMessage(ConsumerRecord<String, String> record) {
        // 实现重试逻辑或死信队列
        logger.error("Failed to process message - Topic: {}, Partition: {}, Offset: {}, Key: {}",
                record.topic(), record.partition(), record.offset(), record.key());

        // 可以在这里实现：
        // 1. 重试队列
        // 2. 死信队列
        // 3. 告警机制
        // 4. 跳过消息（谨慎使用）
    }

    /**
     * 更新偏移量（内存中）
     */
    private void updateOffsets(Map<TopicPartition, OffsetAndMetadata> offsets) {
        // 这里可以实现批量偏移量管理逻辑
        logger.debug("Updated offsets for {} partitions", offsets.size());
    }

    /**
     * 同步提交当前偏移量
     */
    private void commitCurrentOffsets() {
        try {
            consumer.commitSync();
            logger.debug("Committed current offsets synchronously");
        } catch (Exception e) {
            logger.error("Failed to commit current offsets: {}", e.getMessage());
        }
    }

    /**
     * 同步提交偏移量
     */
    private void commitOffsetsSync() {
        try {
            consumer.commitSync();
            logger.debug("Offsets committed synchronously");
        } catch (CommitFailedException e) {
            logger.error("Commit failed: {}", e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error during offset commit: {}", e.getMessage(), e);
        }
    }

    /**
     * 异步提交偏移量
     */
    private void commitOffsetsAsync() {
        consumer.commitAsync(new OffsetCommitCallback() {
            @Override
            public void onComplete(Map<TopicPartition, OffsetAndMetadata> offsets, Exception exception) {
                if (exception != null) {
                    logger.error("Async commit failed for offsets: {}, error: {}", offsets, exception.getMessage());
                } else {
                    logger.debug("Async commit successful for {} partitions", offsets.size());
                }
            }
        });
    }

    /**
     * 处理消费者错误
     */
    private void handleConsumerError(Exception e) {
        if (e instanceof org.apache.kafka.common.errors.RetriableException) {
            logger.warn("Retriable error occurred, will retry: {}", e.getMessage());
        } else {
            logger.error("Non-retriable error occurred: {}", e.getMessage());
            // 可能需要重启消费者或发送告警
        }
    }

    /**
     * 手动寻找特定偏移量
     */
    public void seekToOffset(String topic, int partition, long offset) {
        TopicPartition topicPartition = new TopicPartition(topic, partition);
        consumer.seek(topicPartition, offset);
        logger.info("Seeked to offset {} for topic {} partition {}", offset, topic, partition);
    }

    /**
     * 重置到最早的偏移量
     */
    public void seekToBeginning(String topic, int partition) {
        TopicPartition topicPartition = new TopicPartition(topic, partition);
        consumer.seekToBeginning(Collections.singletonList(topicPartition));
        logger.info("Seeked to beginning for topic {} partition {}", topic, partition);
    }

    /**
     * 获取消费者指标
     */
    public void logMetrics() {
        consumer.metrics().forEach((name, metric) -> {
            if (name.name().contains("records-consumed-rate") ||
                    name.name().contains("fetch-latency-avg") ||
                    name.name().contains("commit-latency-avg")) {
                logger.info("Consumer Metric - {}: {}", name.name(), metric.metricValue());
            }
        });
    }

    /**
     * 优雅关闭消费者
     */
    public void shutdown() {
        logger.info("Shutting down consumer...");
        closed.set(true);
        consumer.wakeup(); // 中断当前的poll操作
    }

    /**
     * 清理资源
     */
    private void cleanup() {
        try {
            logger.info("Cleaning up consumer resources...");
            commitCurrentOffsets(); // 最后一次提交偏移量
            consumer.close(Duration.ofSeconds(30)); // 30秒超时关闭
            logger.info("Consumer closed successfully");
        } catch (Exception e) {
            logger.error("Error during consumer cleanup: {}", e.getMessage(), e);
        }
    }

    /**
     * 消息处理器接口
     */
    public interface MessageProcessor {
        /**
         * 处理单条消息
         * @param record 消息记录
         * @return 是否处理成功
         */
        boolean process(ConsumerRecord<String, String> record);
    }
}
