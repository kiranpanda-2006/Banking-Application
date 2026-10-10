package com.banking.controller;

import com.banking.client.AccountserviceClient;
import com.banking.client.TransferServiceClient;
import com.banking.dto.AccountDto;
import com.banking.dto.AccountResponseDto;
import com.banking.dto.TransactionResponseDto;
import com.banking.dto.TransferReqDto;
import com.banking.entity.User;
import com.banking.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.Banner;
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

    private final TransferServiceClient transferServiceClient;

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


    @GetMapping("/transfer")
    public String transferMoney(
            Authentication authentication,
            Model model) {

        String userName = authentication.getName();

        User user = userRepository.findByEmail(userName)
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "No user found with username."
                        ));

        Long userId = user.getId();

        AccountResponseDto account =
                accountserviceClient.findAccountByUserId(userId);

        String senderAccountNumber = account.getAccountNumber();

        model.addAttribute("senderAccountNumber", senderAccountNumber);
        TransferReqDto dto = new TransferReqDto();
        model.addAttribute("transactionDto", dto);
        return "banking/transferMoney";
    }

    @PostMapping("/transfer")
    public String processTransfer(
            @ModelAttribute("transactionDto") TransferReqDto dto,
            BindingResult result,
            Authentication authentication,
            Model model) {

        if (result.hasErrors()) {
            return "banking/transferMoney";
        }

        try {
            String userName = authentication.getName();

            User user = userRepository.findByEmail(userName)
                    .orElseThrow(() ->
                            new UsernameNotFoundException("No user found."));

            AccountResponseDto account =
                    accountserviceClient.findAccountByUserId(user.getId());

            // Sender must come from the authenticated user's account.
            dto.setSenderAccountNumber(account.getAccountNumber());
            dto.setUserId(user.getId());
            System.out.println(dto.getReceiverAccountNumber());
            System.out.println(dto.getAmount());

            ResponseEntity<TransactionResponseDto> response =
                    transferServiceClient.transfer(dto);
            model.addAttribute("transaction",response.getBody());
            return "banking/success/TransactionSuccess";

        } catch (Exception e) {
            model.addAttribute("errorMessage", "Transfer failed. Please try again.");
            System.out.println(e.getMessage());
            return "banking/transferMoney";
        }
    }
}
