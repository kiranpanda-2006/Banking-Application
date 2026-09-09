package com.banking.controller;


import com.banking.dto.AccountResponse;
import com.banking.dto.CreateAccountRequest;
import com.banking.exception.DuplicateResourceException;
import com.banking.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/account")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(
            @Valid @RequestBody CreateAccountRequest request
            ) throws DuplicateResourceException {


        return ResponseEntity.status(HttpStatus.CREATED)
                .body(accountService.createAccount(request));
    }

    @GetMapping("/{accountNumber}")
    public ResponseEntity<AccountResponse> getAccount(
            @PathVariable String accountNumber
    ){


        return ResponseEntity.status(HttpStatus.OK)
                .body(accountService.getAccount(accountNumber));
    }

    @GetMapping("/{accountNumber}/balance")
    public ResponseEntity<BigDecimal> getBalance(
            @PathVariable String accountNumber
    ){


        return ResponseEntity.status(HttpStatus.OK)
                .body(accountService.getBalance(accountNumber));
    }


    @PutMapping("/{accountNumber}/block")
    public ResponseEntity<String> blockAccount(
            @PathVariable String accountNumber
    ){

        accountService.blockAccount(accountNumber);
        return ResponseEntity.status(HttpStatus.OK)
                .body("Account Blocked "+accountNumber);
    }


    /*
    * SAGA STEP-1 - Deduct balance
    * called by transaction service and transfer is initiated
    */

    @PutMapping("/{accountNumber}/deduct")
    public ResponseEntity<String> deductBalance(
            @PathVariable String accountNumber,
            @RequestParam BigDecimal amount
    ){

        accountService.deductBalance(accountNumber,amount);
        return ResponseEntity.status(HttpStatus.OK)
                .body(amount+" Successfully deducted from  "+accountNumber);
    }

    /*
    * SAGA Step=4 - Compensating transaction endpoint
    * call transaction service in two scenarios
    * 1. if fraud detect refund to sender's account
    * 2. if everything ok then deduct money from sender and credit into receivers account*/

    @PutMapping("/{accountNumber}/credit")
    public ResponseEntity<String> creditBalance(
            @PathVariable String accountNumber,
            @RequestParam BigDecimal amount
    ){

        accountService.creditBalance(accountNumber,amount);
        return ResponseEntity.status(HttpStatus.OK)
                .body(amount+" Successfully credited to  "+accountNumber);
    }



}
