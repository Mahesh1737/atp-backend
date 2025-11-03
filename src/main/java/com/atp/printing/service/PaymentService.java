package com.atp.printing.service;

import com.atp.printing.dto.PaymentCompleteDto;
import com.atp.printing.dto.PaymentOrderResponseDto;
import com.atp.printing.entity.Payment;
import com.atp.printing.entity.Session;
import com.atp.printing.repository.PaymentRepository;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final SessionService sessionService;
    private final RazorpayClient razorpayClient;

    @Value("${razorpay.key-id}")
    private String razorpayKeyId;

    @Value("${razorpay.secret}")
    private String razorpaySecret;

    @Transactional
    public PaymentOrderResponseDto createOrder(String sessionId, Integer amountInPaise) {
        log.info("Creating Razorpay order for session: {}", sessionId);

        Session session = sessionService.findBySessionId(sessionId);

        if (session.getPaymentStatus() == Session.PaymentStatus.PAID) {
            throw new RuntimeException("Payment already completed for this session");
        }

        try {
            // Create Razorpay order
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amountInPaise);
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", "order_" + sessionId);

            Order order = razorpayClient.orders.create(orderRequest);

            // Save payment record
            Payment payment = Payment.builder()
                    .session(session)
                    .razorpayOrderId(order.get("id"))
                    .amount(amountInPaise / 100.0)
                    .currency("INR")
                    .status(Payment.PaymentStatus.CREATED)
                    .build();

            paymentRepository.save(payment);

            log.info("Razorpay order created: {}", order.get("id"));

            return PaymentOrderResponseDto.builder()
                    .orderId(order.get("id"))
                    .amount(amountInPaise)
                    .currency("INR")
                    .key(razorpayKeyId)
                    .build();

        } catch (RazorpayException e) {
            log.error("Failed to create Razorpay order", e);
            throw new RuntimeException("Failed to create payment order", e);
        }
    }

    @Transactional
    public boolean verifyPayment(String sessionId, PaymentCompleteDto dto) {
        log.info("Verifying payment for session: {}", sessionId);

        try {
            // Verify signature
            String generatedSignature = generateSignature(
                    dto.getRazorpay_order_id(),
                    dto.getRazorpay_payment_id()
            );

            if (!generatedSignature.equals(dto.getRazorpay_signature())) {
                log.error("Payment signature verification failed for session: {}", sessionId);
                return false;
            }

            // Update payment record
            Payment payment = paymentRepository.findByRazorpayOrderId(dto.getRazorpay_order_id())
                    .orElseThrow(() -> new RuntimeException("Payment not found"));

            payment.setRazorpayPaymentId(dto.getRazorpay_payment_id());
            payment.setRazorpaySignature(dto.getRazorpay_signature());
            payment.setStatus(Payment.PaymentStatus.CAPTURED);
            payment.setCompletedAt(LocalDateTime.now());
            paymentRepository.save(payment);

            // Update session payment status
            sessionService.updatePaymentStatus(sessionId, Session.PaymentStatus.PAID);

            log.info("Payment verified successfully for session: {}", sessionId);
            return true;

        } catch (Exception e) {
            log.error("Payment verification failed", e);
            return false;
        }
    }

    private String generateSignature(String orderId, String paymentId) {
        try {
            String payload = orderId + "|" + paymentId;
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    razorpaySecret.getBytes(),
                    "HmacSHA256"
            );
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(payload.getBytes());

            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            log.error("Error generating signature", e);
            throw new RuntimeException("Failed to generate signature", e);
        }
    }

    public boolean verifyWebhookSignature(String payload, String signature) {
        try {
            JSONObject options = new JSONObject();
            options.put("razorpay_payment_id", payload);
            options.put("razorpay_signature", signature);

            return Utils.verifyWebhookSignature(payload, signature, razorpaySecret);
        } catch (RazorpayException e) {
            log.error("Webhook signature verification failed", e);
            return false;
        }
    }

    @Transactional
    public void handlePaymentFailure(String sessionId, String errorMessage) {
        Payment payment = paymentRepository.findBySessionId(
                sessionService.findBySessionId(sessionId).getId()
        ).orElse(null);

        if (payment != null) {
            payment.setStatus(Payment.PaymentStatus.FAILED);
            payment.setErrorMessage(errorMessage);
            paymentRepository.save(payment);
        }

        sessionService.updatePaymentStatus(sessionId, Session.PaymentStatus.FAILED);
        log.warn("Payment failed for session: {} - {}", sessionId, errorMessage);
    }
}