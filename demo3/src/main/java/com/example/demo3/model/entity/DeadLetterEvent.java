package com.example.demo3.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeadLetterEvent {
    private Object originalEvent;
    private String errorMessage;
    private LocalDateTime timestamp;
    private Integer retryCount;
}
