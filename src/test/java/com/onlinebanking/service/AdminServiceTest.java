package com.onlinebanking.service;

import com.onlinebanking.dto.AdminAccountResponse;
import com.onlinebanking.dto.UserResponse;
import com.onlinebanking.entity.AccountType;
import com.onlinebanking.entity.AuditLog;
import com.onlinebanking.entity.BankAccount;
import com.onlinebanking.entity.Role;
import com.onlinebanking.entity.RoleName;
import com.onlinebanking.entity.TransactionRecord;
import com.onlinebanking.entity.TransactionType;
import com.onlinebanking.entity.User;
import com.onlinebanking.repository.AuditLogRepository;
import com.onlinebanking.repository.BankAccountRepository;
import com.onlinebanking.repository.RoleRepository;
import com.onlinebanking.repository.TransactionRepository;
import com.onlinebanking.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class AdminServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private BankAccountRepository bankAccountRepository;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private AuditLogRepository auditLogRepository;

    private AdminService adminService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        adminService = new AdminService(userRepository, roleRepository, bankAccountRepository, transactionRepository, auditLogRepository);
    }

    @Test
    void changeUserRole_updatesRoleSuccessfully() {
        User user = new User();
        user.setId(10L);
        user.setEmail("test@example.com");
        Role role = new Role();
        role.setId(2L);
        role.setName(RoleName.ROLE_ADMIN);
        user.setRoles(new java.util.HashSet<>(Set.of(new Role())));

        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(roleRepository.findByName(RoleName.ROLE_ADMIN)).thenReturn(Optional.of(role));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = adminService.changeUserRole(10L, RoleName.ROLE_ADMIN);

        assertThat(response.getEmail()).isEqualTo("test@example.com");
        assertThat(response.getRoles()).containsExactly("ROLE_ADMIN");
    }

    @Test
    void setAccountActiveState_disablesAccountSuccessfully() {
        BankAccount account = new BankAccount();
        account.setAccountNumber("ABC123");
        account.setActive(true);
        account.setCurrency("USD");
        account.setBalance(BigDecimal.TEN);
        account.setOwner(new User());
        account.getOwner().setEmail("user@example.com");

        when(bankAccountRepository.findByAccountNumber("ABC123")).thenReturn(Optional.of(account));
        when(bankAccountRepository.save(any(BankAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AdminAccountResponse response = adminService.setAccountActiveState("ABC123", false);

        assertThat(response.isActive()).isFalse();
    }

    @Test
    void listTransactions_filtersByAccountNumberAndType() {
        TransactionRecord record = new TransactionRecord();
        record.setTransactionType(TransactionType.DEPOSIT);
        record.setDescription("Test deposit");
        record.setCurrency("USD");
        BankAccount source = new BankAccount();
        source.setAccountNumber("SRC123");
        record.setSourceAccount(source);
        record.setDestinationAccount(null);
        org.springframework.data.domain.Page<TransactionRecord> page = new org.springframework.data.domain.PageImpl<>(List.of(record));
        when(transactionRepository.findAllFiltered(any(), any(), any())).thenReturn(page);

        org.springframework.data.domain.Page<com.onlinebanking.dto.TransactionResponse> results = adminService.listTransactions("SRC123", "DEPOSIT", org.springframework.data.domain.PageRequest.of(0, 10));

        assertThat(results.getContent()).hasSize(1);
        assertThat(results.getContent().get(0).getTransactionType()).isEqualTo(TransactionType.DEPOSIT);
    }

    @Test
    void changeUserRole_whenUserMissing_throwsException() {
        when(userRepository.findById(123L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> adminService.changeUserRole(123L, RoleName.ROLE_ADMIN))
                .isInstanceOf(RuntimeException.class);
    }
}
