package com.banking.client;

import com.banking.dto.TransactionResponseDto;
import com.banking.dto.TransferReqDto;
import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "transaction-service",url = "${transaction.service.url}")
public interface TransferServiceClient {



    @PostMapping("/api/v1/transaction/transfer")
    ResponseEntity<TransactionResponseDto> transfer(
            @Valid @RequestBody TransferReqDto request
    );

    @GetMapping("/api/v1/transaction/{transactionId}")
     ResponseEntity<TransactionResponseDto> getTransaction(
            @PathVariable("transactionId") String transactionId
    );

    @PostMapping("/api/v1/transaction/{transactionId}/verify")
     ResponseEntity<TransactionResponseDto> verifyOtp(
            @PathVariable("transactionId") String transactionId,
            @RequestParam String otp
    );
}
