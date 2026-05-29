package com.onlinebanking.controller;

import com.onlinebanking.dto.AccountResponse;
import com.onlinebanking.dto.AmountRequest;
import com.onlinebanking.dto.ApiResponse;
import com.onlinebanking.dto.CreateAccountRequest;
import com.onlinebanking.service.BankAccountService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/customer/accounts")
@RequiredArgsConstructor
@Tag(name = "Bank Accounts", description = "Endpoints for managing bank accounts (Checking/Savings), deposits, and withdrawals")
public class BankAccountController {

    private final BankAccountService bankAccountService;

    @GetMapping
    @Operation(summary = "Get all accounts", description = "Retrieves all active bank accounts for the authenticated user")
    public ResponseEntity<ApiResponse<List<AccountResponse>>> getAllAccounts(Principal principal) {
        List<AccountResponse> response = bankAccountService.getAllAccounts(principal.getName());
        return ResponseEntity.ok(new ApiResponse<>(Instant.now(), HttpStatus.OK.value(), "Accounts retrieved", response));
    }

    @PostMapping
    @Operation(summary = "Create an account", description = "Creates a new Checking or Savings account for the authenticated user")
    public ResponseEntity<ApiResponse<AccountResponse>> createAccount(@Valid @RequestBody CreateAccountRequest request,
                                                                      Principal principal,
                                                                      HttpServletRequest servletRequest) {
        AccountResponse response = bankAccountService.createAccount(principal.getName(), request, servletRequest.getRemoteAddr());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(Instant.now(), HttpStatus.CREATED.value(), "Account created", response));
    }

    @GetMapping("/{accountNumber}")
    @Operation(summary = "Get account details", description = "Retrieves complete details of a specific bank account")
    public ResponseEntity<ApiResponse<AccountResponse>> getAccount(@PathVariable String accountNumber,
                                                                   Principal principal) {
        AccountResponse response = bankAccountService.getAccountDetails(accountNumber, principal.getName());
        return ResponseEntity.ok(new ApiResponse<>(Instant.now(), HttpStatus.OK.value(), "Account details", response));
    }

    @GetMapping("/{accountNumber}/balance")
    @Operation(summary = "Get account balance", description = "Retrieves current balance of a specific bank account")
    public ResponseEntity<ApiResponse<BigDecimal>> getBalance(@PathVariable String accountNumber,
                                                               Principal principal) {
        BigDecimal balance = bankAccountService.getBalance(accountNumber, principal.getName());
        return ResponseEntity.ok(new ApiResponse<>(Instant.now(), HttpStatus.OK.value(), "Account balance", balance));
    }

    @PostMapping("/{accountNumber}/deposit")
    @Operation(summary = "Deposit cash", description = "Deposits money into a specific bank account")
    public ResponseEntity<ApiResponse<AccountResponse>> deposit(@PathVariable String accountNumber,
                                                                 @Valid @RequestBody AmountRequest request,
                                                                 Principal principal,
                                                                 HttpServletRequest servletRequest) {
        AccountResponse response = bankAccountService.deposit(accountNumber, request.getAmount(), principal.getName(), request.getCurrency(), request.getDescription(), servletRequest.getRemoteAddr());
        return ResponseEntity.ok(new ApiResponse<>(Instant.now(), HttpStatus.OK.value(), "Deposit successful", response));
    }

    @PostMapping("/{accountNumber}/withdraw")
    @Operation(summary = "Withdraw cash", description = "Withdraws money from a specific bank account")
    public ResponseEntity<ApiResponse<AccountResponse>> withdraw(@PathVariable String accountNumber,
                                                                  @Valid @RequestBody AmountRequest request,
                                                                  Principal principal,
                                                                  HttpServletRequest servletRequest) {
        AccountResponse response = bankAccountService.withdraw(accountNumber, request.getAmount(), principal.getName(), request.getCurrency(), request.getDescription(), servletRequest.getRemoteAddr());
        return ResponseEntity.ok(new ApiResponse<>(Instant.now(), HttpStatus.OK.value(), "Withdrawal successful", response));
    }
}
