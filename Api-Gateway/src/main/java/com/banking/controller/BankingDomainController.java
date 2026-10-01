package com.banking.controller;

import com.banking.client.AccountserviceClient;
import com.banking.dto.AccountDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/account")
@RequiredArgsConstructor
public class BankingDomainController {

    private final AccountserviceClient accountserviceClient;

    @GetMapping("/create")
    public String createAccount(Model model){
        model.addAttribute("accountDto", new AccountDto());
        model.addAttribute("accountTypes", accountserviceClient.getAccountTypes());
        return "banking/createAccount";
    }
}
