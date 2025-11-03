package com.atp.printing.controller;

import com.atp.printing.dto.*;
import com.atp.printing.entity.Printer;
import com.atp.printing.entity.Session;
import com.atp.printing.service.PrinterService;
import com.atp.printing.service.SessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/printer")
@RequiredArgsConstructor
public class PrinterController {

    private final PrinterService printerService;
    private final SessionService sessionService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponseDto<PrinterResponseDto>> registerPrinter(
            @Valid @RequestBody PrinterRegistrationDto dto) {

        log.info("Registering printer: {}", dto.getName());
        PrinterResponseDto response = printerService.registerPrinter(dto);

        return ResponseEntity.ok(ApiResponseDto.success("Printer registered successfully", response));
    }

    @PostMapping("/status")
    public ResponseEntity<ApiResponseDto<String>> updatePrinterStatus(
            @Valid @RequestBody PrinterStatusUpdateDto dto,
            @RequestHeader("Authorization") String authHeader) {

        log.info("Updating status for session: {}", dto.getSessionId());

        try {
            Session.SessionStatus status;
            switch (dto.getStatus().toUpperCase()) {
                case "PRINTING":
                    status = Session.SessionStatus.PRINTING;
                    break;
                case "COMPLETED":
                    status = Session.SessionStatus.COMPLETED;
                    break;
                case "FAILED":
                    status = Session.SessionStatus.FAILED;
                    break;
                default:
                    return ResponseEntity.badRequest()
                            .body(ApiResponseDto.error("Invalid status"));
            }

            sessionService.updateSessionStatus(dto.getSessionId(), status);

            return ResponseEntity.ok(
                    ApiResponseDto.success("Status updated successfully", "OK")
            );

        } catch (Exception e) {
            log.error("Failed to update status", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponseDto.error(e.getMessage()));
        }
    }

    @PostMapping("/heartbeat")
    public ResponseEntity<ApiResponseDto<String>> heartbeat(
            @RequestHeader("Authorization") String authHeader) {

        try {
            String token = authHeader.substring(7);
            printerService.updateHeartbeat(token);

            return ResponseEntity.ok(ApiResponseDto.success("Heartbeat received", "OK"));

        } catch (Exception e) {
            log.error("Heartbeat failed", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponseDto.error(e.getMessage()));
        }
    }
}