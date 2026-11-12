package com.example.aispring.DTO.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AppointmentResponseDTO {
    private Long id;
    private String appointmentNo;
    private Long userId;
    private Long counselorId;
    private String counselorName;
    private Long packageId;
    private String packageName;
    private LocalDateTime appointmentStart;
    private LocalDateTime appointmentEnd;
    private Integer status;
    private String statusDesc;
    private String cancelReason;
    private LocalDateTime confirmedAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
}
