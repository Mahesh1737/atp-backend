package com.atp.printing.dto;

import com.atp.printing.entity.Session;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadRequestDto {
    @NotBlank(message = "Filename is required")
    private String filename;

    @NotBlank(message = "Content type is required")
    private String contentType;

    @NotNull(message = "Page count is required")
    @Min(value = 1, message = "Page count must be at least 1")
    private Integer pageCount;

    @NotNull(message = "Color mode is required")
    private Session.ColorMode colorMode;
}
