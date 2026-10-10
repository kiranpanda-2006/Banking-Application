package com.banking.client;

import com.banking.dto.TransactionResponseDto;
import com.banking.dto.TransferReqDto;
import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "transaction-service",url = "${transaction.service.url}")
public interface TransferServiceClient {



    @PostMapping("/api/v1/transaction/transfer")
    ResponseEntity<TransactionResponseDto> transfer(
            @Valid @RequestBody TransferReqDto request
    );
}
