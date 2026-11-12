package com.example.aispring.DTO.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 新增/编辑文章请求体，字段与前端 ArticleDialog.vue 提交的数据一致
 */
@Data
public class ArticleCommandDTO {

    // 前端新建时传 crypto.randomUUID()，编辑时不使用该字段
    private String id;

    @NotBlank(message = "文章标题不能为空")
    @Size(max = 200, message = "文章标题最多200个字符")
    private String title;

    private Long categoryId;

    // 封面图相对路径
    private String coverImage;

    @Size(max = 1000, message = "摘要最多1000个字符")
    private String summary;

    @NotBlank(message = "文章内容不能为空")
    private String content;

    // 逗号分隔的标签字符串
    private String tags;
}
