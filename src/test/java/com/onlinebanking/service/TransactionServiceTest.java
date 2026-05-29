package com.onlinebanking.service;

import com.onlinebanking.dto.TransactionRequest;
import com.onlinebanking.entity.AuditEventType;
import com.onlinebanking.entity.BankAccount;
import com.onlinebanking.entity.TransactionRecord;
import com.onlinebanking.entity.TransactionType;
import com.onlinebanking.exception.ApiException;
import com.onlinebanking.repository.AuditLogRepository;
import com.onlinebanking.repository.BankAccountRepository;
import com.onlinebanking.repository.TransactionRepository;
import com.onlinebanking.repository.UserRepository;
import com.onlinebanking.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class TransactionServiceTest {

    @Mock
    private BankAccountRepository bankAccountRepository;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private AuditLogRepository auditLogRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private EmailService emailService;

    private AuditLogService auditLogService;
    private TransactionService transactionService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        auditLogService = new AuditLogService(auditLogRepository, userRepository);
        transactionService = new TransactionService(bankAccountRepository, transactionRepository, auditLogService, emailService);
    }

    @Test
    void transferFunds_successfullyProcessesTransfer() {
        BankAccount source = new BankAccount();
        source.setAccountNumber("SRC123");
        source.setBalance(BigDecimal.valueOf(100));
        source.setCurrency("USD");
        source.setActive(true);
        source.setOwner(new com.onlinebanking.entity.User());
        source.getOwner().setId(1L);
        source.getOwner().setEmail("customer@example.com");

        BankAccount destination = new BankAccount();
        destination.setAccountNumber("DEST456");
        destination.setBalance(BigDecimal.valueOf(50));
        destination.setCurrency("USD");
        destination.setActive(true);
        destination.setOwner(new com.onlinebanking.entity.User());
        destination.getOwner().setId(2L);
        destination.getOwner().setEmail("recipient@example.com");

        when(bankAccountRepository.findByAccountNumber("SRC123")).thenReturn(Optional.of(source));
        when(bankAccountRepository.findByAccountNumber("DEST456")).thenReturn(Optional.of(destination));
        when(transactionRepository.save(any(TransactionRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TransactionRequest request = new TransactionRequest("SRC123", "DEST456", BigDecimal.valueOf(25), "USD", "Payment");
        var response = transactionService.transferFunds(request, "customer@example.com", "127.0.0.1");

        assertThat(response).isNotNull();
        assertThat(response.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(25));
        assertThat(source.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(75));
        assertThat(destination.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(75));
    }

    @Test
    void transferFunds_sameSourceAndDestination_throwsApiException() {
        TransactionRequest request = new TransactionRequest("ACC1", "ACC1", BigDecimal.valueOf(10), "USD", "Duplicate");
        assertThatThrownBy(() -> transactionService.transferFunds(request, "customer@example.com", "127.0.0.1"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("different");
    }
}
