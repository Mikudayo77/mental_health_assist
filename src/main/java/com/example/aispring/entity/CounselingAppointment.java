package com.example.aispring.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("counseling_appointment")
@Builder
public class CounselingAppointment {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("appointment_no")
    private String appointmentNo;

    @TableField("user_id")
    private Long userId;

    // 咨询师资料ID(counseling_counselor.id)
    @TableField("counselor_id")
    private Long counselorId;

    @TableField("package_id")
    private Long packageId;

    @TableField("appointment_start")
    private LocalDateTime appointmentStart;

    @TableField("appointment_end")
    private LocalDateTime appointmentEnd;

    // 状态 0待确认 1已确认 2已完成 3已取消 4已拒绝
    private Integer status;

    @TableField("cancel_reason")
    private String cancelReason;

    @TableField("confirmed_at")
    private LocalDateTime confirmedAt;

    @TableField("completed_at")
    private LocalDateTime completedAt;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
