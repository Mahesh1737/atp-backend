package com.atp.printing.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCompleteDto {
    @NotBlank(message = "Payment ID is required")
    private String razorpay_payment_id;

    @NotBlank(message = "Order ID is required")
    private String razorpay_order_id;

    @NotBlank(message = "Signature is required")
    private String razorpay_signature;
}