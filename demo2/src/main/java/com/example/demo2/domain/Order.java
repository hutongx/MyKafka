package com.example.demo2.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
//import org.hibernate.validator.constraints.Email;
//import org.hibernate.validator.constraints.NotBlank;
//import org.hibernate.validator.constraints.NotEmpty;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Data
@Builder
public class Order implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty("orderId")
//    @NotBlank(message = "Order ID cannot be blank")
    private String orderId;

    @JsonProperty("customerId")
//    @NotBlank(message = "Customer ID cannot be blank")
    private String customerId;

    @JsonProperty("customerEmail")
//    @Email(message = "Invalid email format")
//    @NotBlank(message = "Customer email cannot be blank")
    private String customerEmail;

    @JsonProperty("items")
//    @NotEmpty(message = "Order must have at least one item")
    private List<OrderItem> items;

    @JsonProperty("totalAmount")
//    @DecimalMin(value = "0.01", message = "Total amount must be greater than 0")
//    @NotNull(message = "Total amount cannot be null")
    private BigDecimal totalAmount;

    @JsonProperty("currency")
//    @NotBlank(message = "Currency cannot be blank")
    private String currency;

    @JsonProperty("status")
//    @NotNull(message = "Status cannot be null")
    private OrderStatus status;

    @JsonProperty("shippingAddress")
//    @NotNull(message = "Shipping address cannot be null")
    private Address shippingAddress;

    @JsonProperty("createdAt")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonProperty("updatedAt")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    @JsonProperty("version")
    private Long version;

    // Default constructor required for JSON deserialization
    public Order() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.version = 1L;
        this.status = OrderStatus.PENDING;
    }

    // Constructor with required fields
    public Order(String orderId, String customerId, String customerEmail,
                 List<OrderItem> items, BigDecimal totalAmount, String currency,
                 Address shippingAddress) {
        this();
        this.orderId = orderId;
        this.customerId = customerId;
        this.customerEmail = customerEmail;
        this.items = items;
        this.totalAmount = totalAmount;
        this.currency = currency;
        this.shippingAddress = shippingAddress;
    }

    // Business methods
    public String getPartitionKey() {
        return this.customerId; // Ensure all orders from same customer go to same partition
    }

    public void markAsProcessing() {
        this.status = OrderStatus.PROCESSING;
        this.updatedAt = LocalDateTime.now();
        this.version++;
    }

    public void markAsCompleted() {
        this.status = OrderStatus.COMPLETED;
        this.updatedAt = LocalDateTime.now();
        this.version++;
    }

    public void markAsFailed() {
        this.status = OrderStatus.FAILED;
        this.updatedAt = LocalDateTime.now();
        this.version++;
    }

    public boolean isPending() {
        return OrderStatus.PENDING.equals(this.status);
    }

    public boolean isProcessing() {
        return OrderStatus.PROCESSING.equals(this.status);
    }

    public boolean isCompleted() {
        return OrderStatus.COMPLETED.equals(this.status);
    }

    public boolean isFailed() {
        return OrderStatus.FAILED.equals(this.status);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Order order = (Order) o;
        return Objects.equals(orderId, order.orderId) &&
                Objects.equals(version, order.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(orderId, version);
    }

    @Override
    public String toString() {
        return "Order{" +
                "orderId='" + orderId + '\'' +
                ", customerId='" + customerId + '\'' +
                ", customerEmail='" + customerEmail + '\'' +
                ", totalAmount=" + totalAmount +
                ", currency='" + currency + '\'' +
                ", status=" + status +
                ", createdAt=" + createdAt +
                ", version=" + version +
                '}';
    }

    // Inner classes for better structure
    public static class OrderItem implements Serializable {
        private static final long serialVersionUID = 1L;

        @JsonProperty("productId")
//        @NotBlank(message = "Product ID cannot be blank")
        private String productId;

        @JsonProperty("productName")
//        @NotBlank(message = "Product name cannot be blank")
        private String productName;

        @JsonProperty("quantity")
//        @Min(value = 1, message = "Quantity must be at least 1")
        private Integer quantity;

        @JsonProperty("unitPrice")
//        @DecimalMin(value = "0.01", message = "Unit price must be greater than 0")
        private BigDecimal unitPrice;

        @JsonProperty("totalPrice")
//        @DecimalMin(value = "0.01", message = "Total price must be greater than 0")
        private BigDecimal totalPrice;

        // Default constructor
        public OrderItem() {}

        public OrderItem(String productId, String productName, Integer quantity, BigDecimal unitPrice) {
            this.productId = productId;
            this.productName = productName;
            this.quantity = quantity;
            this.unitPrice = unitPrice;
            this.totalPrice = unitPrice.multiply(BigDecimal.valueOf(quantity));
        }

        // Getters and Setters
        public String getProductId() { return productId; }
        public void setProductId(String productId) { this.productId = productId; }

        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }

        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
            if (this.unitPrice != null) {
                this.totalPrice = this.unitPrice.multiply(BigDecimal.valueOf(quantity));
            }
        }

        public BigDecimal getUnitPrice() { return unitPrice; }
        public void setUnitPrice(BigDecimal unitPrice) {
            this.unitPrice = unitPrice;
            if (this.quantity != null) {
                this.totalPrice = unitPrice.multiply(BigDecimal.valueOf(quantity));
            }
        }

        public BigDecimal getTotalPrice() { return totalPrice; }
        public void setTotalPrice(BigDecimal totalPrice) { this.totalPrice = totalPrice; }
    }

    public static class Address implements Serializable {
        private static final long serialVersionUID = 1L;

        @JsonProperty("street")
//        @NotBlank(message = "Street cannot be blank")
        private String street;

        @JsonProperty("city")
//        @NotBlank(message = "City cannot be blank")
        private String city;

        @JsonProperty("state")
//        @NotBlank(message = "State cannot be blank")
        private String state;

        @JsonProperty("zipCode")
//        @NotBlank(message = "ZIP code cannot be blank")
        private String zipCode;

        @JsonProperty("country")
//        @NotBlank(message = "Country cannot be blank")
        private String country;

        // Default constructor
        public Address() {}

        public Address(String street, String city, String state, String zipCode, String country) {
            this.street = street;
            this.city = city;
            this.state = state;
            this.zipCode = zipCode;
            this.country = country;
        }

        // Getters and Setters
        public String getStreet() { return street; }
        public void setStreet(String street) { this.street = street; }

        public String getCity() { return city; }
        public void setCity(String city) { this.city = city; }

        public String getState() { return state; }
        public void setState(String state) { this.state = state; }

        public String getZipCode() { return zipCode; }
        public void setZipCode(String zipCode) { this.zipCode = zipCode; }

        public String getCountry() { return country; }
        public void setCountry(String country) { this.country = country; }

        @Override
        public String toString() {
            return "Address{" +
                    "street='" + street + '\'' +
                    ", city='" + city + '\'' +
                    ", state='" + state + '\'' +
                    ", zipCode='" + zipCode + '\'' +
                    ", country='" + country + '\'' +
                    '}';
        }
    }

    public enum OrderStatus {
        PENDING("PENDING"),
        PROCESSING("PROCESSING"),
        COMPLETED("COMPLETED"),
        CANCELLED("CANCELLED"),
        FAILED("FAILED");

        private final String value;

        OrderStatus(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        @Override
        public String toString() {
            return value;
        }
    }
}
