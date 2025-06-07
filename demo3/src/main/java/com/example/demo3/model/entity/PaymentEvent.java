package com.example.demo3.model.entity;

import com.example.demo3.enums.PaymentMethod;
import com.example.demo3.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentEvent {
    public String paymentId;
    public String orderId;
    public BigDecimal amount;
    public PaymentMethod method;
    public PaymentStatus status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    public LocalDateTime processedAt;

    public String traceId;
}
