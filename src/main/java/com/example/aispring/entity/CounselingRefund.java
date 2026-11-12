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
@TableName("counseling_refund")
@Builder
public class CounselingRefund {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("refund_no")
    private String refundNo;

    @TableField("order_id")
    private Long orderId;

    @TableField("order_no")
    private String orderNo;

    @TableField("user_id")
    private Long userId;

    @TableField("refund_amount")
    private BigDecimal refundAmount;

    @TableField("rule_desc")
    private String ruleDesc;

    // 状态 0待审核 1已退款 2已拒绝
    private Integer status;

    private String reason;

    @TableField("reject_reason")
    private String rejectReason;

    // 审核人 0=系统自动
    @TableField("audited_by")
    private Long auditedBy;

    @TableField("applied_at")
    private LocalDateTime appliedAt;

    @TableField("audited_at")
    private LocalDateTime auditedAt;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
