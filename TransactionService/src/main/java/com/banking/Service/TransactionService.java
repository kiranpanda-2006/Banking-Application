package com.banking.Service;

import com.banking.client.AccountServiceClient;
import com.banking.dto.TransactionResponse;
import com.banking.dto.TransferRequest;
import com.banking.entity.Transaction;
import com.banking.enums.TransactionStatus;
import com.banking.enums.TransactionType;
import com.banking.event.TransactionCompleteEvent;
import com.banking.event.TransactionInitiatedEvent;
import com.banking.exception.ResourceNotFoundException;
import com.banking.repository.TransactionRepo;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final TransactionRepo transactionRepo;
    private final AccountServiceClient accountServiceClient;
    private final RedisTemplate<String,String> redisTemplate;
    private final KafkaTemplate<String, Object> template;

    private static final String TRANSACTION_INITIATED_TOPIC = "transaction.initiated";
    private static final String TRANSACTION_COMPLETED_TOPIC = "transaction.completed";
    private static final String TRANSACTION_REFUNDED_TOPIC = "transaction.refunded";
    private static final String FRAUD_DETECTED_TOPIC = "fraud.detected";


    /*
    * SAGA Step-1: initiate transfer
    * Deducts from sender via feign
    * save transactions as processing
    * publish event to Kafka for fraudCheck
    * Returns
    * */
    public TransactionResponse transfer(@Valid TransferRequest request) {
        log.info("SAGA start - Transaction: {} -> {} amount: {}",
                request.getSenderAccountNumber(),
                request.getReceiverAccountNumber(),
                request.getAmount());

//      SAGA: step-1:   deduct balance from senders account
        String sendersAccountNumber =
                request.getSenderAccountNumber();
        BigDecimal amount =
                request.getAmount();
        accountServiceClient
                .deductBalance(sendersAccountNumber,amount);

        accountServiceClient
                .creditBalance(request.getReceiverAccountNumber(), amount);

       Transaction savedTransaction =
               transactionRepo.save(mapToEntity(request));

       log.info("Transaction saved as Processing: {}",savedTransaction.getId());
//SAGA step-2: publish for fraud Check
       TransactionInitiatedEvent event = new TransactionInitiatedEvent(
               savedTransaction.getId(),
               savedTransaction.getSenderAccountNumber(),
               savedTransaction.getReceiverAccountNumber(),
               savedTransaction.getAmount(),
               savedTransaction.getDescription()
       );

       template.send(TRANSACTION_INITIATED_TOPIC,savedTransaction.getId(),event);
       log.info("SAGA step2: TransactionInitiatedEvent Published: {}",savedTransaction.getId());
       return mapToResponse(savedTransaction);
    }

    public TransactionResponse getTransaction(String transactionId) {
        return mapToResponse(transactionRepo.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction Not found for " +transactionId)));
    }

    public List<TransactionResponse> getTransactionHistory(String accountNumber) {

        return transactionRepo
                .findBySenderAccountNumberOrderByCreatedAtDesc(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException("No Account Exist with "+accountNumber))
                .stream()
                .map(this :: mapToResponse)
                .collect(Collectors.toList());
    }

    public TransactionResponse verifyOtp(String transactionId, String otp) {
        log.info("OTP verification for the  Transaction: {}", transactionId);

        Transaction transaction = transactionRepo.findById(transactionId).
                orElseThrow(() -> new ResourceNotFoundException("No transaction found with "+transactionId));

        String otpKey = "verificationOtp" + transactionId;
        String storedOtp = redisTemplate.opsForValue().get(otpKey);

        if (storedOtp == null){
//            otp expired
            log.warn("OTP expired  for transactionId {}",transactionId);

            compantiateTransaction(transaction,"OTP expired - transaction cancelled and amount refunded.");
            return mapToResponse(transaction);
        }

        if (!storedOtp.equals(otp)){
//            Block account and refund
            log.warn("Wrong OTP Blocking account and refunding: {}", transactionId);
            redisTemplate.delete(otpKey);
            blockingAccountAndCompliant(transaction,"Wrong OTP entered- transaction cancelled, "+
                    "account Blocked for Reason. ");

            return mapToResponse(transaction);
        }
//        otp is correct
        log.info("OTP verified - completing transaction: {}",transactionId);
        redisTemplate.delete(otpKey);
        completeTransaction(transaction);

        return mapToResponse(transaction);
    }

//    helper methods starts

    private Transaction mapToEntity(@Valid TransferRequest request) {
        return Transaction.builder()
                .senderAccountNumber(request.getSenderAccountNumber())
                .receiverAccountNumber(request.getReceiverAccountNumber())
                .amount(request.getAmount())
                .type(TransactionType.TRANSFER)
                .status(TransactionStatus.PROCESSING)
                .description(request.getDescription())
                .referenceNumber(UUID.randomUUID().toString())
                .build();
    }

    private TransactionResponse mapToResponse(Transaction savedTransaction) {
        return TransactionResponse.builder()
                .id(savedTransaction.getId())
                .senderAccountNumber(savedTransaction.getSenderAccountNumber())
                .receiverAccountNumber(savedTransaction.getReceiverAccountNumber())
                .amount(savedTransaction.getAmount())
                .type(savedTransaction.getType())
                .status(savedTransaction.getStatus())
                .description(savedTransaction.getDescription())
                .failureReason(savedTransaction.getFailureReason())
                .referenceNumber(savedTransaction.getReferenceNumber())
                .createdAt(savedTransaction.getCreatedAt())
                .completedAt(savedTransaction.getCompletedAt())
                .build();
    }


    private void completeTransaction(Transaction transaction) {
        log.info("SAGA COMPLETE_TRANSACTION: from account: {} to account: {} amount: {} "
                ,transaction.getSenderAccountNumber(),
                transaction.getReceiverAccountNumber(),
                transaction.getAmount());
//        completing the transaction from senders account to receivers account
        accountServiceClient.creditBalance(
                transaction.getReceiverAccountNumber(),
                transaction.getAmount()
        );

        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setCreatedAt(LocalDateTime.now());
        transactionRepo.save(transaction);

//      PUBLISH complete event and notify to customer
        TransactionCompleteEvent completeEvent = new TransactionCompleteEvent(
                transaction.getId(),
                transaction.getSenderAccountNumber(),
                transaction.getReceiverAccountNumber(),
                transaction.getAmount(),
                transaction.getDescription()
        );

        template.send(TRANSACTION_COMPLETED_TOPIC,transaction.getId(),completeEvent);


        log.info("SAGA COMPLETE - transaction {} to amount {}",
                transaction.getId(),
                transaction.getAmount()
        );

    }

    private void blockingAccountAndCompliant(Transaction transaction, String reason) {
//        publish fraud.detected -> Account Service will block the account
        log.info("SAGA  BLOCKING account: {} AND COMPENSATE : amount: {} ",
                transaction.getSenderAccountNumber(),
                transaction.getAmount());

        Map<String,Object> fraudEvent = new HashMap<>();
        fraudEvent.put("transactionId",transaction.getId());
        fraudEvent.put("accountNumber",transaction.getSenderAccountNumber());
        fraudEvent.put("reason",reason);

        template.send(FRAUD_DETECTED_TOPIC,transaction.getSenderAccountNumber(),fraudEvent);
        log.info("fraud.detected published - account {} will be blocked, kindly contact to your bank. ",
                transaction.getSenderAccountNumber()
        );
        compantiateTransaction(transaction,reason);
    }

    private void compantiateTransaction(Transaction transaction, String reason) {
        log.info("SAGA COMPANTIATE - refunding: {} amount: {}",
                transaction.getSenderAccountNumber(),
                transaction.getAmount());

//        credit money back to senders account number
        accountServiceClient.creditBalance(
                transaction.getSenderAccountNumber(),
                transaction.getAmount()
        );

        transaction.setStatus(TransactionStatus.FLAGGED);
        transaction.setFailureReason(reason +
                " - SAGA compantiate executed, amount refunded at "+ LocalDateTime.now());

        transactionRepo.save(transaction);

//        PUBLISH refund event - and notify to customer alert
        Map<String,Object> refundEvent = new HashMap<>();

        refundEvent.put("transactionId",transaction.getId());
        refundEvent.put("senderAccountNumber",transaction.getSenderAccountNumber());
        refundEvent.put("amount",transaction.getAmount());
        refundEvent.put("reason",reason);

        template.send(TRANSACTION_REFUNDED_TOPIC,transaction.getId(),refundEvent);

        log.info("SAGA COMPENSATION complete - {} refunded to account {}",
                transaction.getAmount(),
                transaction.getSenderAccountNumber()
        );
    }

    public void processCleanResult(String transactionId) {

        Transaction transaction = transactionRepo.findById(transactionId).
                orElseThrow(() -> new ResourceNotFoundException("No transaction found with "+transactionId));

        if (transaction.getStatus() != TransactionStatus.PROCESSING){
            log.warn("Transaction Not Processing - skipping, "+transactionId);
        }

        completeTransaction(transaction);


    }
}
