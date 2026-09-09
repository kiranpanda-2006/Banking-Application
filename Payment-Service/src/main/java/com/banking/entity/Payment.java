package com.banking.entity;

import com.banking.enun.PaymentStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String razorPayOrderId;
    private String razorPayPaymentId;
    @Column(nullable = false)
    private String accountNumber;
    @Column(nullable = false,precision = 15,scale = 2)
    private BigDecimal amount;
    @Column(nullable = false)
    private String currency;
    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    private String description;

    private String failureReason;
    @CreationTimestamp
    private LocalDateTime createdAt;
    @CreationTimestamp
    private LocalDateTime updatedAt;
}
