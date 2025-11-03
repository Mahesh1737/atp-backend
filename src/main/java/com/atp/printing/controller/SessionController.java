package com.atp.printing.controller;

import com.atp.printing.dto.*;
import com.atp.printing.service.SessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/session")
@RequiredArgsConstructor
public class SessionController {

    private final SessionService sessionService;

    @PostMapping("/create")
    public ResponseEntity<ApiResponseDto<SessionResponseDto>> createSession(
            @Valid @RequestBody SessionCreateDto dto) {

        log.info("Creating session for printer: {}", dto.getPrinterId());

        try {
            SessionResponseDto response = sessionService.createSession(dto.getPrinterId());
            return ResponseEntity.ok(ApiResponseDto.success("Session created successfully", response));

        } catch (Exception e) {
            log.error("Failed to create session", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponseDto.error(e.getMessage()));
        }
    }

    @GetMapping("/{sessionId}")
    public ResponseEntity<ApiResponseDto<SessionResponseDto>> getSession(
            @PathVariable String sessionId) {

        log.info("Getting session details: {}", sessionId);

        try {
            SessionResponseDto response = sessionService.getSessionDetails(sessionId);
            return ResponseEntity.ok(ApiResponseDto.success(response));

        } catch (Exception e) {
            log.error("Failed to get session", e);
            return ResponseEntity.status(404)
                    .body(ApiResponseDto.error("Session not found"));
        }
    }

    @PostMapping("/{sessionId}/upload-request")
    public ResponseEntity<ApiResponseDto<UploadResponseDto>> requestUploadUrl(
            @PathVariable String sessionId,
            @Valid @RequestBody UploadRequestDto dto) {

        log.info("Requesting upload URL for session: {}", sessionId);

        try {
            UploadResponseDto response = sessionService.generateUploadUrl(sessionId, dto);
            return ResponseEntity.ok(ApiResponseDto.success("Upload URL generated", response));

        } catch (Exception e) {
            log.error("Failed to generate upload URL", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponseDto.error(e.getMessage()));
        }
    }

    @GetMapping("/{sessionId}/status")
    public ResponseEntity<ApiResponseDto<SessionStatusDto>> getSessionStatus(
            @PathVariable String sessionId) {

        log.info("Getting session status: {}", sessionId);

        try {
            SessionStatusDto response = sessionService.getSessionStatus(sessionId);
            return ResponseEntity.ok(ApiResponseDto.success(response));

        } catch (Exception e) {
            log.error("Failed to get session status", e);
            return ResponseEntity.status(404)
                    .body(ApiResponseDto.error("Session not found"));
        }
    }

    @DeleteMapping("/{sessionId}")
    public ResponseEntity<ApiResponseDto<String>> deleteSession(
            @PathVariable String sessionId) {

        log.info("Deleting session: {}", sessionId);

        try {
            sessionService.deleteSession(sessionId);
            return ResponseEntity.ok(ApiResponseDto.success("Session deleted", "OK"));

        } catch (Exception e) {
            log.error("Failed to delete session", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponseDto.error(e.getMessage()));
        }
    }
}