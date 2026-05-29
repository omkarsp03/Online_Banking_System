package com.onlinebanking.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;

@Data
@AllArgsConstructor
public class ApiResponse<T> {
    private Instant timestamp;
    private int status;
    private String message;
    private T data;
}
