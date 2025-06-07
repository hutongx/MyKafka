package com.example.demo3.model.dto;

import com.example.demo3.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {
    public String orderId;
    public String userId;
    public String productId;
    public BigDecimal amount;
    public OrderStatus status;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;
}
