package com.example.demo2.utils;

import com.example.demo2.domain.Order;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.hibernate.validator.constraints.NotBlank;
import org.hibernate.validator.constraints.NotEmpty;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.util.List;

@Data
public class OrderRequest {

    @NotBlank(message = "Customer ID is required")
//    @Size(min = 3, max = 50, message = "Customer ID must be between 3 and 50 characters")
    @JsonProperty("customerId")
    private String customerId;

    @NotEmpty(message = "Order must have at least one item")
//    @Valid
    @JsonProperty("items")
    private List<Order.OrderItem> items;

//    @NotNull(message = "Total amount is required")
//    @DecimalMin(value = "0.01", message = "Total amount must be greater than 0")
//    @Digits(integer = 10, fraction = 2, message = "Total amount must have at most 10 integer digits and 2 decimal places")
    @JsonProperty("totalAmount")
    private BigDecimal totalAmount;

//    @Valid
    @JsonProperty("shippingAddress")
    private Order.Address shippingAddress;

//    @Valid
    @JsonProperty("billingAddress")
    private Order.Address billingAddress;

    @JsonProperty("paymentMethod")
    private String paymentMethod;

//    @Size(max = 500, message = "Special instructions cannot exceed 500 characters")
    @JsonProperty("specialInstructions")
    private String specialInstructions;

    // Default constructor
    public OrderRequest() {}

    // Constructor with required fields
    public OrderRequest(String customerId, List<Order.OrderItem> items, BigDecimal totalAmount) {
        this.customerId = customerId;
        this.items = items;
        this.totalAmount = totalAmount;
    }

    // Getters and Setters
    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public List<Order.OrderItem> getItems() {
        return items;
    }

    public void setItems(List<Order.OrderItem> items) {
        this.items = items;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public Order.Address getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(Order.Address shippingAddress) {
        this.shippingAddress = shippingAddress;
    }

    public Order.Address getBillingAddress() {
        return billingAddress;
    }

    public void setBillingAddress(Order.Address billingAddress) {
        this.billingAddress = billingAddress;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getSpecialInstructions() {
        return specialInstructions;
    }

    public void setSpecialInstructions(String specialInstructions) {
        this.specialInstructions = specialInstructions;
    }

    @Override
    public String toString() {
        return "OrderRequest{" +
                "customerId='" + customerId + '\'' +
                ", itemCount=" + (items != null ? items.size() : 0) +
                ", totalAmount=" + totalAmount +
                ", paymentMethod='" + paymentMethod + '\'' +
                '}';
    }
}
