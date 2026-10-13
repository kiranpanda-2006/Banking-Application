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
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.Banner;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import org.springframework.http.MediaType;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
@Slf4j
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
            User user = userRepository.findByEmail(authentication.getName())
                    .orElseThrow(() ->
                            new UsernameNotFoundException("No user found."));

            AccountResponseDto account =
                    accountserviceClient.findAccountByUserId(user.getId());

            dto.setSenderAccountNumber(account.getAccountNumber());
            dto.setUserId(user.getId());

            ResponseEntity<TransactionResponseDto> response =
                    transferServiceClient.transfer(dto);

            TransactionResponseDto transaction = response.getBody();

            if (transaction == null || transaction.getId() == null) {
                throw new IllegalStateException("Transaction response is empty");
            }

            String transactionId = transaction.getId();

            // Wait for asynchronous Kafka processing to update the status.
            TransactionResponseDto latestTransaction = transaction;

            long deadline = System.currentTimeMillis() + 10_000;

            while (System.currentTimeMillis() < deadline) {

                ResponseEntity<TransactionResponseDto> latestResponse =
                        transferServiceClient.getTransaction(transactionId);

                if (latestResponse.getBody() != null) {
                    latestTransaction = latestResponse.getBody();
                }

                String status = latestTransaction.getStatus();

                if ("PENDING_VERIFICATION".equals(status)
                        || "COMPLETED".equals(status)
                        || "FAILED".equals(status)
                        || "CANCELLED".equals(status)) {
                    break;
                }

                Thread.sleep(500);
            }

            model.addAttribute("transaction", latestTransaction);
            model.addAttribute("transactionId", transactionId);

            System.out.println(latestTransaction.getStatus());
            return switch (latestTransaction.getStatus()) {
                case "COMPLETED", "FAILED", "CANCELLED" -> "banking/success/TransactionSuccess";
                case "PENDING_VERIFICATION" -> "redirect:/account/verify-otp?transactionId="
                        +latestTransaction.getId();
                default -> "banking/transactionProcessing";
            };

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            model.addAttribute("errorMessage",
                    "Transaction status check was interrupted.");
            return "banking/transactionProcessing";

        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("errorMessage",
                    "Transfer failed. Please try again.");
            return "banking/transferMoney";
        }
    }

    @GetMapping("/verify-otp")
    public String showVerifyOtp(
            @RequestParam(value = "transactionId", required = false)
            String transactionId,
            Model model) {

        if (transactionId == null || transactionId.isBlank()) {
            return "redirect:/account/transfer";
        }
        System.out.println(transactionId);
        model.addAttribute("transactionId", transactionId);
        return "banking/verifyOtp";
    }

//    @PostMapping(
//            value = "/verify-otp",
//            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE
//    )
//    public Mono<String> verifyOtp(ServerWebExchange exchange) {
//
//        return exchange.getFormData().map(formData -> {
//            System.out.println("FORM DATA: " + formData);
//            System.out.println("transactionId: "
//                    + formData.getFirst("transactionId"));
//            System.out.println("OTP present: "
//                    + (formData.getFirst("otp") != null));
//
//            return "banking/verifyOtp";
//        });
//    }

    @PostMapping(
            value = "/verify-otp",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE
    )
    public Mono<String> verifyOtp(ServerWebExchange exchange, Model model) {

        return exchange.getFormData().flatMap(formData -> {

            String transactionId = formData.getFirst("transactionId");
            String otp = formData.getFirst("otp");

            if (transactionId == null || transactionId.isBlank()
                    || otp == null || otp.isBlank()) {
                model.addAttribute("transactionId", transactionId);
                model.addAttribute("errorMessage", "Transaction ID and OTP are required.");
                return Mono.just("banking/verifyOtp");
            }

            return Mono.fromCallable(() ->
                            transferServiceClient.verifyOtp(transactionId, otp))
                    .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic())
                    .map(response -> {
                        TransactionResponseDto transaction = response.getBody();

                        if (transaction == null) {
                            model.addAttribute("transactionId", transactionId);
                            model.addAttribute("errorMessage",
                                    "Empty response from transaction service.");
                            return "banking/verifyOtp";
                        }

                        model.addAttribute("transaction", transaction);
//                        model.addAttribute("transaction", transactionId);

                        // Check the transaction status separately if AccountResponseDto
                        // does not contain a transaction status.
                        return "banking/success/TransactionSuccess";
                    })
                    .onErrorResume(e -> {
                        e.printStackTrace();
                        model.addAttribute("transactionId", transactionId);
                        model.addAttribute("errorMessage",
                                "Unable to verify OTP. Please try again.");
                        return Mono.just("banking/verifyOtp");
                    });
        });
    }
//    @PostMapping("/verify-otp")
//    public String verifyOtp(
//            @RequestParam("transactionId") String transactionId,
//            @RequestParam("otp") String otp,
//            Model model) {
//
//        System.out.println("VERIFY OTP CONTROLLER CALLED");
//        System.out.println("Transaction ID: " + transactionId);
//        System.out.println("OTP received: " + (otp != null && !otp.isBlank()));
//
//        try {
//            ResponseEntity<TransactionResponseDto> response =
//                    transferServiceClient.verifyOtp(transactionId, otp);
//
//            TransactionResponseDto transaction = response.getBody();
//
//            if (transaction == null) {
//                throw new IllegalStateException(
//                        "Transaction service returned an empty response");
//            }
//
//            String status = transaction.getStatus();
//
//            System.out.println("Transaction status after OTP verification: "
//                    + status);
//
//            model.addAttribute("transaction", transaction);
//            model.addAttribute("transactionId", transactionId);
//
//            switch (status) {
//                case "COMPLETED":
//                    return "banking/success/TransactionSuccess";
//
//                case "FAILED":
//                case "CANCELLED":
//                case "FLAGGED":
//                    return "banking/success/TransactionSuccess";
//
//                case "PENDING_VERIFICATION":
//                    model.addAttribute("errorMessage",
//                            "Invalid OTP. Please try again.");
//                    return "banking/verifyOtp";
//
//                default:
//                    return "banking/transactionProcessing";
//            }
//
//        } catch (Exception e) {
//            e.printStackTrace();
//
//            model.addAttribute("transactionId", transactionId);
//            model.addAttribute("errorMessage",
//                    "Unable to verify OTP. Please try again.");
//
//            return "banking/verifyOtp";
//        }
//    }
}
