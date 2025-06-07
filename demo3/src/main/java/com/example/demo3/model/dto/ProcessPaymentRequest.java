package com.example.demo3.model.dto;

import com.example.demo3.enums.PaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessPaymentRequest {
//    @NotBlank(message = "Order ID is required")
    public String orderId;

//    @NotNull(message = "Amount is required")
//    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    public BigDecimal amount;

//    @NotNull(message = "Payment method is required")
    public PaymentMethod method;

    public String traceId;
}
