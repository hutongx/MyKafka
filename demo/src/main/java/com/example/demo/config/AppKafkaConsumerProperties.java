package com.example.demo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@ConfigurationProperties(prefix = "app.kafka")
public class AppKafkaConsumerProperties {
    /** 业务 Topic 映射 */
    private Map<String, String> topics;
    /** 死信主题 */
    private ConsumerProps consumer = new ConsumerProps();

    public Map<String, String> getTopics() { return topics; }
    public void setTopics(Map<String, String> topics) { this.topics = topics; }

    public ConsumerProps getConsumer() { return consumer; }
    public void setConsumer(ConsumerProps consumer) { this.consumer = consumer; }

    public static class ConsumerProps {
        private String deadLetterTopic;
        public String getDeadLetterTopic() { return deadLetterTopic; }
        public void setDeadLetterTopic(String deadLetterTopic) {
            this.deadLetterTopic = deadLetterTopic;
        }
    }
}

