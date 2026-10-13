package com.banking.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
@Service
@Slf4j
@RequiredArgsConstructor
public class AccountEventConsumer {
    private final AccountService accountService;
    private final ObjectMapper objectMapper;
/*
* consume means no fraud detected transfer  money from one account to another
* credit in receivers account
* consume transaction completed event by Kafka*/
    @KafkaListener(topics = "transaction.completed")
    public void consumeTransactionCompleted(
            String message
            ){

        try {
            Map<String, Object> payload = objectMapper.readValue(
                    message,
                    new TypeReference<Map<String, Object>>() {}
            );
            String receiversAccount = (String) payload.get("receiverAccountNumber");
            BigDecimal amount = new BigDecimal(payload.get("amount").toString());

            log.info("crediting account: {} amount: {}",receiversAccount,amount);
            accountService.creditBalance(receiversAccount,amount);
        } catch (Exception e) {
            log.error("Error Crediting amount: {}",e.getMessage());
        }

    }

    /*
    * consume fraud.detect event from kafka
    * Blocks the flagged account
    * */
    @KafkaListener(topics = "fraud.detected")
    public void consumeFraudDetected(String message) {
        try {
            Map<String, Object> payload = objectMapper.readValue(
                    message,
                    new TypeReference<Map<String, Object>>() {}
            );

            String accountNumber =
                    String.valueOf(payload.get("accountNumber")).trim();

            log.info("Fraud detected - Blocking Account {}", accountNumber);

            accountService.blockAccount(accountNumber);

        } catch (Exception e) {
            log.error("Error blocking account", e);
            throw new RuntimeException("Failed to process fraud.detected event", e);
        }
    }


}
