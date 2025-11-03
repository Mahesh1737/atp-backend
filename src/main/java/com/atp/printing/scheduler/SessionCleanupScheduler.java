package com.atp.printing.scheduler;

import com.atp.printing.service.PrinterService;
import com.atp.printing.service.SessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SessionCleanupScheduler {

    private final SessionService sessionService;
    private final PrinterService printerService;

    @Value("${app.printer.heartbeat-timeout-minutes}")
    private Integer printerHeartbeatTimeout;

    /**
     * Clean up expired sessions daily at 2 AM
     */
    @Scheduled(cron = "${app.session.cleanup-cron}")
    public void cleanupExpiredSessions() {
        log.info("Starting expired sessions cleanup");

        try {
            sessionService.cleanupExpiredSessions();
            log.info("Expired sessions cleanup completed");
        } catch (Exception e) {
            log.error("Failed to cleanup expired sessions", e);
        }
    }

    /**
     * Check printer heartbeats every 5 minutes
     */
    @Scheduled(cron = "${app.printer.heartbeat-check-cron}")
    public void checkPrinterHeartbeats() {
        log.debug("Checking printer heartbeats");

        try {
            printerService.markInactivePrinters(printerHeartbeatTimeout);
        } catch (Exception e) {
            log.error("Failed to check printer heartbeats", e);
        }
    }

    /**
     * Log system statistics every hour
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void logSystemStatistics() {
        log.info("System statistics - Active printers: {}",
                printerService.findOnlinePrinters().size());
    }
}