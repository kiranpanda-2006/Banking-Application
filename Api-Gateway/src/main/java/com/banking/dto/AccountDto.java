package com.banking.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AccountDto {

    @NotBlank(message = "Account Holder Name Required.")
    private String accountHolderName;
    @NotBlank(message = "email required")
    @Email(message = "Invalid email format")
    private String email;
    @NotBlank(message = "Mobile Number required.")
    private String phone;
    private String accountType;
    @NotNull(message = "Initial deposit required.")
    @Positive(message = "Initial Deposit Must be positive.")
    private BigDecimal initialDeposit;
}
