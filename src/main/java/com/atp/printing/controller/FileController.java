package com.atp.printing.controller;

import com.atp.printing.dto.ApiResponseDto;
import com.atp.printing.dto.UploadResponseDto;
import com.atp.printing.entity.Session;
import com.atp.printing.service.SessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class FileController {

    private final SessionService sessionService;

    @PostMapping("/upload")
    public ResponseEntity<ApiResponseDto<Map<String, Object>>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("sessionId") String sessionId,
            @RequestParam("pageCount") Integer pageCount,
            @RequestParam("colorMode") String colorMode) {

        log.info("File upload request for session: {}", sessionId);

        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponseDto.error("Please select a file to upload"));
        }

        // Validate file size (10MB)
        if (file.getSize() > 10 * 1024 * 1024) {
            return ResponseEntity.badRequest()
                    .body(ApiResponseDto.error("File size must be less than 10MB"));
        }

        // Validate file type
        String contentType = file.getContentType();
        if (contentType == null || !isValidFileType(contentType)) {
            return ResponseEntity.badRequest()
                    .body(ApiResponseDto.error("Invalid file type. Allowed: PDF, DOCX, JPG, PNG"));
        }

        try {
            Session.ColorMode mode = Session.ColorMode.valueOf(colorMode.toUpperCase());
            UploadResponseDto uploadResponse = sessionService.uploadFile(sessionId, file, pageCount, mode);

            // Get updated session to include QR code URL
            com.atp.printing.entity.Session session = sessionService.findBySessionId(sessionId);

            Map<String, Object> response = new HashMap<>();
            response.put("cloudinaryUrl", uploadResponse.getUploadUrl());
            response.put("fileName", file.getOriginalFilename());
            response.put("fileSize", file.getSize());
            response.put("pageCount", pageCount);
            response.put("colorMode", colorMode);
            response.put("amount", session.getAmount());
            response.put("sessionId", sessionId);
            response.put("qrCodeUrl", session.getFileUrl()); // QR code URL stored separately

            return ResponseEntity.ok(
                    ApiResponseDto.success("File uploaded successfully", response)
            );

        } catch (Exception e) {
            log.error("File upload failed", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponseDto.error("Failed to upload file: " + e.getMessage()));
        }
    }

    @GetMapping("/files/{sessionId}")
    public ResponseEntity<ApiResponseDto<Map<String, Object>>> getFileDetails(
            @PathVariable String sessionId) {

        log.info("Getting file details for session: {}", sessionId);

        try {
            Session session = sessionService.findBySessionId(sessionId);

            Map<String, Object> fileDetails = new HashMap<>();
            fileDetails.put("fileName", session.getFileName());
            fileDetails.put("fileUrl", session.getFileUrl());
            fileDetails.put("pageCount", session.getPageCount());
            fileDetails.put("colorMode", session.getColorMode());
            fileDetails.put("amount", session.getAmount());
            fileDetails.put("status", session.getStatus().name());
            fileDetails.put("paymentStatus", session.getPaymentStatus().name());

            return ResponseEntity.ok(ApiResponseDto.success(fileDetails));

        } catch (Exception e) {
            log.error("Failed to get file details", e);
            return ResponseEntity.status(404)
                    .body(ApiResponseDto.error("File not found"));
        }
    }

    private boolean isValidFileType(String contentType) {
        return contentType.equals("application/pdf") ||
                contentType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document") ||
                contentType.equals("image/jpeg") ||
                contentType.equals("image/jpg") ||
                contentType.equals("image/png");
    }
}