package com.atp.printing.service;

import com.atp.printing.dto.WebSocketMessageDto;
import com.atp.printing.entity.Session;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final S3Service s3Service;

    // Map of printer ID to WebSocket session
    private final Map<Long, WebSocketSession> printerSessions = new ConcurrentHashMap<>();

    public void registerPrinterSession(Long printerId, WebSocketSession session) {
        printerSessions.put(printerId, session);
        log.info("Registered WebSocket session for printer: {}", printerId);
    }

    public void unregisterPrinterSession(Long printerId) {
        printerSessions.remove(printerId);
        log.info("Unregistered WebSocket session for printer: {}", printerId);
    }

    public void sendPrintJob(Session session) {
        Long printerId = session.getPrinter().getId();
        WebSocketSession webSocketSession = printerSessions.get(printerId);

        if (webSocketSession == null || !webSocketSession.isOpen()) {
            log.error("No active WebSocket session for printer: {}", printerId);
            throw new RuntimeException("Printer is offline");
        }

        try {
            // Generate download URL for the file
            String downloadUrl = s3Service.generatePresignedDownloadUrl(session.getFileKey());

            // Create message
            WebSocketMessageDto message = WebSocketMessageDto.builder()
                    .event("print_job")
                    .sessionId(session.getSessionId())
                    .downloadUrl(downloadUrl)
                    .fileName(session.getFileName())
                    .pageCount(session.getPageCount())
                    .colorMode(session.getColorMode().name())
                    .build();

            String jsonMessage = objectMapper.writeValueAsString(message);
            webSocketSession.sendMessage(new TextMessage(jsonMessage));

            log.info("Sent print job to printer {} for session: {}", printerId, session.getSessionId());

        } catch (IOException e) {
            log.error("Failed to send print job to printer: {}", printerId, e);
            throw new RuntimeException("Failed to send print job", e);
        }
    }

    public void sendMessage(Long printerId, String event, Object data) {
        WebSocketSession session = printerSessions.get(printerId);

        if (session == null || !session.isOpen()) {
            log.warn("Cannot send message - no active session for printer: {}", printerId);
            return;
        }

        try {
            WebSocketMessageDto message = WebSocketMessageDto.builder()
                    .event(event)
                    .metadata(data)
                    .build();

            String jsonMessage = objectMapper.writeValueAsString(message);
            session.sendMessage(new TextMessage(jsonMessage));

            log.debug("Sent message to printer {}: {}", printerId, event);

        } catch (IOException e) {
            log.error("Failed to send message to printer: {}", printerId, e);
        }
    }

    public boolean isPrinterOnline(Long printerId) {
        WebSocketSession session = printerSessions.get(printerId);
        return session != null && session.isOpen();
    }

    public int getActivePrinterCount() {
        return (int) printerSessions.values().stream()
                .filter(WebSocketSession::isOpen)
                .count();
    }
}