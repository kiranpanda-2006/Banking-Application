package com.banking.service;

import com.banking.client.AccountServiceClient;
import com.banking.model.FraudCheckResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class FraudDetectionService {

    private final AccountServiceClient accountServiceClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final RedisTemplate<String, String> redisTemplate;

    private static final String VERIFICATION_REQUIRED_TOPIC =
            "verification.required";

    private static final String FRAUD_CHECK_CLEAN_RESULT_TOPIC =
            "fraud.check.clean";

    @Value("${fraud.transaction-limit-per-minuit}")
    private int maxTransactionPerMinuit;

    @Value("${fraud.suspicious-amount-multiplyer}")
    private double suspiciousAmountMultiplyer;

    @Value("${fraud.max-balance-percentage}")
    private double maxBalancePercentage;

    public void checkTransaction(Map<String, Object> payload) {

        String transactionId = String.valueOf(payload.get("id"));
        String accountNumber =
                String.valueOf(payload.get("senderAccountNumber"));

        Object amountValue = payload.get("amount");

        if (amountValue == null) {
            throw new IllegalArgumentException(
                    "Transaction amount is missing");
        }

        BigDecimal amount = new BigDecimal(amountValue.toString());

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "Transaction amount must be positive");
        }

        // Fetch sender balance from Account Service
        BigDecimal senderBalance =
                accountServiceClient.getBalance(accountNumber);

        if (senderBalance == null) {
            throw new IllegalStateException(
                    "Unable to fetch sender account balance");
        }

        log.info(
                "Checking transaction: {}, account: {}, amount: {}, balance: {}",
                transactionId, accountNumber, amount, senderBalance
        );

        FraudCheckResult result =
                performFraudCheck(accountNumber, amount, senderBalance);

        if (result.isFraud()) {

            log.warn(
                    "Suspicious activity detected for account {}: {}",
                    accountNumber, result.getReason()
            );

            Map<String, Object> verificationEvent = new HashMap<>();

            verificationEvent.put("transactionId", transactionId);
            verificationEvent.put("accountNumber", accountNumber);
            verificationEvent.put("amount", amount);
            verificationEvent.put("reason", result.getReason());

            kafkaTemplate.send(
                    VERIFICATION_REQUIRED_TOPIC,
                    transactionId,
                    verificationEvent
            );

        } else {

            log.info("Transaction {} passed fraud check", transactionId);

            Map<String, Object> cleanEvent = new HashMap<>();

            cleanEvent.put("transactionId", transactionId);
            cleanEvent.put("accountNumber", accountNumber);
            cleanEvent.put("amount", amount);
            cleanEvent.put("reason", null);

            kafkaTemplate.send(
                    FRAUD_CHECK_CLEAN_RESULT_TOPIC,
                    transactionId,
                    cleanEvent
            );
        }
    }

    private FraudCheckResult performFraudCheck(
            String accountNumber,
            BigDecimal amount,
            BigDecimal senderBalance) {

        // 1. Velocity check
        if (isVelocityExceeded(accountNumber)) {
            return new FraudCheckResult(
                    true,
                    "Too many transactions in 60 seconds"
            );
        }

        // 2. Unusual transaction amount check
        if (isAmountSuspicious(accountNumber, amount)) {
            return new FraudCheckResult(
                    true,
                    "Unusual transaction amount detected"
            );
        }

        // 3. Balance percentage check
        if (isBalanceCheckFailed(senderBalance, amount)) {
            return new FraudCheckResult(
                    true,
                    "Transaction exceeds configured balance percentage"
            );
        }

        return new FraudCheckResult(false, null);
    }

    private boolean isBalanceCheckFailed(
            BigDecimal senderBalance,
            BigDecimal amount) {

        BigDecimal maxAllowed = senderBalance.multiply(
                BigDecimal.valueOf(maxBalancePercentage)
        );

        boolean suspicious = amount.compareTo(maxAllowed) > 0;

        log.info(
                "Balance check: amount={}, maxAllowed={}, suspicious={}",
                amount, maxAllowed, suspicious
        );

        return suspicious;
    }

    private boolean isAmountSuspicious(
            String accountNumber,
            BigDecimal amount) {

        String avgKey = "fraud:avg-amount:" + accountNumber;

        String avgStr = redisTemplate.opsForValue().get(avgKey);

        // First transaction: initialize average
        if (avgStr == null) {
            redisTemplate.opsForValue().set(avgKey, amount.toString());
            return false;
        }

        BigDecimal avgAmount = new BigDecimal(avgStr);

        BigDecimal threshold = avgAmount.multiply(
                BigDecimal.valueOf(suspiciousAmountMultiplyer)
        );

        boolean suspicious = amount.compareTo(threshold) > 0;

        // Update average only if transaction is not suspicious
        if (!suspicious) {

            BigDecimal newAvg = avgAmount.add(amount)
                    .divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);

            redisTemplate.opsForValue().set(avgKey, newAvg.toString());
        }

        log.info(
                "Amount check: amount={}, threshold={}, suspicious={}",
                amount, threshold, suspicious
        );

        return suspicious;
    }

    private boolean isVelocityExceeded(String accountNumber) {

        String key = "fraud:velocity:" + accountNumber;

        Long count = redisTemplate.opsForValue().increment(key);

        if (count != null && count == 1) {
            redisTemplate.expire(key, 60, TimeUnit.SECONDS);
        }

        log.info(
                "Velocity check: account={}, count={}/{}",
                accountNumber, count, maxTransactionPerMinuit
        );

        return count != null && count > maxTransactionPerMinuit;
    }
}
