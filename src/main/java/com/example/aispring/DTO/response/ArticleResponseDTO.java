package com.example.aispring.DTO.response;

import com.example.aispring.entity.KnowledgeArticle;
import cn.hutool.core.util.StrUtil;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 文章响应体，字段覆盖前端三个页面的读取需求：
 * knowledge.vue（title/categoryId/authorName/readCount/updatedAt/status）
 * frontendKnowledge.vue（coverImage/categoryName/title/authorName/readCount/updatedAt）
 * articleDetail.vue（summary/content/tagArray/categoryName/readCount/authorName）
 */
@Data
@Builder
public class ArticleResponseDTO {
    private String id;
    private String title;
    private Long categoryId;
    private String categoryName;
    private String coverImage;
    private String summary;
    private String content;
    private String tags;
    // articleDetail.vue 用 tagArray 渲染标签
    private List<String> tagArray;
    private String authorName;
    private Integer readCount;
    private Integer status;
    private LocalDateTime publishedAt;
    private LocalDateTime updatedAt;
    private LocalDateTime createdAt;

    /**
     * 是否带正文（列表接口不需要 content，避免一次传输大量 HTML）
     */
    public static ArticleResponseDTO from(KnowledgeArticle article, String categoryName, boolean withContent) {
        if (article == null) {
            return null;
        }
        List<String> tagArray = new ArrayList<>();
        if (StrUtil.isNotBlank(article.getTags())) {
            Arrays.stream(article.getTags().split(","))
                    .map(String::trim)
                    .filter(StrUtil::isNotBlank)
                    .forEach(tagArray::add);
        }
        return ArticleResponseDTO.builder()
                .id(article.getId())
                .title(article.getTitle())
                .categoryId(article.getCategoryId())
                .categoryName(categoryName)
                .coverImage(article.getCoverImage())
                .summary(article.getSummary())
                .content(withContent ? article.getContent() : null)
                .tags(article.getTags())
                .tagArray(tagArray)
                .authorName(article.getAuthorName())
                .readCount(article.getReadCount() == null ? 0 : article.getReadCount())
                .status(article.getStatus())
                .publishedAt(article.getPublishedAt())
                .updatedAt(article.getUpdatedAt())
                .createdAt(article.getCreatedAt())
                .build();
    }
}
