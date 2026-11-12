package com.example.aispring.DTO.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class NotificationResponseDTO {
    private Long id;
    private String title;
    private String content;
    private Integer type;
    private String typeDesc;
    private String bizNo;
    private Integer readFlag;
    private LocalDateTime createdAt;
}
