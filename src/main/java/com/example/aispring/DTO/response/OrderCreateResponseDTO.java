package com.example.aispring.DTO.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class OrderCreateResponseDTO {
    private String orderNo;
    private String appointmentNo;
    private BigDecimal amount;
}
