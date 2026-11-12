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
@TableName("counseling_package")
@Builder
public class CounselingPackage {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String description;

    @TableField("duration_minutes")
    private Integer durationMinutes;

    private BigDecimal price;

    // 状态 1上架 0下架
    private Integer status;

    @TableField("created_by")
    private Long createdBy;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
