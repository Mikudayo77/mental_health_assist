package com.example.aispring.DTO.command;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CrisisHandleCommandDTO {

    @NotNull(message = "危机记录ID不能为空")
    private Long crisisId;

    @NotNull(message = "处理状态不能为空")
    private Integer handleStatus;

    @Size(max = 255, message = "处理备注长度不能超过255个字符")
    private String handleRemark;
}
