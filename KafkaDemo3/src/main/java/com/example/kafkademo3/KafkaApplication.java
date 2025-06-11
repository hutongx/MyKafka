package com.example.kafkademo3;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Kafka Producer和Consumer使用示例
 */
public class KafkaApplication {

    private static final Logger logger = LoggerFactory.getLogger(KafkaApplication.class);

    private static final String BOOTSTRAP_SERVERS = "localhost:9092";
    private static final String TOPIC_NAME = "user-events";
    private static final String CONSUMER_GROUP = "user-event-processors";

    public static void main(String[] args) {
        KafkaApplication app = new KafkaApplication();

        // 启动生产者示例
        app.runProducerExample();

        // 启动消费者示例
        app.runConsumerExample();

        // 让程序运行一段时间
        try {
            Thread.sleep(60000); // 运行1分钟
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        logger.info("Application shutting down...");
    }

    /**
     * 生产者使用示例
     */
    public void runProducerExample() {
        KafkaMessageProducer producer = new KafkaMessageProducer(BOOTSTRAP_SERVERS, TOPIC_NAME);

        // 启动生产者线程
        ExecutorService producerExecutor = Executors.newSingleThreadExecutor();
        producerExecutor.submit(() -> {
            try {
                // 发送测试消息
                for (int i = 0; i < 100; i++) {
                    String key = "user-" + (i % 10); // 10个不同的用户
                    String message = String.format("{\"userId\":\"%s\",\"action\":\"click\",\"timestamp\":%d,\"page\":\"home\"}",
                            key, System.currentTimeMillis());

                    // 异步发送（推荐）
                    producer.sendAsync(key, message);

                    // 每10条消息同步发送一条（演示同步发送）
                    if (i % 10 == 0) {
                        try {
                            producer.sendSync(key + "-sync", message);
                            logger.info("Synchronously sent message for key: {}", key + "-sync");
                        } catch (Exception e) {
                            logger.error("Failed to send sync message: {}", e.getMessage());
                        }
                    }

                    Thread.sleep(100); // 模拟生产间隔
                }

                // 刷新缓冲区
                producer.flush();

                // 记录指标
                producer.logMetrics();

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.info("Producer thread interrupted");
            } catch (Exception e) {
                logger.error("Producer error: {}", e.getMessage(), e);
            }
        });

        // 设置执行器关闭
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            producerExecutor.shutdown();
            try {
                if (!producerExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                    producerExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                producerExecutor.shutdownNow();
            }
            producer.close();
        }));
    }

    /**
     * 消费者使用示例
     */
    public void runConsumerExample() {
        // 实现消息处理器
        KafkaMessageConsumer.MessageProcessor messageProcessor = new UserEventProcessor();

        // 创建消费者
        KafkaMessageConsumer consumer = new KafkaMessageConsumer(
                BOOTSTRAP_SERVERS,
                CONSUMER_GROUP,
                Arrays.asList(TOPIC_NAME),
                messageProcessor
        );

        // 启动消费者线程
        ExecutorService consumerExecutor = Executors.newSingleThreadExecutor();
        consumerExecutor.submit(() -> {
            try {
                consumer.start(); // 这会阻塞直到关闭
            } catch (Exception e) {
                logger.error("Consumer error: {}", e.getMessage(), e);
            }
        });

        // 设置关闭钩子
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutting down consumer...");
            consumer.shutdown();
            consumerExecutor.shutdown();
            try {
                if (!consumerExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                    consumerExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                consumerExecutor.shutdownNow();
            }
        }));

        // 定期记录消费者指标
        ExecutorService metricsExecutor = Executors.newScheduledThreadPool(1);
        ((java.util.concurrent.ScheduledExecutorService) metricsExecutor).scheduleAtFixedRate(
                consumer::logMetrics, 30, 30, TimeUnit.SECONDS
        );
    }

    /**
     * 用户事件处理器实现
     */
    static class UserEventProcessor implements KafkaMessageConsumer.MessageProcessor {

        private static final Logger logger = LoggerFactory.getLogger(UserEventProcessor.class);

        @Override
        public boolean process(ConsumerRecord<String, String> record) {
            try {
                logger.info("Processing user event - Key: {}, Value: {}, Partition: {}, Offset: {}",
                        record.key(), record.value(), record.partition(), record.offset());

                // 模拟业务处理逻辑
                processUserEvent(record.key(), record.value());

                return true; // 处理成功

            } catch (Exception e) {
                logger.error("Failed to process user event for key: {}, error: {}",
                        record.key(), e.getMessage(), e);
                return false; // 处理失败
            }
        }

        private void processUserEvent(String userId, String eventData) {
            // 实际的业务逻辑处理
            // 例如：解析JSON、存储到数据库、发送到其他系统等

            // 模拟处理时间
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            logger.debug("User event processed successfully for user: {}", userId);
        }
    }
}
