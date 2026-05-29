package com.onlinebanking.repository;

import com.onlinebanking.entity.TransactionRecord;
import com.onlinebanking.entity.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TransactionRepository extends JpaRepository<TransactionRecord, Long> {
    List<TransactionRecord> findBySourceAccountIdOrDestinationAccountId(Long sourceAccountId, Long destinationAccountId);
    Page<TransactionRecord> findBySourceAccountIdOrDestinationAccountId(Long sourceAccountId, Long destinationAccountId, Pageable pageable);
    
    @Query("SELECT t FROM TransactionRecord t WHERE t.initiatedBy.email = :email ORDER BY t.createdAt DESC")
    List<TransactionRecord> findByInitiatedByEmail(@Param("email") String email);

    @Query("SELECT t FROM TransactionRecord t WHERE t.initiatedBy.email = :email")
    Page<TransactionRecord> findByInitiatedByEmail(@Param("email") String email, Pageable pageable);

    @Query("SELECT t FROM TransactionRecord t WHERE " +
           "(:accountNumber IS NULL OR :accountNumber = '' OR t.sourceAccount.accountNumber = :accountNumber OR t.destinationAccount.accountNumber = :accountNumber) " +
           "AND (:transactionType IS NULL OR t.transactionType = :transactionType)")
    Page<TransactionRecord> findAllFiltered(@Param("accountNumber") String accountNumber, 
                                           @Param("transactionType") TransactionType transactionType, 
                                           Pageable pageable);
}
