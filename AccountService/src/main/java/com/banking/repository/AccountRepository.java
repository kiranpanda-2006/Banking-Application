package com.banking.repository;


import com.banking.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account,String> {


    boolean existsByEmail(String email);

    boolean existsByAccountNumber(String accountNumber);

    boolean existsByUserId(Long userId);

    Optional<Account> findByAccountNumber(String accountNumber);
}
