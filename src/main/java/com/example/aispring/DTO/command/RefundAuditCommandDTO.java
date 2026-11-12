package com.example.aispring.DTO.command;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RefundAuditCommandDTO {

    @NotNull(message = "退款记录ID不能为空")
    private Long refundId;

    @NotNull(message = "审核结果不能为空")
    private Boolean approve;

    @Size(max = 255, message = "拒绝原因长度不能超过255个字符")
    private String rejectReason;
}
