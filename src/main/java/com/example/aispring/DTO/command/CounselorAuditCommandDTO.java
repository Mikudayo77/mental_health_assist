package com.example.aispring.DTO.command;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CounselorAuditCommandDTO {

    @NotNull(message = "咨询师资料ID不能为空")
    private Long counselorId;

    @NotNull(message = "审核结果不能为空")
    private Integer auditStatus;

    @Size(max = 255, message = "审核备注长度不能超过255个字符")
    private String auditRemark;
}
