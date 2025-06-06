package org.kafka_maven.consumer;

import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.TopicPartition;

import java.io.InputStream;
import java.time.Duration;
import java.util.*;

public class ConsumerExample {

    public static void main(String[] args) throws Exception {
        // 1. 加载配置
        Properties props = new Properties();
        try (InputStream in = ConsumerExample.class
                .getClassLoader()
                .getResourceAsStream("application.properties")) {
            props.load(in);
        }

        // 2. 创建 KafkaConsumer
        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
            String topic = "test2";
            consumer.subscribe(Collections.singletonList(topic));

            // 3. 循环拉取
//            while (true) {
//                ConsumerRecords<String, String> records =
//                        consumer.poll(Duration.ofMillis(1000));
//
//                for (ConsumerRecord<String, String> rec : records) {
//                    System.out.printf("Received message: key=%s value=%s partition=%d offset=%d%n",
//                            rec.key(), rec.value(), rec.partition(), rec.offset());
//                    // 业务处理...
//                }
//
//                // 4. 手动同步提交 offset
//                consumer.commitSync();
//            }
            while (true) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));
                if (records.isEmpty()) {
                    continue;           // 没拿到新数据就别提交
                }

                for (ConsumerRecord<String, String> rec : records) {
                    System.out.printf("key=%s value=%s partition=%d offset=%d%n",
                            rec.key(), rec.value(), rec.partition(), rec.offset());
                    // TODO 业务逻辑
                }
                consumer.commitSync();  // 只在真正处理完一批数据后提交
            }
        }
    }
}

