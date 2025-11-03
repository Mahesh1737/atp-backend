package com.atp.printing.repository;

import com.atp.printing.entity.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SessionRepository extends JpaRepository<Session, Long> {

    /**
     * Find session by session ID
     */
    Optional<Session> findBySessionId(String sessionId);

    /**
     * Find all sessions by status
     */
    List<Session> findByStatus(Session.SessionStatus status);

    /**
     * Find expired sessions that are not completed, expired, or failed
     */
    @Query("SELECT s FROM Session s WHERE s.expiresAt < :now AND s.status NOT IN ('COMPLETED', 'EXPIRED', 'FAILED')")
    List<Session> findExpiredSessions(@Param("now") LocalDateTime now);

    /**
     * Find sessions by printer ID and status
     */
    @Query("SELECT s FROM Session s WHERE s.printer.id = :printerId AND s.status = :status")
    List<Session> findByPrinterIdAndStatus(@Param("printerId") Long printerId,
                                           @Param("status") Session.SessionStatus status);

    /**
     * Count sessions by printer since a specific time
     */
    @Query("SELECT COUNT(s) FROM Session s WHERE s.printer.id = :printerId AND s.createdAt > :since")
    Long countSessionsByPrinterSince(@Param("printerId") Long printerId,
                                     @Param("since") LocalDateTime since);

    /**
     * Find all sessions by printer ID
     */
    @Query("SELECT s FROM Session s WHERE s.printer.id = :printerId ORDER BY s.createdAt DESC")
    List<Session> findByPrinterId(@Param("printerId") Long printerId);

    /**
     * Find sessions by payment status
     */
    List<Session> findByPaymentStatus(Session.PaymentStatus paymentStatus);

    /**
     * Find recent sessions (last 24 hours)
     */
    @Query("SELECT s FROM Session s WHERE s.createdAt > :since ORDER BY s.createdAt DESC")
    List<Session> findRecentSessions(@Param("since") LocalDateTime since);

    /**
     * Count active sessions
     */
    @Query("SELECT COUNT(s) FROM Session s WHERE s.status IN ('ACTIVE', 'UPLOADED', 'PAID', 'PRINTING')")
    Long countActiveSessions();

    /**
     * Find sessions that need cleanup (completed/failed more than 24 hours ago)
     */
    @Query("SELECT s FROM Session s WHERE s.status IN ('COMPLETED', 'FAILED') AND s.updatedAt < :threshold")
    List<Session> findSessionsForCleanup(@Param("threshold") LocalDateTime threshold);

    /**
     * Check if session exists by session ID
     */
    boolean existsBySessionId(String sessionId);
}