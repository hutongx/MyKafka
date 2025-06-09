package com.example.kafkademo.model.DTOs;

public class SendEventResponse {
    private String message;
    private String topic;
    private Integer partition;
    private Long offset;
    private String correlationId;

    public SendEventResponse() {}

    public SendEventResponse(String message) {
        this.message = message;
    }

    public SendEventResponse(String message, String topic, Integer partition, Long offset, String correlationId) {
        this.message = message;
        this.topic = topic;
        this.partition = partition;
        this.offset = offset;
        this.correlationId = correlationId;
    }

    // Getters and setters
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }

    public Integer getPartition() { return partition; }
    public void setPartition(Integer partition) { this.partition = partition; }

    public Long getOffset() { return offset; }
    public void setOffset(Long offset) { this.offset = offset; }

    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
}
