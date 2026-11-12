package com.example.aispring.DTO.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class OrderResponseDTO {
    private Long id;
    private String orderNo;
    private Long userId;
    private Long appointmentId;
    private Long packageId;
    private String packageName;
    private Integer packageDuration;
    private BigDecimal amount;
    private BigDecimal refundedAmount;
    private Integer status;
    private String statusDesc;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;
    // 详情接口才填充
    private List<PaymentResponseDTO> payments;
    private List<RefundResponseDTO> refunds;
}
