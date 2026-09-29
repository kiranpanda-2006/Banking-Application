package com.banking.client;

import com.banking.dto.AccountDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "account-service",url = "${account.service.url}")
public interface AccountserviceClient {

    @GetMapping("/api/internal/account/type")
    String[] getAccountTypes();

    @PostMapping("/api/v1/account")
    void createAccount(
            @RequestBody AccountDto accountDto
    );
}
