package com.atp.printing.controller;

import com.atp.printing.dto.*;
import com.atp.printing.entity.Session;
import com.atp.printing.service.PaymentService;
import com.atp.printing.service.SessionService;
import com.atp.printing.service.WebSocketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final SessionService sessionService;
    private final WebSocketService webSocketService;

    @PostMapping("/session/{sessionId}/create-order")
    public ResponseEntity<ApiResponseDto<PaymentOrderResponseDto>> createOrder(
            @PathVariable String sessionId,
            @Valid @RequestBody PaymentOrderDto dto) {

        log.info("Creating payment order for session: {}", sessionId);

        try {
            PaymentOrderResponseDto response = paymentService.createOrder(sessionId, dto.getAmount());
            return ResponseEntity.ok(ApiResponseDto.success("Order created", response));

        } catch (Exception e) {
            log.error("Failed to create order", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponseDto.error(e.getMessage()));
        }
    }

    @PostMapping("/session/{sessionId}/payment/complete")
    public ResponseEntity<ApiResponseDto<PaymentCompleteResponseDto>> completePayment(
            @PathVariable String sessionId,
            @Valid @RequestBody PaymentCompleteDto dto) {

        log.info("Completing payment for session: {}", sessionId);

        try {
            boolean verified = paymentService.verifyPayment(sessionId, dto);

            if (!verified) {
                return ResponseEntity.badRequest()
                        .body(ApiResponseDto.error("Payment verification failed"));
            }

            // Get session and send print job to printer
            Session session = sessionService.findBySessionId(sessionId);

            try {
                webSocketService.sendPrintJob(session);
                sessionService.updateSessionStatus(sessionId, Session.SessionStatus.PRINTING);
            } catch (Exception e) {
                log.error("Failed to send print job", e);
                // Payment is still successful, but printing failed
            }

            PaymentCompleteResponseDto response = PaymentCompleteResponseDto.builder()
                    .success(true)
                    .message("Payment successful")
                    .printJobId(sessionId)
                    .build();

            return ResponseEntity.ok(ApiResponseDto.success("Payment completed", response));

        } catch (Exception e) {
            log.error("Failed to complete payment", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponseDto.error(e.getMessage()));
        }
    }

    @PostMapping("/payment/webhook")
    public ResponseEntity<String> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("X-Razorpay-Signature") String signature) {

        log.info("Received Razorpay webhook");

        try {
            boolean verified = paymentService.verifyWebhookSignature(payload, signature);

            if (!verified) {
                log.error("Webhook signature verification failed");
                return ResponseEntity.badRequest().body("Invalid signature");
            }

            // Process webhook event
            log.info("Webhook verified successfully");

            return ResponseEntity.ok("Webhook processed");

        } catch (Exception e) {
            log.error("Webhook processing failed", e);
            return ResponseEntity.internalServerError().body("Webhook processing failed");
        }
    }
}