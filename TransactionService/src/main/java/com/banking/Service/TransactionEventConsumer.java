package com.banking.Service;

import com.banking.entity.Transaction;
import com.banking.enums.TransactionStatus;
import com.banking.exception.ResourceNotFoundException;
import com.banking.repository.TransactionRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class TransactionEventConsumer {


    private final TransactionRepo transactionRepo;
    private final TransactionService transactionService;

    private final RedisTemplate<String,String> redisTemplate;

    private final KafkaTemplate<String,Object> kafkaTemplate;

    private static final long OTP_EXPIRATION_MINUTE = 5;

    private static final String TRANSACTION_OTP_GENERATED_TOPIC = "transaction.otp.generated";

    /**
     *consume verification.required
     * generate otp and user to verify
     * @param payload
     */

    @KafkaListener(topics = "verification.required")
    public void consumeVerificationRequired(
            @Payload Map<String, Object> payload) {

        log.info("method started.");

        try {
            log.info("Received verification.required event: {}", payload);

            String transactionId = String.valueOf(payload.get("transactionId"));
            String accountNumber = String.valueOf(payload.get("accountNumber"));
            String reason = String.valueOf(payload.get("reason"));

            Transaction transaction = transactionRepo.findById(transactionId)
                    .orElseThrow(() ->
                            new ResourceNotFoundException("Transaction not found: " + transactionId));

            if (transaction.getStatus() != TransactionStatus.PROCESSING) {
                log.warn("Transaction not processing; skipping: {}", transactionId);
                return;
            }

            // Generate a 6-digit OTP
            String otp = String.format("%06d",
                    new java.security.SecureRandom().nextInt(900000) + 100000);

            // Store OTP in Redis for 5 minutes
            String otpKey = "verificationOtp:" + transactionId;

            redisTemplate.opsForValue().set(
                    otpKey,
                    otp,
                    OTP_EXPIRATION_MINUTE,
                    TimeUnit.MINUTES
            );

            // Update transaction status
            transaction.setStatus(TransactionStatus.PENDING_VERIFICATION);
            transactionRepo.save(transaction);

            // Publish OTP notification event
            Map<String, Object> otpEvent = new HashMap<>();
            otpEvent.put("transactionId", transactionId);
            otpEvent.put("accountNumber", accountNumber);
            otpEvent.put("reason", reason);
            otpEvent.put("otp", otp);
            otpEvent.put("amount", payload.get("amount"));

            kafkaTemplate.send(
                    TRANSACTION_OTP_GENERATED_TOPIC,
                    transactionId,
                    otpEvent
            );

            log.info("OTP generated for transaction {} and expires in {} minutes",
                    transactionId, OTP_EXPIRATION_MINUTE);

        } catch (Exception e) {
            log.error("Error handling verification.required event", e);
        }
    }
    /**
     * consume fraud.check.clean
     * confirm the transaction is completed
     */
    @KafkaListener(topics = "fraud.check.clean")
    public void consumeFraudCheckCleanResult(
            @Payload Map<String,Object> payload
    ){
        try {
            String transactionId = (String) payload.get("transactionId");

            transactionService.processCleanResult(transactionId);
        }catch (Exception e){
            log.error("Error Processing Fraud check result: "+e.getMessage());
        }
    }
}
