package com.example.aispring.DTO.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class PaymentResponseDTO {
    private String paymentNo;
    private String orderNo;
    private BigDecimal amount;
    private Integer payMethod;
    private Integer status;
    private String transactionNo;
    private LocalDateTime paidAt;
}
