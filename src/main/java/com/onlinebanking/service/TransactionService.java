package com.onlinebanking.service;

import com.onlinebanking.dto.TransactionResponse;
import com.onlinebanking.dto.TransactionRequest;
import com.onlinebanking.entity.AuditEventType;
import com.onlinebanking.entity.BankAccount;
import com.onlinebanking.entity.TransactionRecord;
import com.onlinebanking.entity.TransactionType;
import com.onlinebanking.exception.ApiException;
import com.onlinebanking.repository.BankAccountRepository;
import com.onlinebanking.repository.TransactionRepository;
import com.onlinebanking.service.AuditLogService;
import com.onlinebanking.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final BankAccountRepository bankAccountRepository;
    private final TransactionRepository transactionRepository;
    private final AuditLogService auditLogService;
    private final EmailService emailService;

    @Transactional
    public TransactionResponse transferFunds(TransactionRequest request, String initiatorEmail, String remoteAddress) {
        validateTransfer(request);

        BankAccount source = bankAccountRepository.findByAccountNumber(request.getSourceAccountNumber())
                .orElseThrow(() -> new ApiException("Source account not found"));
        BankAccount destination = bankAccountRepository.findByAccountNumber(request.getDestinationAccountNumber())
                .orElseThrow(() -> new ApiException("Destination account not found"));

        if (!source.getOwner().getEmail().equalsIgnoreCase(initiatorEmail)) {
            throw new ApiException("Unauthorized transfer attempt");
        }
        if (!source.isActive() || !destination.isActive()) {
            throw new ApiException("Both accounts must be active for transfer");
        }
        if (!source.getCurrency().equalsIgnoreCase(destination.getCurrency())) {
            throw new ApiException("Source and destination must use the same currency");
        }
        if (source.getBalance().compareTo(request.getAmount()) < 0) {
            throw new ApiException("Insufficient balance for transfer");
        }

        source.setBalance(source.getBalance().subtract(request.getAmount()));
        destination.setBalance(destination.getBalance().add(request.getAmount()));

        TransactionRecord transaction = new TransactionRecord();
        transaction.setTransactionType(TransactionType.TRANSFER);
        transaction.setAmount(request.getAmount());
        transaction.setCurrency(source.getCurrency());
        transaction.setDescription(request.getDescription());
        transaction.setSourceAccount(source);
        transaction.setDestinationAccount(destination);
        transaction.setInitiatedBy(source.getOwner());

        bankAccountRepository.save(source);
        bankAccountRepository.save(destination);
        TransactionRecord saved = transactionRepository.save(transaction);
        auditLogService.logEvent(AuditEventType.TRANSFER, "Transfer of " + request.getAmount() + " from " + source.getAccountNumber() + " to " + destination.getAccountNumber(), remoteAddress, source.getOwner());

        // Notify source owner (outgoing transfer)
        emailService.sendTransferConfirmation(
                source.getOwner().getEmail(), 
                source.getOwner().getFirstName(), 
                source.getAccountNumber(), 
                destination.getAccountNumber(), 
                request.getAmount(), 
                source.getCurrency(), 
                true
        );
        
        // Notify destination owner (incoming transfer, if different user)
        if (!source.getOwner().getId().equals(destination.getOwner().getId())) {
            emailService.sendTransferConfirmation(
                    destination.getOwner().getEmail(), 
                    destination.getOwner().getFirstName(), 
                    source.getAccountNumber(), 
                    destination.getAccountNumber(), 
                    request.getAmount(), 
                    source.getCurrency(), 
                    false
            );
        }

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> getAccountHistory(String accountNumber, String ownerEmail, Pageable pageable) {
        BankAccount account = bankAccountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ApiException("Bank account not found"));
        if (!account.getOwner().getEmail().equalsIgnoreCase(ownerEmail)) {
            throw new ApiException("Unauthorized access to transaction history");
        }

        return transactionRepository.findBySourceAccountIdOrDestinationAccountId(account.getId(), account.getId(), pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> getAllTransactions(String ownerEmail, Pageable pageable) {
        return transactionRepository.findByInitiatedByEmail(ownerEmail, pageable)
                .map(this::toResponse);
    }

    private void validateTransfer(TransactionRequest request) {
        if (request.getSourceAccountNumber().equals(request.getDestinationAccountNumber())) {
            throw new ApiException("Source and destination accounts must be different");
        }
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiException("Transfer amount must be greater than zero");
        }
    }

    private TransactionResponse toResponse(TransactionRecord record) {
        return new TransactionResponse(
                record.getId(),
                record.getTransactionType(),
                record.getAmount(),
                record.getCurrency(),
                record.getDescription(),
                record.getCreatedAt(),
                record.getSourceAccount() != null ? record.getSourceAccount().getAccountNumber() : null,
                record.getDestinationAccount() != null ? record.getDestinationAccount().getAccountNumber() : null
        );
    }
}
