package com.atp.printing.service;

import com.atp.printing.dto.PrinterRegistrationDto;
import com.atp.printing.dto.PrinterResponseDto;
import com.atp.printing.entity.Printer;
import com.atp.printing.repository.PrinterRepository;
import com.atp.printing.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PrinterService {

    private final PrinterRepository printerRepository;
    private final JwtUtil jwtUtil;

    @Transactional
    public PrinterResponseDto registerPrinter(PrinterRegistrationDto dto) {
        log.info("Registering new printer: {}", dto.getName());

        Printer printer = Printer.builder()
                .name(dto.getName())
                .location(dto.getLocation())
                .pricePerPageBw(dto.getPricePerPageBw())
                .pricePerPageColor(dto.getPricePerPageColor())
                .colorSupported(dto.getColorSupported())
                .status(Printer.PrinterStatus.OFFLINE)
                .build();

        printer = printerRepository.save(printer);

        // Generate JWT token for printer
        String jwtToken = jwtUtil.generatePrinterToken(printer.getId(), printer.getName());
        printer.setJwtToken(jwtToken);
        printer = printerRepository.save(printer);

        log.info("Printer registered successfully with ID: {}", printer.getId());

        return PrinterResponseDto.builder()
                .id(printer.getId())
                .name(printer.getName())
                .location(printer.getLocation())
                .jwtToken(jwtToken)
                .status(printer.getStatus().name())
                .createdAt(printer.getCreatedAt())
                .build();
    }

    @Transactional
    public void updateHeartbeat(String token) {
        Long printerId = jwtUtil.extractPrinterId(token);
        Printer printer = printerRepository.findById(printerId)
                .orElseThrow(() -> new RuntimeException("Printer not found"));

        printer.setLastHeartbeat(LocalDateTime.now());
        printer.setStatus(Printer.PrinterStatus.ONLINE);
        printerRepository.save(printer);

        log.debug("Updated heartbeat for printer: {}", printerId);
    }

    @Transactional
    public void updateStatus(Long printerId, Printer.PrinterStatus status) {
        Printer printer = printerRepository.findById(printerId)
                .orElseThrow(() -> new RuntimeException("Printer not found"));

        printer.setStatus(status);
        printerRepository.save(printer);

        log.info("Updated printer {} status to {}", printerId, status);
    }

    public Printer findByToken(String token) {
        return printerRepository.findByJwtToken(token)
                .orElseThrow(() -> new RuntimeException("Printer not found"));
    }

    public Printer findById(Long id) {
        return printerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Printer not found with ID: " + id));
    }

    public List<Printer> findOnlinePrinters() {
        return printerRepository.findByStatus(Printer.PrinterStatus.ONLINE);
    }

    @Transactional
    public void markInactivePrinters(int timeoutMinutes) {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(timeoutMinutes);
        List<Printer> inactivePrinters = printerRepository.findInactivePrinters(threshold);

        for (Printer printer : inactivePrinters) {
            if (printer.getStatus() == Printer.PrinterStatus.ONLINE) {
                printer.setStatus(Printer.PrinterStatus.OFFLINE);
                printerRepository.save(printer);
                log.warn("Marked printer {} as OFFLINE due to inactivity", printer.getId());
            }
        }
    }
}