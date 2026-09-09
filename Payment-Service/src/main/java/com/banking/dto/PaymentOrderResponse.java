package com.banking.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentOrderResponse {


    private String razorPayOrderId;
    private String paymentId;
    private BigDecimal amount;
    private String currency;
    private String status;
    private String razorPayKeyId;
}
