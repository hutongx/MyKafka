package com.example.kafkademo.model.DTOs;

// Request/Response DTOs
public class UserEventRequest {
    private String userId;
    private String eventType;
    private String eventData;

    public UserEventRequest() {}

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getEventData() { return eventData; }
    public void setEventData(String eventData) { this.eventData = eventData; }
}
