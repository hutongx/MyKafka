package com.example.demo.domain;

import lombok.Data;

// 1. 定义交易数据 POJO
@Data
public class Trade {
    public String symbol;    // 股票代码
    public double price;     // 成交价格
    public long volume;      // 成交量
    public long timestamp;   // 事件时间，毫秒级

    // Jackson 反序列化需要无参构造器
    public Trade() {}

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public long getVolume() {
        return volume;
    }

    public void setVolume(long volume) {
        this.volume = volume;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "Trade{" +
                "symbol='" + symbol + '\'' +
                ", price=" + price +
                ", volume=" + volume +
                ", timestamp=" + timestamp +
                '}';
    }
}
