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
public class PrinterResponseDto {
    private Long id;
    private String name;
    private String location;
    private String jwtToken;
    private String status;
    private LocalDateTime createdAt;
}