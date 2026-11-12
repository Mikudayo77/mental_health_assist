package com.example.aispring.DTO.command;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PackageCommandDTO {

    @NotBlank(message = "套餐名称不能为空")
    @Size(max = 100, message = "套餐名称长度不能超过100个字符")
    private String name;

    @Size(max = 500, message = "套餐描述长度不能超过500个字符")
    private String description;

    @NotNull(message = "咨询时长不能为空")
    @Min(value = 10, message = "咨询时长不能少于10分钟")
    @Max(value = 240, message = "咨询时长不能超过240分钟")
    private Integer durationMinutes;

    @NotNull(message = "价格不能为空")
    @DecimalMin(value = "0.01", message = "价格必须大于0")
    @Digits(integer = 8, fraction = 2, message = "价格格式不正确")
    private BigDecimal price;

    private Integer status;
}
