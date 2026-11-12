package com.example.aispring.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("counseling_counselor")
@Builder
public class Counselor {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("real_name")
    private String realName;

    private Integer gender;

    private String phone;

    private String email;

    @TableField("qualification_no")
    private String qualificationNo;

    @TableField("certification_file_url")
    private String certificationFileUrl;

    private String specialty;

    private String introduction;

    @TableField("years_of_experience")
    private Integer yearsOfExperience;

    // 审核状态 0待审核 1通过 2拒绝
    @TableField("audit_status")
    private Integer auditStatus;

    @TableField("audit_remark")
    private String auditRemark;

    @TableField("audited_by")
    private Long auditedBy;

    @TableField("audited_at")
    private LocalDateTime auditedAt;

    // 启用状态 1启用 0禁用
    private Integer status;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
