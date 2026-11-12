package com.example.aispring.DTO.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class RefundResponseDTO {
    private Long id;
    private String refundNo;
    private Long orderId;
    private String orderNo;
    private BigDecimal refundAmount;
    private String ruleDesc;
    private Integer status;
    private String statusDesc;
    private String reason;
    private String rejectReason;
    private LocalDateTime appliedAt;
    private LocalDateTime auditedAt;
}
