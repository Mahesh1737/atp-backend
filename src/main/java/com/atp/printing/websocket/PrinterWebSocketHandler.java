package com.atp.printing.websocket;

import com.atp.printing.security.JwtUtil;
import com.atp.printing.service.PrinterService;
import com.atp.printing.service.WebSocketService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class PrinterWebSocketHandler extends TextWebSocketHandler {

    private final JwtUtil jwtUtil;
    private final PrinterService printerService;
    private final WebSocketService webSocketService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Map session ID to printer ID
    private final Map<String, Long> sessionToPrinter = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("WebSocket connection established: {}", session.getId());

        // Extract token from query parameters or headers
        String token = extractToken(session);

        if (token == null || !jwtUtil.validatePrinterToken(token)) {
            log.error("Invalid printer token for session: {}", session.getId());
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Invalid token"));
            return;
        }

        try {
            Long printerId = jwtUtil.extractPrinterId(token);

            // Register printer session
            webSocketService.registerPrinterSession(printerId, session);
            sessionToPrinter.put(session.getId(), printerId);

            // Update printer heartbeat
            printerService.updateHeartbeat(token);

            // Send connection success message
            session.sendMessage(new TextMessage("{\"event\":\"connected\",\"status\":\"success\"}"));

            log.info("Printer {} connected via WebSocket", printerId);

        } catch (Exception e) {
            log.error("Error during WebSocket connection", e);
            session.close(CloseStatus.SERVER_ERROR);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        log.debug("Received WebSocket message: {}", payload);

        try {
            JsonNode jsonNode = objectMapper.readTree(payload);
            String event = jsonNode.get("event").asText();

            Long printerId = sessionToPrinter.get(session.getId());
            if (printerId == null) {
                log.error("No printer ID found for session: {}", session.getId());
                return;
            }

            switch (event) {
                case "heartbeat":
                    handleHeartbeat(printerId, session);
                    break;
                case "print_status":
                    handlePrintStatus(jsonNode);
                    break;
                case "printer_status":
                    handlePrinterStatus(printerId, jsonNode);
                    break;
                default:
                    log.warn("Unknown event type: {}", event);
            }

        } catch (Exception e) {
            log.error("Error handling WebSocket message", e);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        log.info("WebSocket connection closed: {} - {}", session.getId(), status);

        Long printerId = sessionToPrinter.remove(session.getId());
        if (printerId != null) {
            webSocketService.unregisterPrinterSession(printerId);
            log.info("Printer {} disconnected", printerId);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket transport error for session: {}", session.getId(), exception);
        session.close(CloseStatus.SERVER_ERROR);
    }

    private String extractToken(WebSocketSession session) {
        // Try to get token from query parameters
        String query = session.getUri().getQuery();
        if (query != null && query.contains("token=")) {
            String[] params = query.split("&");
            for (String param : params) {
                if (param.startsWith("token=")) {
                    return param.substring(6);
                }
            }
        }

        // Try to get from handshake headers
        var headers = session.getHandshakeHeaders();
        var authHeaders = headers.get("Authorization");
        if (authHeaders != null && !authHeaders.isEmpty()) {
            String authHeader = authHeaders.get(0);
            if (authHeader.startsWith("Bearer ")) {
                return authHeader.substring(7);
            }
        }

        return null;
    }

    private void handleHeartbeat(Long printerId, WebSocketSession session) throws Exception {
        printerService.updateHeartbeat(printerService.findById(printerId).getJwtToken());
        session.sendMessage(new TextMessage("{\"event\":\"heartbeat_ack\"}"));
        log.debug("Heartbeat received from printer: {}", printerId);
    }

    private void handlePrintStatus(JsonNode jsonNode) {
        String sessionId = jsonNode.get("sessionId").asText();
        String status = jsonNode.get("status").asText();

        log.info("Print status update for session {}: {}", sessionId, status);

        // This will be handled by SessionService through the controller
        // The printer agent will call the REST API to update status
    }

    private void handlePrinterStatus(Long printerId, JsonNode jsonNode) {
        String status = jsonNode.get("status").asText();
        log.info("Printer {} status update: {}", printerId, status);
        // Update printer status if needed
    }
}