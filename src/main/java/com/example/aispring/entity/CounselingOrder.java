package com.example.aispring.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("counseling_order")
@Builder
public class CounselingOrder {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("order_no")
    private String orderNo;

    @TableField("user_id")
    private Long userId;

    @TableField("appointment_id")
    private Long appointmentId;

    @TableField("package_id")
    private Long packageId;

    // 套餐名称快照
    @TableField("package_name")
    private String packageName;

    // 套餐时长快照(分钟)
    @TableField("package_duration")
    private Integer packageDuration;

    private BigDecimal amount;

    @TableField("refunded_amount")
    private BigDecimal refundedAmount;

    // 状态 0未支付 1已支付 2退款中 3已退款 4已取消
    private Integer status;

    @TableField("paid_at")
    private LocalDateTime paidAt;

    @TableField("canceled_at")
    private LocalDateTime canceledAt;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
