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
public class PrinterStatusUpdateDto {
    @NotBlank(message = "Session ID is required")
    private String sessionId;

    @NotBlank(message = "Status is required")
    private String status; // PRINTING, COMPLETED, FAILED

    private String errorMessage;
}
