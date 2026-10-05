package com.banking.controller;

import com.banking.entity.Account;
import com.banking.enums.AccountStatus;
import com.banking.enums.AccountType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/internal/account")
public class ClassController {

    @GetMapping("/type")
    public AccountType[] getAccountType(){
        return AccountType.values();
    }
    @GetMapping("/status")
    public AccountStatus[] getStatus(){
        return  AccountStatus.values();
    }

}
