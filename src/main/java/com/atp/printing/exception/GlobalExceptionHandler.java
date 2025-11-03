package com.atp.printing.exception;

import com.atp.printing.dto.ApiResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponseDto<Map<String, String>>> handleValidationExceptions(
            MethodArgumentNotValidException ex) {

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        log.warn("Validation errors: {}", errors);

        return ResponseEntity.badRequest()
                .body(ApiResponseDto.<Map<String, String>>builder()
                        .success(false)
                        .message("Validation failed")
                        .data(errors)
                        .build());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponseDto<Void>> handleResourceNotFoundException(
            ResourceNotFoundException ex) {

        log.error("Resource not found: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponseDto.<Void>error(ex.getMessage()));
    }

    @ExceptionHandler(PaymentException.class)
    public ResponseEntity<ApiResponseDto<Void>> handlePaymentException(
            PaymentException ex) {

        log.error("Payment error: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponseDto.<Void>error(ex.getMessage()));
    }

    @ExceptionHandler(SessionExpiredException.class)
    public ResponseEntity<ApiResponseDto<Void>> handleSessionExpiredException(
            SessionExpiredException ex) {

        log.error("Session expired: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.GONE)
                .body(ApiResponseDto.<Void>error(ex.getMessage()));
    }

    @ExceptionHandler(PrinterOfflineException.class)
    public ResponseEntity<ApiResponseDto<Void>> handlePrinterOfflineException(
            PrinterOfflineException ex) {

        log.error("Printer offline: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponseDto.<Void>error(ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponseDto<Void>> handleGenericException(Exception ex) {
        log.error("Unexpected error", ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponseDto.<Void>error("An unexpected error occurred"));
    }
}

// Custom Exceptions
class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}

class PaymentException extends RuntimeException {
    public PaymentException(String message) {
        super(message);
    }
}

class SessionExpiredException extends RuntimeException {
    public SessionExpiredException(String message) {
        super(message);
    }
}

class PrinterOfflineException extends RuntimeException {
    public PrinterOfflineException(String message) {
        super(message);
    }
}