package com.example.demo.kafka.model;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class Order implements Serializable {

    private static final long serialVersionUID = 1L;

    private String orderId;
    private String userId;
    private Double amount;
    private LocalDateTime createdAt;

    public Order() {
    }

    public Order(String orderId, String userId, Double amount, LocalDateTime createdAt) {
        this.orderId = orderId;
        this.userId = userId;
        this.amount = amount;
        this.createdAt = createdAt;
    }

    // Getters & Setters

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "Order{" +
                "orderId='" + orderId + '\'' +
                ", userId='" + userId + '\'' +
                ", amount=" + amount +
                ", createdAt=" + createdAt +
                '}';
    }
}

