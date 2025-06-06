package com.example.demo.domain;

import lombok.Data;

import java.io.Serializable;

@Data
public class OrderMessage implements Serializable {

    private String orderId;

    private double amount;
}


