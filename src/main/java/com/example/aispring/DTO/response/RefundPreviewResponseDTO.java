package com.example.aispring.DTO.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class RefundPreviewResponseDTO {
    private boolean refundable;
    private BigDecimal refundAmount;
    private String ruleDesc;
    private String rejectReason;
}
