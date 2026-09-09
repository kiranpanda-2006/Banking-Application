package com.banking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransferRequest {

    @NotBlank(message = "Senders account number is required.")
    private String senderAccountNumber;
    @NotBlank(message = "Receiver account number is required")
    private String receiverAccountNumber;
    @NotNull(message = "transfer amount required.")
    @Positive(message = "transferring amount must be positive")
    private BigDecimal amount;

    private String description;
}
