package com.example.demo2.service;

import com.example.demo2.domain.Order;
import com.example.demo2.utils.OrderRequest;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public interface OrderService {
    CompletableFuture<String> createOrder(OrderRequest orderRequest);
    void processOrder(Order order);
    boolean validateOrder(Order order);
}
