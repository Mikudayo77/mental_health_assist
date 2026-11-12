package com.example.aispring.DTO.command;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CounselorApplyCommandDTO {

    @NotBlank(message = "真实姓名不能为空")
    @Size(min = 2, max = 50, message = "真实姓名长度必须在2到50个字符之间")
    private String realName;

    private Integer gender;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @Email(message = "邮箱格式不正确")
    @Size(max = 100, message = "邮箱长度不能超过100个字符")
    private String email;

    @NotBlank(message = "资质证书编号不能为空")
    @Size(max = 64, message = "资质证书编号长度不能超过64个字符")
    private String qualificationNo;

    @Size(max = 255, message = "证书附件URL长度不能超过255个字符")
    private String certificationFileUrl;

    @Size(max = 255, message = "擅长领域长度不能超过255个字符")
    private String specialty;

    @Size(max = 2000, message = "个人简介长度不能超过2000个字符")
    private String introduction;

    @Min(value = 0, message = "从业年限不能小于0")
    @Max(value = 60, message = "从业年限不能超过60")
    private Integer yearsOfExperience;
}
