package com.banking.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;

@Service
@Slf4j
public class NotificationService {

    /**
     * consume the generated otp from transaction service
     * notify to the user.
     * @param payload
     */
    @KafkaListener(topics = "transaction.otp.generated")
    public void consumeOtpGenerated(
            @Payload Map<String,Object> payload
            ){
        try {
            String accountNumber = (String) payload.get("accountNumber");
            String otp = (String) payload.get("otp");
            String transactionId = (String) payload.get("transactionId");
            String amount = payload.get("amount").toString();
            String reason = (String) payload.get("reason");

            sendAlert(
                    accountNumber,
                    "TRANSACTION VERIFICATION REQUIRED",
                    String.format(
                            "suspicious Activity detect on your account. "+
                                    "Reason: %s "+
                                    "A transaction of %s is pending verification. "+
                                    "Your OTP is: %s. valid for 5 mints. "+
                                    "If this wasn't you - ignore this message"
                    )

            );
        }catch (Exception e){
            log.error("Error Sending OTP notification {}",e.getMessage());
        }
    }
    @KafkaListener(topics = "transaction.completed")
    public void consumeTransactionGenerated(
            Map<String,Object> payload
    ){
        try {
            String senderAccountNumber = (String) payload.get("senderAccountNumber");
            String receiverAccountNumber = (String) payload.get("receiverAccountNumber");
            String amount = payload.get("accountNumber").toString();
//            debit alert
            sendAlert(
                    senderAccountNumber,
                    "DEBIT ALERT",
                    String.format(
                            "%s debited from account %s ",
                            amount,
                            senderAccountNumber
                    )
            );
            sendAlert(
                    receiverAccountNumber,
                    "CREDIT ALERT",
                    String.format(
                            "%s credited to account %s",
                            amount,receiverAccountNumber
                    )
            );
        }catch (Exception e){
            log.error("Error sending transaction notification: {}",e.getMessage());
        }
    }
    @KafkaListener(topics = "fraud.detected")
    public void consumeFraudDetected(
            Map<String,Object> payload
    ){
        try {
            String senderAccount = (String) payload.get("accountNumber");
            String reason = (String) payload.get("reason");

            sendAlert(senderAccount,
                    "SUSPICIOUS ACTIVITY DETECTED",
                    String.format(
                            "your account %s has been blocked "+
                                    "Reason: %s"+
                            "Please contact your Bank immediately",
                            senderAccount,
                            reason
                    ));
        } catch (Exception e) {
            log.error("Error sending Fraud alert: {}",e.getMessage());
        }
    }
    @KafkaListener(topics = "transaction.refunded")
    public void consumeTransactionRefunded(
            @Payload Map<String,Object> payload
    ){
        try{
            String senderAccount = (String) payload.get("senderAccountNumber");
            String amount = payload.get("amount").toString();
            String reason = (String) payload.get("reason");


            sendAlert(senderAccount,
                    "REFUNDED PROCESSED",
                    String.format(
                            "your transaction of %s was cancelled"+
                                    "Reason: %s"+
                                    "%s has been refunded to account %s",
                            amount,
                            reason,
                            amount,
                            senderAccount
                    ));
        }catch (Exception e){
            log.error("Error sending refund notification: {}",e.getMessage());
        }
    }
    @KafkaListener(topics = "payment.completed")
    public void consumePaymentCompleted(
            @Payload Map<String,Object> payload
    ){
        try{
            String accountNumber = (String) payload.get("accountNumber");
            String amount = payload.get("amount").toString();


            sendAlert(accountNumber,
                    "PAYMENT SUCCESSFUL",
                    String.format(
                            "your payment of %s is completed"+
                            "RazorPay Id: %s",
                            amount, payload.get("razorPayPaymentId")
                    ));
        }catch (Exception e){
            log.error("Error sending payment completed notification: {}",e.getMessage());
        }
    }
    @KafkaListener(topics = "payment.failed")
    public void consumePaymentFailed(
            @Payload Map<String,Object> payload
    ){
        try{
            String accountNumber = (String) payload.get("accountNumber");
            String amount = payload.get("amount").toString();
            String reason = (String) payload.get("failureReason");


            sendAlert(accountNumber,
                    "PAYMENT FAILED",
                    String.format(
                            "your payment of %s is failed"+
                                    "Reason: %s"+
                                    "RazorPay Id: %s",
                            amount,reason, payload.get("razorPayOrderId")
                    ));
        }catch (Exception e){
            log.error("Error sending payment failed notification: {}",e.getMessage());
        }
    }
    private void sendAlert(String accountNumber, String sub, String message) {

        log.info("------------------------");
        log.info("AccountNumber: {}",accountNumber);
        log.info("Subject: {}",sub);
        log.info("Message: {}",message);
        log.info("------------------------");


    }


}
