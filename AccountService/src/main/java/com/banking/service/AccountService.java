package com.banking.service;


import com.banking.dto.AccountResponse;
import com.banking.dto.CreateAccountRequest;
import com.banking.entity.Account;
import com.banking.enums.AccountStatus;
import com.banking.enums.AccountType;
import com.banking.exception.ContractViolationException;
import com.banking.exception.DuplicateResourceException;
import com.banking.exception.InsufficientBalanceException;
import com.banking.exception.ResourceNotFoundException;
import com.banking.repository.AccountRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class AccountService {


    private final AccountRepository accountRepository;

    private static final SecureRandom random = new SecureRandom();


    public AccountResponse createAccount(@Valid CreateAccountRequest request) throws DuplicateResourceException {
        log.info("creating account {}", request.getEmail());

        if (accountRepository.existsByEmail(request.getEmail())){
            throw new DuplicateResourceException("Account Already exist By this email");
        }

        Account account = Account.builder()
                .accountHolderName(request.getAccountHolderName())
                .accountType(request.getAccountType())
                .email(request.getEmail())
                .phone(request.getPhone())
                .balance(request.getInitialDeposit())
                .status(AccountStatus.ACTIVE)
                .accountNumber(generateAccountNumber())
                .dailyTransactionLimit(
                        request
                                .getAccountType()
                                .equals(AccountType.SAVINGS)
                                ? new BigDecimal("100000")
                                : new BigDecimal("500000")
                        )
                .createdAt(LocalDateTime.now())
                .build();

        Account savedAccount =
                accountRepository.save(account);

        log.info("account saved {} ", savedAccount.getAccountNumber());

        return mapToResponse(savedAccount);
    }

    public AccountResponse getAccount(String accountNumber) {
        Optional<Account> accountOptional =
                accountRepository.findByAccountNumber(accountNumber);
        if (accountOptional.isEmpty()){
            throw new ResourceNotFoundException("No account exist with "+accountNumber);
        }
        Account account =
                accountOptional.get();
        return mapToResponse(account);
    }

    public BigDecimal getBalance(String accountNumber) {
        Optional<Account> accountOptional =
                accountRepository.findByAccountNumber(accountNumber);
        if (accountOptional.isEmpty()){
            throw new ResourceNotFoundException("No account exist with "+accountNumber);
        }

        return accountOptional.get().getBalance();
    }
/*
* get account by account number
* block account - called by fraud detection service via Kafka
* @param accountNumber
*/
    public void blockAccount(String accountNumber) {
        log.info("blocking account {}",accountNumber);

        Optional<Account> accountOptional =
                accountRepository.findByAccountNumber(accountNumber);
        if (accountOptional.isEmpty()){
            throw new ResourceNotFoundException("No account exist with "+accountNumber);
        }

        Account account =
                accountOptional.get();
        account.setStatus(AccountStatus.BLOCKED);

        accountRepository.save(account);

        log.info("account blocked {}",accountNumber);
    }

    /*
    *Deduct balance from senders account
    * called by transaction service
    * @param accountNumber
    * @Param amount*/

    public void deductBalance(String accountNumber, BigDecimal amount) {
        log.info("deducting {} from {}",amount,accountNumber);

        Optional<Account> accountOptional =
                accountRepository.findByAccountNumber(accountNumber);
        if (accountOptional.isEmpty()){
            throw new ResourceNotFoundException("No account exist with "+accountNumber);
        }
        Account account =
                accountOptional.get();
        if (account.getStatus() != AccountStatus.ACTIVE){
            throw new ContractViolationException("Account Blocked for some time due to detect Unusual Activity.");
        }

        if (account.getBalance().compareTo(amount) < 0){
            throw new InsufficientBalanceException("Insufficient Balance "+account.getBalance()+" in "+account.getAccountNumber());
        }

        account.setBalance(account.getBalance().subtract(amount));
        accountRepository.save(account);

        log.info("Balance Update, new Balance: {}",account.getBalance());

    }
/*
*credit balance to the account
* called By - Transaction service
* @Param accountNumber
* @Param amount*/
    public void creditBalance(String accountNumber, BigDecimal amount) {
        log.info("crediting {} to {}",amount,accountNumber);

        Optional<Account> accountOptional =
                accountRepository.findByAccountNumber(accountNumber);
        if (accountOptional.isEmpty()){
            throw new ResourceNotFoundException("No account exist with "+accountNumber);
        }
        Account account =
                accountOptional.get();
        if (account.getStatus() != AccountStatus.ACTIVE){
            throw new ContractViolationException("Account Blocked for some time due to detect Unusual Activity.");
        }

        account.setBalance(account.getBalance().add(amount));
        accountRepository.save(account);

        log.info("{} credited successfully to {} , new Balance: {} ",amount,accountNumber,account.getBalance());

    }



//    helper methods are here

    private AccountResponse mapToResponse(Account savedAccount) {
        return AccountResponse.builder()
                .id(savedAccount.getId())
                .accountNumber(savedAccount.getAccountNumber())
                .accountHolderName(savedAccount.getAccountHolderName())
                .email(savedAccount.getEmail())
                .phone(savedAccount.getPhone())
                .accountType(savedAccount.getAccountType())
                .status(savedAccount.getStatus())
                .balance(savedAccount.getBalance())
                .dailyTransactionLimit(savedAccount.getDailyTransactionLimit())
                .createdAt(savedAccount.getCreatedAt())
                .build();
    }

//generating unique 12 digit accountNumber can take time run many database queries.
    private String generateAccountNumber() {
        String accountNumber;
        do {
            Long number = random.nextLong(1_000_000_000_000L);

            accountNumber = String.format("%012d",number);
        }while (accountRepository.existsByAccountNumber(accountNumber));

        return accountNumber;
    }
}
