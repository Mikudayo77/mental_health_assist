package com.example.aispring.DTO.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ChatMessageResponseDTO {
    private Long id;
    private Long appointmentId;
    private Long senderId;
    private Integer senderType;
    private String senderTypeDesc;
    private String content;
    private Integer crisisFlag;
    private LocalDateTime createdAt;
}
