package com.example.demo.kafka.config;

import com.example.demo.kafka.model.Order;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    // ------------------- Producer 相关 -------------------

    @Bean
    public ProducerFactory<String, Order> producerFactory() {
        Map<String, Object> props = new HashMap<>();
        // Kafka 集群地址
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        // Key 序列化
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        // Value 序列化为 JSON
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        // 等待 ISR 所有副本都写入才返回 ACK
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        // 幂等
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        props.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 1);
        // 重试次数
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, Order> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    // 如果要直接创建 Topic，可以在这里声明。例如：
    @Bean
    public NewTopic ordersTopic() {
        // 第一个参数：topic 名称，第二个：partition 数量，第三个：replica factor
        return new NewTopic("orders", 4, (short) 2);
    }

    // ------------------- Consumer 相关 -------------------

    @Bean
    public ConsumerFactory<String, Order> consumerFactory() {
        JsonDeserializer<Order> deserializer = new JsonDeserializer<>(Order.class);
        // 允许反序列化 com.example.kafka.model 下的类
        deserializer.addTrustedPackages("com.example.kafka.model");

        Map<String, Object> props = new HashMap<>();
        // Kafka 集群地址
        props.put(org.apache.kafka.clients.consumer.ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        // group.id
        props.put(org.apache.kafka.clients.consumer.ConsumerConfig.GROUP_ID_CONFIG, "order-consumer-group");
        // Key 反序列化
        props.put(org.apache.kafka.clients.consumer.ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        // Value 反序列化为 JSON
        props.put(org.apache.kafka.clients.consumer.ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        // 关闭自动提交
        props.put(org.apache.kafka.clients.consumer.ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        // 如果第一次消费不存在 offset，则从最早的记录开始消费
        props.put(org.apache.kafka.clients.consumer.ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        // 一次 poll 最多拉取 50 条记录，方便批量提交
        props.put(org.apache.kafka.clients.consumer.ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 50);

        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), deserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Order> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Order> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        // 手动提交 offset
        factory.getContainerProperties().setAckMode(
                org.springframework.kafka.listener.ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        // 如果需要并行消费，可以设置并发数量
        factory.setConcurrency(3);
        return factory;
    }
}

