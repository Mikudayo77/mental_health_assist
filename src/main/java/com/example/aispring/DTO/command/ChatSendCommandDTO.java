package com.example.aispring.DTO.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChatSendCommandDTO {

    @NotNull(message = "预约ID不能为空")
    private Long appointmentId;

    @NotBlank(message = "消息内容不能为空")
    @Size(max = 2000, message = "消息长度不能超过2000个字符")
    private String content;
}
