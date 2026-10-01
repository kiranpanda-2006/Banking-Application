package com.banking.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "account-service",url = "${account.service.url}")
public interface AccountserviceClient {

    @GetMapping("/api/internal/account/type")
    String[] getAccountTypes();

}
