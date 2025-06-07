package com.example.demo.kafka.consumer;

import com.example.demo.kafka.model.Order;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * OrderConsumer 演示了如何批量消费同一分区的多条记录并手动提交 offset。
 * 这里使用 MANUAL_IMMEDIATE 模式，一旦调用 ack.acknowledge()，offset 立即提交到 broker。
 */
@Component
public class OrderConsumer {

    private static final Logger logger = LoggerFactory.getLogger(OrderConsumer.class);

    /**
     * 监听 orders 这个 topic，group 为 order-consumer-group，批量拉取最大 50 条 (由 max.poll.records 控制)
     *
     * @param records 批量拉取的 ConsumerRecord 列表
     * @param ack     Acknowledgment 用于手动提交 offset
     */
    @KafkaListener(topics = "orders", groupId = "order-consumer-group",
            containerFactory = "kafkaListenerContainerFactory")
    public void listenOrders(List<ConsumerRecord<String, Order>> records,
                             Acknowledgment ack) {

        if (records == null || records.isEmpty()) {
            return;
        }

        // 按分区分组，在同一个分区内顺序处理
        Map<TopicPartition, List<ConsumerRecord<String, Order>>> recordsByPartition =
                records.stream().collect(Collectors.groupingBy(rec ->
                        new TopicPartition(rec.topic(), rec.partition())));

        for (Map.Entry<TopicPartition, List<ConsumerRecord<String, Order>>> entry : recordsByPartition.entrySet()) {
            TopicPartition partition = entry.getKey();
            List<ConsumerRecord<String, Order>> partitionRecords = entry.getValue();

            logger.info("🛎️ 处理 partition={} 上的 {} 条消息", partition.partition(), partitionRecords.size());

            // 按 offset 从小到大依次处理
            for (ConsumerRecord<String, Order> record : partitionRecords) {
                Order order = record.value();
                logger.info("   → 接收到订单：partition={}, offset={}, key={}, order={}",
                        record.partition(), record.offset(), record.key(), order);
                // TODO: 在这里执行业务逻辑，比如保存到数据库、调用其他微服务等
            }

            // 取本批次最后一条记录的 offset，然后提交 offset+1
            long lastOffset = partitionRecords.get(partitionRecords.size() - 1).offset();
            // ack.acknowledge() 会提交到 broker (同步)
            ack.acknowledge();
            logger.info("   🆗 已提交 offset (partition={}, nextOffset={})",
                    partition.partition(), lastOffset + 1);
        }
    }
}

