package com.example.aispring.DTO.command;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RefundApplyCommandDTO {

    @NotNull(message = "订单ID不能为空")
    private Long orderId;

    @Size(max = 255, message = "退款原因长度不能超过255个字符")
    private String reason;
}
