package com.atp.printing.repository;

import com.atp.printing.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /**
     * Find payment by Razorpay order ID
     */
    Optional<Payment> findByRazorpayOrderId(String razorpayOrderId);

    /**
     * Find payment by Razorpay payment ID
     */
    Optional<Payment> findByRazorpayPaymentId(String razorpayPaymentId);

    /**
     * Find payment by session ID
     */
    Optional<Payment> findBySessionId(Long sessionId);

    /**
     * Find all payments by status
     */
    List<Payment> findByStatus(Payment.PaymentStatus status);

    /**
     * Find payments by session ID ordered by creation date
     */
    @Query("SELECT p FROM Payment p WHERE p.session.id = :sessionId ORDER BY p.createdAt DESC")
    List<Payment> findBySessionIdOrderByCreatedAtDesc(@Param("sessionId") Long sessionId);

    /**
     * Find all successful payments
     */
    @Query("SELECT p FROM Payment p WHERE p.status = 'CAPTURED'")
    List<Payment> findAllSuccessfulPayments();

    /**
     * Find payments within date range
     */
    @Query("SELECT p FROM Payment p WHERE p.createdAt BETWEEN :startDate AND :endDate")
    List<Payment> findPaymentsBetweenDates(@Param("startDate") LocalDateTime startDate,
                                           @Param("endDate") LocalDateTime endDate);

    /**
     * Calculate total revenue
     */
    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.status = 'CAPTURED'")
    Double calculateTotalRevenue();

    /**
     * Calculate revenue for a specific period
     */
    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.status = 'CAPTURED' AND p.completedAt BETWEEN :startDate AND :endDate")
    Double calculateRevenueBetweenDates(@Param("startDate") LocalDateTime startDate,
                                        @Param("endDate") LocalDateTime endDate);

    /**
     * Count payments by status
     */
    Long countByStatus(Payment.PaymentStatus status);

    /**
     * Find failed payments
     */
    @Query("SELECT p FROM Payment p WHERE p.status = 'FAILED' ORDER BY p.createdAt DESC")
    List<Payment> findFailedPayments();

    /**
     * Find pending payments older than specified time
     */
    @Query("SELECT p FROM Payment p WHERE p.status = 'CREATED' AND p.createdAt < :threshold")
    List<Payment> findStalePendingPayments(@Param("threshold") LocalDateTime threshold);
}