package com.onlinebanking.dto;

import com.onlinebanking.entity.AccountType;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@AllArgsConstructor
public class AdminAccountResponse {
    private Long id;
    private String accountNumber;
    private AccountType accountType;
    private String currency;
    private BigDecimal balance;
    private boolean active;
    private String ownerEmail;
    private Instant createdAt;
}
