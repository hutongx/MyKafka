package com.example.kafkademo.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.Objects;

public class UserEvent {

    @JsonProperty("user_id")
    private String userId;

    @JsonProperty("event_type")
    private String eventType;

    @JsonProperty("event_data")
    private String eventData;

    @JsonProperty("timestamp")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;

    @JsonProperty("correlation_id")
    private String correlationId;

    public UserEvent() {}

    public UserEvent(String userId, String eventType, String eventData, String correlationId) {
        this.userId = userId;
        this.eventType = eventType;
        this.eventData = eventData;
        this.correlationId = correlationId;
        this.timestamp = LocalDateTime.now();
    }

    // Getters and Setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getEventData() { return eventData; }
    public void setEventData(String eventData) { this.eventData = eventData; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserEvent userEvent = (UserEvent) o;
        return Objects.equals(userId, userEvent.userId) &&
                Objects.equals(eventType, userEvent.eventType) &&
                Objects.equals(correlationId, userEvent.correlationId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, eventType, correlationId);
    }

    @Override
    public String toString() {
        return "UserEvent{" +
                "userId='" + userId + '\'' +
                ", eventType='" + eventType + '\'' +
                ", timestamp=" + timestamp +
                ", correlationId='" + correlationId + '\'' +
                '}';
    }
}
