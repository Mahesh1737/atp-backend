package com.atp.printing.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrinterRegistrationDto {
    @NotBlank(message = "Printer name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    private String name;

    @Size(max = 200, message = "Location must not exceed 200 characters")
    private String location;

    @Min(value = 0, message = "Price must be positive")
    private Double pricePerPageBw = 2.50;

    @Min(value = 0, message = "Price must be positive")
    private Double pricePerPageColor = 5.00;

    private Boolean colorSupported = true;
}
