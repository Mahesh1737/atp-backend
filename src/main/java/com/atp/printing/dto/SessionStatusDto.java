package com.atp.printing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionStatusDto {
    private String sessionId;
    private String status;
    private String paymentStatus;
    private String fileName;
    private Integer pageCount;
    private Double amount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
