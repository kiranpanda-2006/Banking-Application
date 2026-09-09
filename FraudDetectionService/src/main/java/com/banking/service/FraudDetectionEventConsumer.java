package com.banking.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.Map;

/*
* consume the initiate transaction from transaction service
* check is fraud detected or not
* if detected : -> revert the transaction
* otherwise complete the transaction .*/
@Service
@Slf4j
@RequiredArgsConstructor
public class FraudDetectionEventConsumer {

    private final FraudDetectionService fraudDetectionService;

    /**
     * check every transaction.initiated topic
     * every transaction goes throw fraud check before completion
     * @param payload
     */
    @KafkaListener(topics = "transaction.initiated", groupId = "fraud-detection-group")
    public void consumeTransactionInitiated(
            @Payload Map<String,Object> payload
            ){
        log.info("Received Transaction for fraudCheck: {}",payload.get("TransactionId"));

        try{
            fraudDetectionService.checkTransaction(payload);
        } catch (Exception e) {
            log.info("some ");
        }
    }
}
