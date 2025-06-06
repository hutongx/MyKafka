package org.kafka_maven.domain;

import java.io.Serializable;

public class OrderMessage implements Serializable {
    private String orderId;
    private double amount;

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    @Override
    public String toString() {
        return "OrderMessage{" +
                "orderId='" + orderId + '\'' +
                ", amount=" + amount +
                '}';
    }
}

