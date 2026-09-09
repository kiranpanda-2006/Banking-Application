package com.banking.contraller;

import com.banking.Service.TransactionService;
import com.banking.dto.TransactionResponse;
import com.banking.dto.TransferRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/transaction")
@Slf4j
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping("/transfer")
    public ResponseEntity<TransactionResponse> transfer(
            @Valid @RequestBody TransferRequest request
            ){
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transactionService.transfer(request));
    }

    @GetMapping("/{transactionId}")
    public ResponseEntity<TransactionResponse> getTransaction(
            @RequestParam String transactionId
    ){
        return ResponseEntity.status(HttpStatus.OK)
                .body(transactionService.getTransaction(transactionId));
    }

    @GetMapping("/account/{accountNumber}")
    public ResponseEntity<List<TransactionResponse>> getTransactionHistory(
            @RequestParam String accountNumber
    ){
        return ResponseEntity.status(HttpStatus.OK)
                .body(transactionService.getTransactionHistory(accountNumber));
    }

    @PostMapping("/{transactionId}/verify")
    public ResponseEntity<TransactionResponse> verifyOtp(
            @PathVariable String transactionId,
            @RequestParam String otp
    ){
        log.info("OTP verification request - transaction : {}", transactionId);
        return ResponseEntity.status(HttpStatus.OK)
                .body(transactionService.verifyOtp(transactionId,otp));
    }


}
