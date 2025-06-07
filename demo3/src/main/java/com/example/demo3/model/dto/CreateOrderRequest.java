package com.example.demo3.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {
//    @NotBlank(message = "User ID is required")
    public String userId;

//    @NotBlank(message = "Product ID is required")
    public String productId;

//    @NotNull(message = "Amount is required")
//    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    public BigDecimal amount;

    public String traceId;
}
