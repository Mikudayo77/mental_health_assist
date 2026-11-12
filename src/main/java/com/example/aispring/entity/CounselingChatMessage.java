package com.example.aispring.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("counseling_chat_message")
@Builder
public class CounselingChatMessage {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("appointment_id")
    private Long appointmentId;

    @TableField("sender_id")
    private Long senderId;

    // 发送者类型 1用户 2咨询师
    @TableField("sender_type")
    private Integer senderType;

    private String content;

    // 危机标记 0无 1命中敏感词
    @TableField("crisis_flag")
    private Integer crisisFlag;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
