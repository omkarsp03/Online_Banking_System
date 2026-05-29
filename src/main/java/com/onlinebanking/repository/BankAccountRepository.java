package com.onlinebanking.repository;

import com.onlinebanking.entity.AccountType;
import com.onlinebanking.entity.BankAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BankAccountRepository extends JpaRepository<BankAccount, Long> {
    Optional<BankAccount> findByAccountNumber(String accountNumber);
    List<BankAccount> findByOwnerEmail(String ownerEmail);
    List<BankAccount> findByAccountTypeAndActive(AccountType accountType, boolean active);
}
