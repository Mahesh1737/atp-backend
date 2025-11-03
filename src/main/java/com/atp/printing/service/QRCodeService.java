package com.atp.printing.service;

import lombok.extern.slf4j.Slf4j;
import net.glxn.qrgen.javase.QRCode;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.Base64;

@Slf4j
@Service
public class QRCodeService {

    /**
     * Generate QR code as byte array
     */
    public byte[] generateQRCodeBytes(String data, int width, int height) {
        log.debug("Generating QR code for data: {}", data);

        try {
            ByteArrayOutputStream stream = QRCode.from(data)
                    .withSize(width, height)
                    .stream();

            return stream.toByteArray();
        } catch (Exception e) {
            log.error("Failed to generate QR code", e);
            throw new RuntimeException("Failed to generate QR code", e);
        }
    }

    /**
     * Generate QR code as Base64 string
     */
    public String generateQRCodeBase64(String data, int width, int height) {
        byte[] qrBytes = generateQRCodeBytes(data, width, height);
        return Base64.getEncoder().encodeToString(qrBytes);
    }

    /**
     * Generate QR code as data URI
     */
    public String generateQRCodeDataUri(String data, int width, int height) {
        String base64 = generateQRCodeBase64(data, width, height);
        return "data:image/png;base64," + base64;
    }

    /**
     * Generate QR code with default size (300x300)
     */
    public byte[] generateQRCode(String data) {
        return generateQRCodeBytes(data, 300, 300);
    }
}