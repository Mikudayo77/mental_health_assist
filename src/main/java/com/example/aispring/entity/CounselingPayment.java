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
@TableName("counseling_payment")
@Builder
public class CounselingPayment {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("payment_no")
    private String paymentNo;

    @TableField("order_id")
    private Long orderId;

    @TableField("order_no")
    private String orderNo;

    @TableField("user_id")
    private Long userId;

    private BigDecimal amount;

    // 支付方式 1模拟支付
    @TableField("pay_method")
    private Integer payMethod;

    // 状态 1成功 0失败
    private Integer status;

    @TableField("transaction_no")
    private String transactionNo;

    @TableField("paid_at")
    private LocalDateTime paidAt;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
