package com.atp.printing.repository;

import com.atp.printing.entity.Printer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PrinterRepository extends JpaRepository<Printer, Long> {

    /**
     * Find printer by JWT token
     */
    Optional<Printer> findByJwtToken(String jwtToken);

    /**
     * Find all printers by status
     */
    List<Printer> findByStatus(Printer.PrinterStatus status);

    /**
     * Find printers that haven't sent heartbeat since threshold
     */
    @Query("SELECT p FROM Printer p WHERE p.lastHeartbeat < :threshold")
    List<Printer> findInactivePrinters(@Param("threshold") LocalDateTime threshold);

    /**
     * Find printer by name
     */
    Optional<Printer> findByName(String name);

    /**
     * Find all online printers
     */
    @Query("SELECT p FROM Printer p WHERE p.status = 'ONLINE'")
    List<Printer> findAllOnlinePrinters();

    /**
     * Count printers by status
     */
    Long countByStatus(Printer.PrinterStatus status);

    /**
     * Find printers by location
     */
    List<Printer> findByLocationContainingIgnoreCase(String location);

    /**
     * Check if printer exists by name
     */
    boolean existsByName(String name);
}