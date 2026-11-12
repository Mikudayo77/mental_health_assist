package com.example.aispring.DTO.command;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AppointmentCreateCommandDTO {

    @NotNull(message = "咨询师不能为空")
    private Long counselorId;

    @NotNull(message = "套餐不能为空")
    private Long packageId;

    @NotNull(message = "预约开始时间不能为空")
    @Future(message = "预约时间必须在未来")
    private LocalDateTime appointmentStart;
}
