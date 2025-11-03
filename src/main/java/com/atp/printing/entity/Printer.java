package com.atp.printing.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "printers")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Printer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 200)
    private String location;

    @Column(name = "jwt_token", nullable = false, unique = true, length = 500)
    private String jwtToken;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PrinterStatus status = PrinterStatus.OFFLINE;

    @Column(name = "last_heartbeat")
    private LocalDateTime lastHeartbeat;

    @Column(name = "price_per_page_bw")
    private Double pricePerPageBw = 2.50;

    @Column(name = "price_per_page_color")
    private Double pricePerPageColor = 5.00;

    @Column(name = "color_supported")
    private Boolean colorSupported = true;

    @Column(name = "max_pages_per_job")
    private Integer maxPagesPerJob = 100;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum PrinterStatus {
        ONLINE,
        OFFLINE,
        BUSY,
        ERROR
    }
}