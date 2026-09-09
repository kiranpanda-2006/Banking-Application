package com.banking.service;

import com.banking.dto.CreatePaymentRequest;
import com.banking.dto.PaymentOrderResponse;
import com.banking.entity.Payment;
import com.banking.enun.PaymentStatus;
import com.banking.exception.ResourceNotFoundException;
import com.banking.repository.PaymentRepository;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;

    private final KafkaTemplate<String,Object> kafkaTemplate;

    private static final String PAYMENT_COMPLETED_TOPIC = "payment.captured";
    private static final String PAYMENT_FAILED_TOPIC = "payment.failed";

    @Value("${Razorpay.key}")
    private final String razorPayKey;
    @Value("${Razorpay.secret}")
    private final String razorPaySecret;

    /**
     * create Razorpay payment order
     *
     *
     * flow:
     * 1.create order in Razorpay
     * 2.Save Payment record in DB
     * 3.Return order details to frontend
     * 4.FrontEnd shows razorPay checkOut
     * 5.user pays
     * 6.Razorpay calls webHook
     * @param request
     * @return
     */

    public PaymentOrderResponse createPaymentOrder(@Valid CreatePaymentRequest request) throws RazorpayException {
        log.info("Creating Payment Order for account: {} amount: {}",
                request.getAccountNumber(),
                request.getAmount());

        RazorpayClient razorpayClient = new RazorpayClient(razorPayKey,razorPaySecret);

//            converted amount
        int convertedAmount = request.getAmount()
                .multiply(BigDecimal.valueOf(100))
                .intValue();

        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount",convertedAmount);
        orderRequest.put("currency","UDD/INR");
        orderRequest.put("receipt", "rcpt_" + System.currentTimeMillis() +
                UUID.randomUUID().toString()
                        .replace("-", "")
                        .substring(0,10)
        );

        Order razorpayOrder = razorpayClient.orders.create(orderRequest);

        log.info("Razorpay Order created: {}",razorpayOrder.get("id").toString());

//        save payment Record in DB

        Payment payment = Payment.builder()
                .razorPayOrderId(razorpayOrder.get("id").toString())
                .accountNumber(request.getAccountNumber())
                .amount(request.getAmount())
                .currency("USD/INR")
                .status(PaymentStatus.CREATED)
                .description(request.getDescription())
                .build();

        Payment savedPayment =
                paymentRepository.save(payment);

        return new PaymentOrderResponse(
                razorpayOrder.get("id").toString(),
                savedPayment.getId(),
                savedPayment.getAmount(),
                "INR",
                "CREATED",
                razorPayKey
        );
    }

    public void handleWebHook(Map<String, Object> payload) {

        log.info("Received Razorpay webhook: {}",payload.get("event"));

        String event = (String) payload.get("event");

        if ("payment.captured".equals(event)){
            handlePaymentSuccess(payload);
        } else if ("payment.failed".equals(event)) {
            handlePaymentFailure(payload);
        }


    }
    //helper methods
    private void handlePaymentSuccess(Map<String, Object> payload) {
        try {
            Map<String,Object> paymentData = extractPaymentData(payload);

            assert paymentData != null;
            String orderID = (String) paymentData.get("order_id");
            String paymentId = (String) paymentData.get("id");

            Payment payment = paymentRepository.findByRazorpayOrderId(orderID)
                    .orElseThrow(() -> new ResourceNotFoundException("Not found Order: "+orderID));

            payment.setRazorPayPaymentId(paymentId);
            payment.setStatus(PaymentStatus.COMPLETED);
            paymentRepository.save(payment);

//            publish payment completed event

            Map<String,Object> event = new HashMap<>();
            event.put("paymentId",payment.getId());
            event.put("accountNumber",payment.getAccountNumber());
            event.put("amount",payment.getAmount());
            event.put("razorPayPaymentId",paymentId);

            kafkaTemplate.send(PAYMENT_COMPLETED_TOPIC,payment.getId(),event);

            log.info("Payment Completed: {}",payment.getId());

        } catch (Exception e) {
            log.error("Error handling payment Success: {}",e.getMessage());
        }
    }

    private void handlePaymentFailure(Map<String, Object> payload) {

        try {
            Map<String,Object> paymentData = extractPaymentData(payload);

            assert paymentData != null;
            String orderID = (String) paymentData.get("order_id");

            Payment payment = paymentRepository.findByRazorpayOrderId(orderID)
                    .orElseThrow(() -> new ResourceNotFoundException("Not found Order: "+orderID));

            payment.setFailureReason("payment failed via RazorPay");
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);

//            publish payment completed event

            Map<String,Object> event = new HashMap<>();
            event.put("paymentId",payment.getId());
            event.put("accountNumber",payment.getAccountNumber());
            event.put("amount",payment.getAmount());
            event.put("reason","payment failed via RazorPay");

            kafkaTemplate.send(PAYMENT_FAILED_TOPIC,payment.getId(),event);

            log.warn("Payment Failed: {}",payment.getId());

        } catch (Exception e) {
            log.error("Error handling payment failed: {}",e.getMessage());
        }

    }


    private Map<String, Object> extractPaymentData(Map<String, Object> payload) {
        Map<String,Object> entity = (Map<String, Object>) payload.get("payload");

        Map<String,Object> paymentWrapper = (Map<String, Object>) entity.get("payment");

        return (Map<String, Object>) paymentWrapper.get("entity");
    }



}

