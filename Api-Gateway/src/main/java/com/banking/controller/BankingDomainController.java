package com.banking.controller;

import com.banking.client.AccountserviceClient;
import com.banking.dto.AccountDto;
import com.banking.dto.AccountResponseDto;
import com.banking.entity.User;
import com.banking.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@Controller
@RequestMapping("/account")
@RequiredArgsConstructor
public class BankingDomainController {

    private final AccountserviceClient accountserviceClient;

    private final UserRepository userRepository;

    @GetMapping("/create")
    public String createAccount(Model model){
        model.addAttribute("accountDto", new AccountDto());
        model.addAttribute("accountTypes", accountserviceClient.getAccountTypes());
        return "banking/createAccount";
    }

    @PostMapping("/create")
    public String saveAccount(
            @Valid @ModelAttribute("accountDto") AccountDto accountDto,
            BindingResult result,
            Authentication authentication,
            Model model
    ) {

        if (result.hasErrors()) {
            model.addAttribute("accountTypes",
                    accountserviceClient.getAccountTypes());

            return "banking/createAccount";
        }

        System.out.println("1. Form submitted successfully");

        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new UsernameNotFoundException("User not found.")
                );

        ResponseEntity<AccountResponseDto> response =
                accountserviceClient.createAccount(
                        accountDto,
                        user.getId()
                );

        AccountResponseDto account = response.getBody();

        System.out.println("2. HTTP Status: " + response.getStatusCode());
        System.out.println("3. Response Body: " + response.getBody());

        if (account == null) {
            throw new RuntimeException("Account creation returned empty response");
        }

        System.out.println("4. AccountNumber: " + account.getAccountNumber());

        model.addAttribute("account", account);

        return "banking/success/accountSuccess";
    }
//    work in the next session
    @GetMapping("/details")
    public String getAccountDetailsPage(){
            return "banking/accountDetails";
        }

        @GetMapping("/get/details")
        public String getAccountDetails(
                @RequestParam(required = false) String accountNumber,
                Model model){

            if (accountNumber == null || accountNumber.isBlank()){
                return "banking/accountDetails";
            }

       AccountResponseDto response =
               accountserviceClient.getAccountByAccountNumber(accountNumber);
        model.addAttribute("account",response);
        return "banking/accountDetails";
    }

    @GetMapping("/balance")
    public String checkBalance(){
        return "banking/CheckBalance";
    }

    @GetMapping("/check-balance")
    public String checkBalance(
            @RequestParam(required = false) String accountNumber,
            Model model) {

        if (accountNumber == null || accountNumber.isBlank()) {
            return "banking/checkBalance";
        }

        BigDecimal balance =
                accountserviceClient.getBalance(accountNumber);

        model.addAttribute("balance", balance);
        model.addAttribute("accountNumber", accountNumber);

        return "banking/checkBalance";
    }
}
