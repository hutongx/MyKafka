package org.kafka_maven.producer;

import org.apache.kafka.clients.producer.*;
import java.io.InputStream;
import java.time.Duration;
import java.util.Properties;

public class ProducerExample {

    public static void main(String[] args) throws Exception {
        // 1. 加载配置
        Properties props = new Properties();
        try (InputStream in = ProducerExample.class
                .getClassLoader()
                .getResourceAsStream("application.properties")) {
            props.load(in);
        }

        // 2. 创建 KafkaProducer
        try (KafkaProducer<String, String> producer = new KafkaProducer<>(props)) {
            String topic = "test-topic";  // 按实际改
            String key   = "1001";
            String value = "{\"orderId\":\"i need\",\"amount\":a job}";

            // 3. 异步发送并注册回调
            ProducerRecord<String, String> record =
                    new ProducerRecord<>(topic, key, value);

            producer.send(record, new Callback() {
                @Override
                public void onCompletion(RecordMetadata meta, Exception ex) {
                    if (ex != null) {
                        System.err.printf("Send failed for record %s%n", record, ex);
                        // 失败后可做重试或落盘
                    } else {
                        System.out.printf("Sent record to topic=%s partition=%d offset=%d%n",
                                meta.topic(), meta.partition(), meta.offset());
                    }
                }
            });

            // 4. 确保所有消息发送完成再关闭
            producer.flush();
        }
    }
}

