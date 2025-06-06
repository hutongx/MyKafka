package com.example.demo.domain;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OrderEvent {
    public String orderId;
    public String userId;
    public String skuId;
    public String orderAmount;
    public LocalDateTime orderTime;
    public String status;
}

//            "order_id": "202504210001",
//            "user_id": "U10023",
//            "sku_id": "SKU789",
//            "order_amount": 259.90,
//            "order_time": "2025-04-21T13:45:02Z",
//            "status": "TO_PAY"
