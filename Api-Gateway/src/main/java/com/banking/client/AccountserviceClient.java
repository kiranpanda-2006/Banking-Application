package com.banking.client;

import com.banking.dto.AccountDto;
import com.banking.dto.AccountResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@FeignClient(name = "account-service",url = "${account.service.url}")
public interface AccountserviceClient {

    @GetMapping("/api/internal/account/type")
    String[] getAccountTypes();


    @GetMapping("/api/internal/account/status")
    String[] getAccountStatus();

    @PostMapping("/api/v1/account")
    ResponseEntity<AccountResponseDto> createAccount(
            @RequestBody AccountDto accountDto);

    @GetMapping("/api/v1/account/{accountNumber}")
    AccountResponseDto getAccountByAccountNumber(
            @RequestParam("accountNumber") String accountNumber);

    @GetMapping("/api/v1/account/{accountNumber}/balance")
    BigDecimal getBalance
            (@RequestParam("accountNumber") String accountNumber);

//    @PutMapping("/{accountNumber}/credit")
}
