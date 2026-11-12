package com.example.aispring.DTO.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CrisisRecordResponseDTO {
    private Long id;
    private Long userId;
    private Integer sourceType;
    private String sourceTypeDesc;
    private Long sourceId;
    private String messageContent;
    private String matchedWords;
    private String riskLevel;
    private String riskLevelDesc;
    private String aiAnalysis;
    private Integer handleStatus;
    private String handleStatusDesc;
    private Long handledBy;
    private LocalDateTime handledAt;
    private String handleRemark;
    private LocalDateTime createdAt;
}
