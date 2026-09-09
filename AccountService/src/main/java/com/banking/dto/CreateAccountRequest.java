package com.banking.dto;


import com.banking.enums.AccountType;
import jakarta.validation.constraints.Email;
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
public class CreateAccountRequest {

    @NotBlank(message = "Account Holder Name Required.")
    private String accountHolderName;
    @NotBlank(message = "email required")
    @Email(message = "Invalid email format")
    private String email;
    @NotBlank(message = "Mobile Number required.")
    private String phone;
    @NotNull(message = "Account Type required.")
    private AccountType accountType;
    @NotNull(message = "Initial deposit required.")
    @Positive(message = "Initial Deposit Must be positive.")
    private BigDecimal initialDeposit;
}
