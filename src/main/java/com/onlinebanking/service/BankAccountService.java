package com.onlinebanking.service;

import com.onlinebanking.dto.AccountResponse;
import com.onlinebanking.dto.CreateAccountRequest;
import com.onlinebanking.entity.AuditEventType;
import com.onlinebanking.entity.BankAccount;
import com.onlinebanking.entity.TransactionRecord;
import com.onlinebanking.entity.TransactionType;
import com.onlinebanking.entity.User;
import com.onlinebanking.exception.ApiException;
import com.onlinebanking.repository.BankAccountRepository;
import com.onlinebanking.repository.TransactionRepository;
import com.onlinebanking.repository.UserRepository;
import com.onlinebanking.service.AuditLogService;
import com.onlinebanking.service.EmailService;
import com.onlinebanking.util.AccountNumberGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BankAccountService {

    private final BankAccountRepository bankAccountRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final AuditLogService auditLogService;
    private final EmailService emailService;

    @Transactional
    public AccountResponse createAccount(String ownerEmail, CreateAccountRequest request, String remoteAddress) {
        User owner = findUserByEmail(ownerEmail);
        String accountNumber = createUniqueAccountNumber();

        BankAccount account = new BankAccount();
        account.setAccountNumber(accountNumber);
        account.setAccountType(request.getAccountType());
        account.setCurrency(request.getCurrency().toUpperCase());
        account.setBalance(BigDecimal.ZERO);
        account.setActive(true);
        account.setOwner(owner);

        BankAccount saved = bankAccountRepository.save(account);
        auditLogService.logEvent(AuditEventType.ACCOUNT_CREATED, "Created account " + saved.getAccountNumber(), remoteAddress, owner);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccountDetails(String accountNumber, String ownerEmail) {
        BankAccount account = findAccountForOwner(accountNumber, ownerEmail);
        return toResponse(account);
    }

    @Transactional(readOnly = true)
    public BigDecimal getBalance(String accountNumber, String ownerEmail) {
        BankAccount account = findAccountForOwner(accountNumber, ownerEmail);
        return account.getBalance();
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> getAllAccounts(String ownerEmail) {
        return bankAccountRepository.findByOwnerEmail(ownerEmail)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public AccountResponse deposit(String accountNumber, BigDecimal amount, String ownerEmail, String currency, String description, String remoteAddress) {
        validateAmount(amount);
        BankAccount account = findAccountForOwner(accountNumber, ownerEmail);
        validateCurrency(account.getCurrency(), currency);
        account.setBalance(account.getBalance().add(amount));

        TransactionRecord transaction = buildTransaction(TransactionType.DEPOSIT, amount, account, null, ownerEmail,
                description == null ? "Deposit to account" : description);
        transactionRepository.save(transaction);
        BankAccount saved = bankAccountRepository.save(account);
        auditLogService.logEvent(AuditEventType.DEPOSIT, "Deposit of " + amount + " to account " + accountNumber, remoteAddress, account.getOwner());
        
        emailService.sendTransactionAlert(account.getOwner().getEmail(), account.getOwner().getFirstName(), accountNumber, "DEPOSIT", amount, currency);
        
        return toResponse(saved);
    }

    @Transactional
    public AccountResponse withdraw(String accountNumber, BigDecimal amount, String ownerEmail, String currency, String description, String remoteAddress) {
        validateAmount(amount);
        BankAccount account = findAccountForOwner(accountNumber, ownerEmail);
        validateCurrency(account.getCurrency(), currency);
        if (account.getBalance().compareTo(amount) < 0) {
            throw new ApiException("Insufficient balance for withdrawal");
        }
        account.setBalance(account.getBalance().subtract(amount));

        TransactionRecord transaction = buildTransaction(TransactionType.WITHDRAWAL, amount, account, null, ownerEmail,
                description == null ? "Withdrawal from account" : description);
        transactionRepository.save(transaction);
        BankAccount saved = bankAccountRepository.save(account);
        auditLogService.logEvent(AuditEventType.WITHDRAWAL, "Withdrawal of " + amount + " from account " + accountNumber, remoteAddress, account.getOwner());
        
        emailService.sendTransactionAlert(account.getOwner().getEmail(), account.getOwner().getFirstName(), accountNumber, "WITHDRAWAL", amount, currency);
        
        return toResponse(saved);
    }

    private BankAccount findAccountForOwner(String accountNumber, String ownerEmail) {
        BankAccount account = bankAccountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ApiException("Bank account not found"));
        if (!account.getOwner().getEmail().equalsIgnoreCase(ownerEmail)) {
            throw new ApiException("Unauthorized access to bank account");
        }
        if (!account.isActive()) {
            throw new ApiException("Bank account is not active");
        }
        return account;
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new ApiException("User not found"));
    }

    private String createUniqueAccountNumber() {
        String accountNumber;
        int attempts = 0;
        do {
            if (attempts++ > 10) {
                throw new ApiException("Unable to generate unique account number");
            }
            accountNumber = AccountNumberGenerator.generate();
        } while (bankAccountRepository.findByAccountNumber(accountNumber).isPresent());
        return accountNumber;
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiException("Amount must be greater than zero");
        }
    }

    private void validateCurrency(String accountCurrency, String requestCurrency) {
        if (requestCurrency == null || !accountCurrency.equalsIgnoreCase(requestCurrency)) {
            throw new ApiException("Currency mismatch with account currency");
        }
    }

    private AccountResponse toResponse(BankAccount account) {
        return new AccountResponse(
                account.getId(),
                account.getAccountNumber(),
                account.getAccountType(),
                account.getCurrency(),
                account.getBalance(),
                account.isActive(),
                account.getCreatedAt()
        );
    }

    private void auditAccountCreated(User owner, BankAccount account) {
        // no-op for step 5; audit logs may be added in later steps
    }

    private TransactionRecord buildTransaction(TransactionType type,
                                               java.math.BigDecimal amount,
                                               BankAccount source,
                                               BankAccount destination,
                                               String initiatorEmail,
                                               String description) {
        TransactionRecord transaction = new TransactionRecord();
        transaction.setTransactionType(type);
        transaction.setAmount(amount);
        transaction.setCurrency(source != null ? source.getCurrency() : (destination != null ? destination.getCurrency() : null));
        transaction.setDescription(description);
        transaction.setSourceAccount(source);
        transaction.setDestinationAccount(destination);
        transaction.setInitiatedBy(findUserByEmail(initiatorEmail));
        return transaction;
    }
}
