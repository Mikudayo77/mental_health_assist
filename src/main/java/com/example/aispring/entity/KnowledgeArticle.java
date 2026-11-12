package com.example.aispring.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识库文章
 * id 由前端生成（crypto.randomUUID()），所以用 INPUT 策略而不是自增
 */
@Data
@Builder
@TableName("knowledge_article")
public class KnowledgeArticle {
    @TableId(type = IdType.INPUT)
    private String id;

    private String title;

    private Long categoryId;

    // 封面图相对路径，前端用 fileBaseUrl + coverImage 拼接
    private String coverImage;

    private String summary;

    // 富文本 HTML
    private String content;

    // 标签，逗号分隔
    private String tags;

    private Long authorId;

    // 前端列表和详情都显示 authorName
    private String authorName;

    // 0草稿 1已发布 2已下线
    private Integer status;

    // 前端列表显示 readCount
    private Integer readCount;

    private LocalDateTime publishedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
