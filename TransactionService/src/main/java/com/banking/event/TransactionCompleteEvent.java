package com.banking.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransactionCompleteEvent {

    private String transactionId;
    private String sendersAccountNumber;
    private String receiverAccountNumber;
    private BigDecimal amount;
    private String description;
}
