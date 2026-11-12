package com.example.aispring.DTO.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CounselorResponseDTO {
    private Long id;
    private Long userId;
    private String realName;
    private Integer gender;
    private String phone;
    private String email;
    private String qualificationNo;
    private String certificationFileUrl;
    private String specialty;
    private String introduction;
    private Integer yearsOfExperience;
    private Integer auditStatus;
    private String auditStatusDesc;
    private String auditRemark;
    private Integer status;
    private LocalDateTime createdAt;
}
