package com.banking.repository;


import com.banking.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account,String> {


    boolean existByEmail( String email);

    boolean existByAccountNumber(String accountNumber);

    Optional<Account> findByAccountNumber(String accountNumber);
}
