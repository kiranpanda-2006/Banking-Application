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
    public void consumeVerificationRequired(@Payload Map<String,Object> payload){

        try{
            String transactionId = (String) payload.get("transactionId");
            String accountNumber = (String) payload.get("accountNumber");
            String reason  = (String) payload.get("reason");


            log.info("Verification required: transaction: {} reason: {}",transactionId,reason);

            Transaction transaction
                    = transactionRepo.findById(transactionId).orElseThrow(
                    () -> new ResourceNotFoundException("Transaction not found.")
            );

            if (transaction.getStatus() != TransactionStatus.PROCESSING){
                log.warn("Transaction Not Processing - skipping, "+transactionId);
            }

//            generate 6 digit otp
            String otp = String.format("%06d", (int) (Math.random() * 900000)+100000);

//            store it in redis and set the expiration time
            String otpKey = "verificationOtp" + transactionId;

            redisTemplate.opsForValue().set(otpKey, otp, OTP_EXPIRATION_MINUTE, TimeUnit.MINUTES);

//            update status

            transaction.setStatus(TransactionStatus.PENDING_VERIFICATION);
            transactionRepo.save(transaction);

            log.info("OTP generated for Transaction- {} expires in {} Minuit",otp,OTP_EXPIRATION_MINUTE );

//            notify the user via email or sms

            Map<String,Object> otpEvent = new HashMap<>();

            otpEvent.put("transactionId",transactionId);
            otpEvent.put("accountNumber",accountNumber);
            otpEvent.put("reason",reason);
            otpEvent.put("otp",otp);
            otpEvent.put("amount",payload.get("amount"));

            kafkaTemplate.send(TRANSACTION_OTP_GENERATED_TOPIC,transactionId,otpEvent);


        }catch (Exception e){

            log.error("Error handling verification required: {} ", e.getMessage());
        }

    }

    /**
     * consume fraud.check.clean
     * confirm the transaction is completed
     */
    @KafkaListener(topics = "fraud.check.result")
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
