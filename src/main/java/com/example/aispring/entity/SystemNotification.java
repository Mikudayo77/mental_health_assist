package com.example.aispring.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("counseling_notification")
@Builder
public class SystemNotification {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    private String title;

    private String content;

    // 类型 1预约 2退款 3危机 4审核
    private Integer type;

    @TableField("biz_no")
    private String bizNo;

    // 0未读 1已读
    @TableField("read_flag")
    private Integer readFlag;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
