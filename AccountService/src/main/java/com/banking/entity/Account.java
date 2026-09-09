package com.banking.entity;


import com.banking.enums.AccountStatus;
import com.banking.enums.AccountType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "accounts")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    @Column(nullable = false)
    private String accountNumber;
    @Column(nullable = false)
    private String accountHolderName;
    @Column(nullable = false)
    private String email;
    @Column(nullable = false)
    private String phone;
    @Column(nullable = false)
    @Enumerated(value = EnumType.STRING)
    private AccountType accountType;
    @Column(nullable = false)
    @Enumerated(value = EnumType.STRING)
    private AccountStatus status;
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal balance;
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal dailyTransactionLimit;
    @CreationTimestamp
    private LocalDateTime createdAt;

    private LocalDateTime updateAt;
}
