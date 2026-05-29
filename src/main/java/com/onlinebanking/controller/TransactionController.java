package com.onlinebanking.controller;

import com.onlinebanking.dto.ApiResponse;
import com.onlinebanking.dto.PagedResponse;
import com.onlinebanking.dto.TransactionRequest;
import com.onlinebanking.dto.TransactionResponse;
import com.onlinebanking.service.TransactionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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

import java.security.Principal;
import java.time.Instant;

@RestController
@RequestMapping("/api/customer/transactions")
@RequiredArgsConstructor
@Tag(name = "Transactions", description = "Endpoints for funds transfer and transaction history")
public class TransactionController {

    private final TransactionService transactionService;

    @GetMapping
    @Operation(summary = "Get all transactions", description = "Retrieves all transactions across all accounts of the authenticated user")
    public ResponseEntity<ApiResponse<PagedResponse<TransactionResponse>>> getAllTransactions(
            Principal principal,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PagedResponse<TransactionResponse> response = PagedResponse.fromPage(
                transactionService.getAllTransactions(principal.getName(), pageable));
        return ResponseEntity.ok(new ApiResponse<>(Instant.now(), HttpStatus.OK.value(), "Transactions retrieved", response));
    }

    @PostMapping
    @Operation(summary = "Transfer funds", description = "Transfers money from a source account to a destination account")
    public ResponseEntity<ApiResponse<TransactionResponse>> transfer(@Valid @RequestBody TransactionRequest request,
                                                                     Principal principal,
                                                                     HttpServletRequest servletRequest) {
        TransactionResponse response = transactionService.transferFunds(request, principal.getName(), servletRequest.getRemoteAddr());
        return ResponseEntity.ok(new ApiResponse<>(Instant.now(), HttpStatus.OK.value(), "Transfer completed", response));
    }

    @GetMapping("/{accountNumber}")
    @Operation(summary = "Get transaction history by account", description = "Retrieves transaction history for a specific account")
    public ResponseEntity<ApiResponse<PagedResponse<TransactionResponse>>> getHistory(
            @PathVariable String accountNumber,
            Principal principal,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PagedResponse<TransactionResponse> response = PagedResponse.fromPage(
                transactionService.getAccountHistory(accountNumber, principal.getName(), pageable));
        return ResponseEntity.ok(new ApiResponse<>(Instant.now(), HttpStatus.OK.value(), "Transaction history", response));
    }
}
