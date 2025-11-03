package com.atp.printing.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentOrderDto {
    @NotNull(message = "Amount is required")
    @Min(value = 1, message = "Amount must be at least 1")
    private Integer amount; // in paise
}