package com.onlinebanking.dto;

import com.onlinebanking.entity.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@AllArgsConstructor
public class TransactionResponse {
    private Long id;
    private TransactionType transactionType;
    private BigDecimal amount;
    private String currency;
    private String description;
    private Instant createdAt;
    private String sourceAccountNumber;
    private String destinationAccountNumber;
}
