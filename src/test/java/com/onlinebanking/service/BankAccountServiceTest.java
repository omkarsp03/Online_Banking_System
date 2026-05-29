package com.onlinebanking.service;

import com.onlinebanking.dto.AccountResponse;
import com.onlinebanking.dto.CreateAccountRequest;
import com.onlinebanking.entity.AuditEventType;
import com.onlinebanking.entity.BankAccount;
import com.onlinebanking.entity.Role;
import com.onlinebanking.entity.RoleName;
import com.onlinebanking.entity.User;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class BankAccountServiceTest {

    @Mock
    private BankAccountRepository bankAccountRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private AuditLogRepository auditLogRepository;
    @Mock
    private EmailService emailService;

    private AuditLogService auditLogService;
    private BankAccountService bankAccountService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        auditLogService = new AuditLogService(auditLogRepository, userRepository);
        bankAccountService = new BankAccountService(bankAccountRepository, userRepository, transactionRepository, auditLogService, emailService);
    }

    @Test
    void createAccount_savesAccountAndReturnsResponse() {
        User owner = new User();
        owner.setEmail("customer@example.com");
        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(owner));
        when(bankAccountRepository.findByAccountNumber(any())).thenReturn(Optional.empty());
        when(bankAccountRepository.save(any(BankAccount.class))).thenAnswer(invocation -> {
            BankAccount account = invocation.getArgument(0);
            account.setId(10L);
            return account;
        });

        CreateAccountRequest request = new CreateAccountRequest();
        request.setAccountType(com.onlinebanking.entity.AccountType.CHECKING);
        request.setCurrency("USD");

        AccountResponse response = bankAccountService.createAccount("customer@example.com", request, "127.0.0.1");

        assertThat(response).isNotNull();
        assertThat(response.getAccountNumber()).isNotBlank();
        assertThat(response.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void deposit_withInvalidCurrency_throwsApiException() {
        BankAccount account = new BankAccount();
        account.setAccountNumber("123");
        account.setOwner(new User());
        account.setCurrency("USD");
        account.setBalance(BigDecimal.valueOf(100));
        account.setActive(true);
        account.getOwner().setEmail("customer@example.com");

        when(bankAccountRepository.findByAccountNumber("123")).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> bankAccountService.deposit("123", BigDecimal.valueOf(10), "customer@example.com", "EUR", "Top-up", "127.0.0.1"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Currency mismatch");
    }

    @Test
    void withdraw_withInsufficientBalance_throwsApiException() {
        BankAccount account = new BankAccount();
        account.setAccountNumber("123");
        account.setOwner(new User());
        account.setCurrency("USD");
        account.setBalance(BigDecimal.valueOf(10));
        account.setActive(true);
        account.getOwner().setEmail("customer@example.com");

        when(bankAccountRepository.findByAccountNumber("123")).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> bankAccountService.withdraw("123", BigDecimal.valueOf(20), "customer@example.com", "USD", "Payout", "127.0.0.1"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Insufficient balance");
    }
}
