package com.example.aispring.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识库分类
 */
@Data
@TableName("knowledge_category")
public class KnowledgeCategory {
    @TableId(type = IdType.AUTO)
    private Long id;

    // 分类名称，前端用 categoryName 渲染标签
    private String categoryName;

    // 父分类ID，0 表示顶级
    private Long parentId;

    // 排序值
    private Integer sort;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
