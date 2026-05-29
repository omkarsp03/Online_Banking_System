package com.onlinebanking.service;

import com.onlinebanking.dto.AdminAccountResponse;
import com.onlinebanking.dto.AuditLogResponse;
import com.onlinebanking.dto.TransactionResponse;
import com.onlinebanking.dto.UserResponse;
import com.onlinebanking.entity.AuditLog;
import com.onlinebanking.entity.BankAccount;
import com.onlinebanking.entity.RoleName;
import com.onlinebanking.entity.TransactionRecord;
import com.onlinebanking.entity.TransactionType;
import com.onlinebanking.entity.User;
import com.onlinebanking.exception.ApiException;
import com.onlinebanking.repository.AuditLogRepository;
import com.onlinebanking.repository.BankAccountRepository;
import com.onlinebanking.repository.RoleRepository;
import com.onlinebanking.repository.TransactionRepository;
import com.onlinebanking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final BankAccountRepository bankAccountRepository;
    private final TransactionRepository transactionRepository;
    private final AuditLogRepository auditLogRepository;

    @Transactional(readOnly = true)
    public Page<UserResponse> listUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(this::toUserResponse);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long userId) {
        return userRepository.findById(userId)
                .map(this::toUserResponse)
                .orElseThrow(() -> new ApiException("User not found"));
    }

    @Transactional(readOnly = true)
    public Page<AdminAccountResponse> listAccounts(Pageable pageable) {
        return bankAccountRepository.findAll(pageable).map(this::toAccountResponse);
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> listTransactions(String accountNumber, String transactionType, Pageable pageable) {
        TransactionType typeEnum = null;
        if (transactionType != null && !transactionType.isBlank()) {
            try {
                typeEnum = TransactionType.valueOf(transactionType.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                return Page.empty(pageable);
            }
        }
        return transactionRepository.findAllFiltered(
                accountNumber != null && !accountNumber.isBlank() ? accountNumber.trim() : null,
                typeEnum,
                pageable
        ).map(this::toTransactionResponse);
    }

    @Transactional
    public UserResponse changeUserRole(Long userId, RoleName roleName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found"));
        var role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new ApiException("Role not found: " + roleName));
        user.getRoles().clear();
        user.getRoles().add(role);
        User saved = userRepository.save(user);
        return toUserResponse(saved);
    }

    @Transactional
    public AdminAccountResponse setAccountActiveState(String accountNumber, boolean active) {
        BankAccount account = bankAccountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ApiException("Bank account not found"));
        account.setActive(active);
        BankAccount saved = bankAccountRepository.save(account);
        return toAccountResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> listAuditLogs(Pageable pageable) {
        return auditLogRepository.findAll(pageable).map(this::toAuditResponse);
    }

    private UserResponse toUserResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhone(),
                user.isEnabled(),
                user.getRoles().stream().map(role -> role.getName().name()).collect(Collectors.toSet()),
                user.getCreatedAt()
        );
    }

    private AdminAccountResponse toAccountResponse(BankAccount account) {
        return new AdminAccountResponse(
                account.getId(),
                account.getAccountNumber(),
                account.getAccountType(),
                account.getCurrency(),
                account.getBalance(),
                account.isActive(),
                account.getOwner().getEmail(),
                account.getCreatedAt()
        );
    }

    private TransactionResponse toTransactionResponse(TransactionRecord record) {
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

    private AuditLogResponse toAuditResponse(AuditLog auditLog) {
        return new AuditLogResponse(
                auditLog.getId(),
                auditLog.getEventType(),
                auditLog.getEventDetails(),
                auditLog.getIpAddress(),
                auditLog.getUser() != null ? auditLog.getUser().getEmail() : null,
                auditLog.getCreatedAt()
        );
    }
}
