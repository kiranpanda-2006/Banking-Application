package com.banking.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponseDto {

    private String id;
    private String senderAccountNumber;
    private String receiverAccountNumber;
    private BigDecimal amount;
    private String type;
    private String status;
    private String description;
    private String failureReason;
    private String referenceNumber;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
}
