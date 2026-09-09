package com.banking.repository;

import com.banking.dto.TransactionResponse;
import com.banking.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TransactionRepo extends JpaRepository<Transaction,String> {
    Optional<Transaction> findByTransactionId(String transactionId);

    Optional<List<Transaction>> findBySenderAccountNumberOrderByCreatedAtDesc(String accountNumber);
}
