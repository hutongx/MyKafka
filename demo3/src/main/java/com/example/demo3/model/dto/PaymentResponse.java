package com.example.demo3.model.dto;

import com.example.demo3.enums.PaymentMethod;
import com.example.demo3.enums.PaymentStatus;
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
public class PaymentResponse {
    public String paymentId;
    public String orderId;
    public BigDecimal amount;
    public PaymentMethod method;
    public PaymentStatus status;
    public LocalDateTime processedAt;
    public String transactionId;
}
