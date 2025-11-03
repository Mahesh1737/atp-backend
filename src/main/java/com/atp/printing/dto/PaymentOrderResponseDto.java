package com.atp.printing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentOrderResponseDto {
    private String orderId;
    private Integer amount;
    private String currency;
    private String key;
}
