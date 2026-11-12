package com.example.aispring.DTO.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class PackageResponseDTO {
    private Long id;
    private String name;
    private String description;
    private Integer durationMinutes;
    private BigDecimal price;
    private Integer status;
    private String statusDesc;
    private LocalDateTime createdAt;
}
