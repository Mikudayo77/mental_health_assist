package com.example.aispring.DTO.command;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CancelOrderCommandDTO {

    @NotBlank(message = "订单号不能为空")
    private String orderNo;
}
