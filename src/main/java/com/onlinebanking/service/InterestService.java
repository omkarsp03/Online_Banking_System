package com.onlinebanking.service;

import com.onlinebanking.entity.AccountType;
import com.onlinebanking.entity.AuditEventType;
import com.onlinebanking.entity.BankAccount;
import com.onlinebanking.entity.TransactionRecord;
import com.onlinebanking.entity.TransactionType;
import com.onlinebanking.repository.BankAccountRepository;
import com.onlinebanking.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class InterestService {

    private final BankAccountRepository bankAccountRepository;
    private final TransactionRepository transactionRepository;
    private final AuditLogService auditLogService;

    // Daily at 2:00 AM
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void calculateDailyInterest() {
        log.info("Starting daily interest calculation for SAVINGS accounts");
        List<BankAccount> savingsAccounts = bankAccountRepository.findByAccountTypeAndActive(AccountType.SAVINGS, true);
        
        // APY of 2%
        BigDecimal apy = new BigDecimal("0.02");
        BigDecimal daysInYear = new BigDecimal("365");
        BigDecimal dailyRate = apy.divide(daysInYear, 8, RoundingMode.HALF_UP);

        int count = 0;
        for (BankAccount account : savingsAccounts) {
            BigDecimal balance = account.getBalance();
            if (balance.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            BigDecimal interest = balance.multiply(dailyRate).setScale(4, RoundingMode.HALF_UP);
            if (interest.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            // Update balance
            account.setBalance(balance.add(interest));
            account.setLastInterestDate(Instant.now());
            bankAccountRepository.save(account);

            // Record transaction
            TransactionRecord record = new TransactionRecord();
            record.setTransactionType(TransactionType.INTEREST);
            record.setAmount(interest);
            record.setCurrency(account.getCurrency());
            record.setDescription("Daily interest payment (2% APY)");
            record.setDestinationAccount(account);
            record.setInitiatedBy(account.getOwner());
            transactionRepository.save(record);

            // Log event
            auditLogService.logEvent(
                    AuditEventType.DEPOSIT, // using DEPOSIT or general audit type since interest credits to the account
                    "Credited daily interest of " + interest + " " + account.getCurrency() + " to account " + account.getAccountNumber(),
                    "SYSTEM",
                    account.getOwner()
            );

            count++;
        }
        log.info("Completed daily interest calculation. Processed {} accounts.", count);
    }
}
