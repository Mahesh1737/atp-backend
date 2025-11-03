package com.atp.printing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionResponseDto {
    private Long id;
    private String sessionId;
    private String printerName;
    private Double pricePerPage;
    private List<String> colorOptions;
    private LocalDateTime expiresAt;
    private String status;
    private String qrUrl;
}
