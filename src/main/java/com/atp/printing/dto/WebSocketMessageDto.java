package com.atp.printing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketMessageDto {
    private String event;
    private String sessionId;
    private String downloadUrl;
    private String fileName;
    private Integer pageCount;
    private String colorMode;
    private Object metadata;
}