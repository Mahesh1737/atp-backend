package com.atp.printing.service;

import com.atp.printing.dto.*;
import com.atp.printing.entity.Printer;
import com.atp.printing.entity.Session;
import com.atp.printing.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SessionService {

    private final SessionRepository sessionRepository;
    private final PrinterService printerService;
    private final CloudinaryService cloudinaryService;
    private final QRCodeService qrCodeService;

    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Value("${app.session.expiration-minutes}")
    private Integer sessionExpirationMinutes;

    @Transactional
    public SessionResponseDto createSession(Long printerId) {
        log.info("Creating session for printer: {}", printerId);

        Printer printer = printerService.findById(printerId);
        String sessionId = UUID.randomUUID().toString();

        Session session = Session.builder()
                .sessionId(sessionId)
                .printer(printer)
                .status(Session.SessionStatus.ACTIVE)
                .paymentStatus(Session.PaymentStatus.PENDING)
                .expiresAt(LocalDateTime.now().plusMinutes(sessionExpirationMinutes))
                .build();

        session = sessionRepository.save(session);

        // Generate QR code URL
        String qrUrl = frontendUrl + "/session/" + sessionId;

        log.info("Session created with ID: {}", sessionId);

        return SessionResponseDto.builder()
                .id(session.getId())
                .sessionId(sessionId)
                .printerName(printer.getName())
                .pricePerPage(printer.getPricePerPageBw())
                .colorOptions(printer.getColorSupported() ?
                        Arrays.asList("BW", "Color") : Arrays.asList("BW"))
                .expiresAt(session.getExpiresAt())
                .status(session.getStatus().name())
                .qrUrl(qrUrl)
                .build();
    }

    public SessionResponseDto getSessionDetails(String sessionId) {
        Session session = sessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        Printer printer = session.getPrinter();

        return SessionResponseDto.builder()
                .id(session.getId())
                .sessionId(sessionId)
                .printerName(printer.getName())
                .pricePerPage(printer.getPricePerPageBw())
                .colorOptions(printer.getColorSupported() ?
                        Arrays.asList("BW", "Color") : Arrays.asList("BW"))
                .expiresAt(session.getExpiresAt())
                .status(session.getStatus().name())
                .build();
    }

    @Transactional
    public UploadResponseDto uploadFile(String sessionId, MultipartFile file,
                                        Integer pageCount, Session.ColorMode colorMode) throws IOException {
        log.info("Uploading file for session: {}", sessionId);

        Session session = sessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        if (session.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Session has expired");
        }

        // Upload file to Cloudinary
        Map<String, Object> uploadResult = cloudinaryService.uploadFile(file);
        String cloudinaryUrl = (String) uploadResult.get("secure_url");
        String publicId = (String) uploadResult.get("public_id");

        // Calculate amount
        Printer printer = session.getPrinter();
        double pricePerPage = colorMode == Session.ColorMode.COLOR ?
                printer.getPricePerPageColor() : printer.getPricePerPageBw();
        double amount = pageCount * pricePerPage;

        // Generate QR code and upload to Cloudinary
        String qrData = frontendUrl + "/session/" + sessionId;
        byte[] qrCodeBytes = qrCodeService.generateQRCode(qrData);
        Map<String, Object> qrUploadResult = cloudinaryService.uploadFile(
                qrCodeBytes,
                "qr-" + sessionId + ".png"
        );
        String qrCodeUrl = (String) qrUploadResult.get("secure_url");

        // Update session
        session.setFileName(file.getOriginalFilename());
        session.setFileUrl(cloudinaryUrl);
        session.setFileKey(publicId);
        session.setPageCount(pageCount);
        session.setColorMode(colorMode);
        session.setAmount(amount);
        session.setStatus(Session.SessionStatus.UPLOADED);
        sessionRepository.save(session);

        log.info("File uploaded successfully to Cloudinary: {}", cloudinaryUrl);

        return UploadResponseDto.builder()
                .uploadUrl(cloudinaryUrl)
                .fileKey(publicId)
                .expiresIn(0L) // Not applicable for Cloudinary
                .build();
    }

    public SessionStatusDto getSessionStatus(String sessionId) {
        Session session = sessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        return SessionStatusDto.builder()
                .sessionId(sessionId)
                .status(session.getStatus().name())
                .paymentStatus(session.getPaymentStatus().name())
                .fileName(session.getFileName())
                .pageCount(session.getPageCount())
                .amount(session.getAmount())
                .createdAt(session.getCreatedAt())
                .updatedAt(session.getUpdatedAt())
                .build();
    }

    @Transactional
    public void updateSessionStatus(String sessionId, Session.SessionStatus status) {
        Session session = sessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        session.setStatus(status);
        sessionRepository.save(session);

        log.info("Updated session {} status to {}", sessionId, status);
    }

    @Transactional
    public void updatePaymentStatus(String sessionId, Session.PaymentStatus paymentStatus) {
        Session session = sessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        session.setPaymentStatus(paymentStatus);
        if (paymentStatus == Session.PaymentStatus.PAID) {
            session.setStatus(Session.SessionStatus.PAID);
        }
        sessionRepository.save(session);

        log.info("Updated session {} payment status to {}", sessionId, paymentStatus);
    }

    public Session findBySessionId(String sessionId) {
        return sessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));
    }

    @Transactional
    public void cleanupExpiredSessions() {
        List<Session> expiredSessions = sessionRepository.findExpiredSessions(LocalDateTime.now());

        for (Session session : expiredSessions) {
            // Delete file from Cloudinary if exists
            if (session.getFileKey() != null) {
                try {
                    cloudinaryService.deleteFile(session.getFileKey());
                } catch (Exception e) {
                    log.error("Failed to delete file for expired session: {}", session.getSessionId(), e);
                }
            }

            // Mark session as expired
            session.setStatus(Session.SessionStatus.EXPIRED);
            sessionRepository.save(session);
        }

        if (!expiredSessions.isEmpty()) {
            log.info("Cleaned up {} expired sessions", expiredSessions.size());
        }
    }

    @Transactional
    public void deleteSession(String sessionId) {
        Session session = findBySessionId(sessionId);

        // Delete file from Cloudinary
        if (session.getFileKey() != null) {
            try {
                cloudinaryService.deleteFile(session.getFileKey());
            } catch (Exception e) {
                log.error("Failed to delete file for session: {}", sessionId, e);
            }
        }

        sessionRepository.delete(session);
        log.info("Deleted session: {}", sessionId);
    }
}