package com.example.aispring.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("counseling_crisis_record")
@Builder
public class CrisisRecord {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    // 来源 1AI聊天 2人工聊天
    @TableField("source_type")
    private Integer sourceType;

    // 来源ID(AI会话ID或预约ID)
    @TableField("source_id")
    private Long sourceId;

    @TableField("message_content")
    private String messageContent;

    @TableField("matched_words")
    private String matchedWords;

    @TableField("risk_level")
    private String riskLevel;

    @TableField("ai_analysis")
    private String aiAnalysis;

    // 处理状态 0待处理 1已处理
    @TableField("handle_status")
    private Integer handleStatus;

    @TableField("handled_by")
    private Long handledBy;

    @TableField("handled_at")
    private LocalDateTime handledAt;

    @TableField("handle_remark")
    private String handleRemark;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
