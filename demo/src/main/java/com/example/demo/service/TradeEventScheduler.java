package com.example.demo.service;

import com.example.demo.domain.OrderEvent;
import com.example.demo.domain.Trade;
import com.example.demo.producer.OrderProducer;
import com.example.demo.producer.TradeProducer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoField;
import java.util.Random;
import java.util.UUID;

@Service
@Slf4j
public class TradeEventScheduler {

    private final TradeProducer tradeProducer;
    private final ObjectMapper objectMapper;
    private final Random random = new Random();

    public TradeEventScheduler(TradeProducer tradeProducer, ObjectMapper objectMapper) {
        this.tradeProducer = tradeProducer;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedRate = 5000)  // 上一次执行开始后 5000ms 再执行下一次
    public void sendTradePeriodically() {
        try {
            Trade trade = buildRandomTrade();
            tradeProducer.sendTrade(trade);
            // 你也可以在这里做一些日志打印
        } catch (JsonProcessingException e) {
            // 序列化失败
            log.error("生成 OrderEvent JSON 失败", e);
        }
    }

    private Trade buildRandomTrade() {
        Trade o = new Trade();
        o.setSymbol(UUID.randomUUID().toString());
        o.setPrice(random.nextInt(1_000));
        o.setTimestamp(System.currentTimeMillis());
        o.setVolume(Math.abs(random.nextLong()));
        return o;
    }
}
