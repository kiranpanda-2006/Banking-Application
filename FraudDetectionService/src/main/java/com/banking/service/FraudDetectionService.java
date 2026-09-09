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
    private final KafkaTemplate<String,Object> kafkaTemplate;
    private RedisTemplate<String, String> redisTemplate;
    private static final String VERIFICATION_REQUIRED_TOPIC = "verification.required";
    private static final String FRAUD_CHECK_CLEAN_RESULT_TOPIC = "fraud.check.clean";
    @Value("${fraud.transaction-limit-per-minuit}")
    private int maxTransactionPerMinuit;
    @Value("${fraud.suspicious-amount-multiplyer}")
    private double suspiciousAmountMultiplyer;
    @Value("${fraud.max-balance-percentage}")
    private double maxBalancePercentage;



    public void checkTransaction(Map<String, Object> payload) {

        String transactionId = (String) payload.get("transactionId");
        String accountNumber = (String) payload.get("senderAccountNumber");
        BigDecimal amount = new BigDecimal(payload.get("amount").toString());

//        fetching original balance from senders account number from Account Service
        BigDecimal senderBalance = accountServiceClient.getBalance(accountNumber);

        log.info("checking transaction: {}, account: {}, amount: {}, balance: {}"
                , transactionId,
                accountNumber,
                amount,
                senderBalance
        );

        FraudCheckResult result =
                performFraudCheck(accountNumber, amount, senderBalance);
        if (result.isFraud()) {
            log.info("Suspicious Activity Detected: account {}, reason: {}" +
                    "Requesting OTP verification", accountNumber, result.getReason());

            Map<String,Object> verificationEvent = new HashMap<>();
            verificationEvent.put("transactionId",transactionId);
            verificationEvent.put("accountNumber",accountNumber);
            verificationEvent.put("amount",amount);
            verificationEvent.put("reason",result.getReason());

            kafkaTemplate.send(VERIFICATION_REQUIRED_TOPIC,transactionId,verificationEvent);
        }else {
//            transaction is clean
            log.info("transaction clean");

            Map<String,Object> transactionCleanEvent = new HashMap<>();
            transactionCleanEvent.put("transactionId",transactionId);
            transactionCleanEvent.put("accountNumber",accountNumber);
            transactionCleanEvent.put("amount",amount);
            transactionCleanEvent.put("reason",null);

            kafkaTemplate.send(FRAUD_CHECK_CLEAN_RESULT_TOPIC,transactionId,transactionCleanEvent);
        }

    }

    private FraudCheckResult performFraudCheck(String accountNumber,
                                               BigDecimal amount,
                                               BigDecimal senderBalance) {
//        1st pattern: velocity check
        if (isVelocityExceeded(accountNumber)){
            return new FraudCheckResult(true,"TooMany transaction in 60 seconds"+
                    " -- velocity limit exceeded.");
        }

//        2nd pattern: transferring amount more than usual transaction
        if (isAmountSuspicious(accountNumber,amount)){
            return new FraudCheckResult(true,"Unusual Transaction amount "+
                    " -- Transaction is 3x greater than from Average daily Transaction ");
        }

//        3rd pattern: Balance Check
        if (senderBalance.compareTo(BigDecimal.ZERO) < 0
        && isBalanceCheckFailed(senderBalance,amount)){
            return new FraudCheckResult(true,"Transaction exceed 90% of account Balance.");
        }
        return new FraudCheckResult(false,null);
    }

    private boolean isBalanceCheckFailed(BigDecimal senderBalance, BigDecimal amount) {

        BigDecimal maxAllowed = senderBalance.multiply(
                BigDecimal.valueOf(maxBalancePercentage)
        );

        log.info("Balance Check- amount: {} maxAllowed: {} suspicious: {}", amount,maxAllowed,amount.compareTo(maxAllowed)>0);

        return amount.compareTo(maxAllowed)>0;
    }

    private boolean isAmountSuspicious(String accountNumber, BigDecimal amount) {

        String avgKey = "Fraud:avg-amount"+accountNumber;
        String avgStr = redisTemplate.opsForValue().get(avgKey);

        if (avgStr == null){
            redisTemplate.opsForValue().set(avgKey, amount.toString());
            return false;
        }

        BigDecimal avgAmount = new BigDecimal(avgStr);

        BigDecimal threshHold = avgAmount.multiply(
                BigDecimal.valueOf(suspiciousAmountMultiplyer)
        );

//        update the running average

        BigDecimal newAvg = avgAmount.add(amount)
                .divide(BigDecimal.valueOf(2),2, RoundingMode.HALF_UP);

        redisTemplate.opsForValue().set(avgKey,newAvg.toString());

        log.info("Amount Check - amount: {} threshold: {} suspicious: {}" ,amount,threshHold,amount.compareTo(threshHold)>0);

        return amount.compareTo(threshHold)>0;
    }

    private boolean isVelocityExceeded(String accountNumber) {
        String key = "fraud:velocity"+accountNumber;
        Long count = redisTemplate.opsForValue().increment(key);

        if (count != null && count == 1){
            redisTemplate.expire(key,60, TimeUnit.SECONDS);
        }
        log.info("velocity check: account {} count {}/{}",accountNumber,count, maxTransactionPerMinuit);

        return count != null && count > maxTransactionPerMinuit;
    }

}
