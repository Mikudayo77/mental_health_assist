package com.example.aispring.controller;

import com.example.aispring.DTO.command.ArticleCommandDTO;
import com.example.aispring.DTO.response.ArticleResponseDTO;
import com.example.aispring.common.PageResult;
import com.example.aispring.common.Result;
import com.example.aispring.common.ResultCode;
import com.example.aispring.entity.KnowledgeCategory;
import com.example.aispring.enumClass.UserType;
import com.example.aispring.service.KnowledgeService;
import com.example.aispring.util.JwtTokenUtil;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 知识库：分类树 + 文章增删改查
 * 路径与前端 src/api/admin.js、src/api/frontend.js 一一对应
 */
@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeController {

    @Autowired
    private KnowledgeService knowledgeService;

    /**
     * 分类树（前端按 id / categoryName 消费）
     */
    @GetMapping("/category/tree")
    public Result<List<KnowledgeCategory>> categoryTree() {
        return Result.ok(knowledgeService.listCategories());
    }

    /**
     * 文章分页。前端参数名为 currentPage / size（不是 pageNum / pageSize）
     */
    @GetMapping("/article/page")
    public Result<PageResult<ArticleResponseDTO>> articlePage(
            @RequestParam(value = "currentPage", defaultValue = "1") Integer currentPage,
            @RequestParam(value = "size", defaultValue = "10") Integer size,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "categoryId", required = false) Long categoryId,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "sortField", required = false) String sortField,
            @RequestParam(value = "sortDirection", required = false) String sortDirection) {
        return Result.ok(knowledgeService.pageArticles(currentPage, size, title, categoryId,
                status, sortField, sortDirection, isAdmin()));
    }

    /**
     * 文章详情。非管理员只能看到已发布文章；读者访问时阅读量 +1，管理员预览不计数
     */
    @GetMapping("/article/{id}")
    public Result<ArticleResponseDTO> articleDetail(@PathVariable("id") String id) {
        ArticleResponseDTO article = knowledgeService.getArticle(id, true);
        if (article == null) {
            return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "文章不存在", null);
        }
        boolean admin = isAdmin();
        if (!admin && !Integer.valueOf(1).equals(article.getStatus())) {
            // 草稿与已下线文章对外一律视为不存在，避免通过猜 ID 看到未发布内容
            return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "文章不存在", null);
        }
        if (!admin) {
            knowledgeService.increaseReadCount(id);
        }
        return Result.ok(article);
    }

    /**
     * 新增文章（前端会把 UUID 放在 id 字段里传上来）
     */
    @PostMapping("/article")
    public Result<String> createArticle(@Valid @RequestBody ArticleCommandDTO dto) {
        Long authorId = JwtTokenUtil.getCurrentUserId();
        return Result.ok(knowledgeService.createArticle(dto, authorId));
    }

    /**
     * 编辑文章
     */
    @PutMapping("/article/{id}")
    public Result<Void> updateArticle(@PathVariable("id") String id, @Valid @RequestBody ArticleCommandDTO dto) {
        if (!knowledgeService.updateArticle(id, dto)) {
            return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "文章不存在", null);
        }
        return Result.ok();
    }

    /**
     * 发布 / 下线，请求体 {"status": 1}
     */
    @PutMapping("/article/{id}/status")
    public Result<Void> changeStatus(@PathVariable("id") String id, @RequestBody Map<String, Object> body) {
        Object raw = body == null ? null : body.get("status");
        if (!(raw instanceof Number number)) {
            return Result.error(ResultCode.PARAM_ERROR.getCode(), "status 参数不合法", null);
        }
        if (!knowledgeService.changeStatus(id, number.intValue())) {
            return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "文章不存在", null);
        }
        return Result.ok();
    }

    /**
     * 删除文章
     */
    @DeleteMapping("/article/{id}")
    public Result<Void> deleteArticle(@PathVariable("id") String id) {
        if (!knowledgeService.deleteArticle(id)) {
            return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "文章不存在", null);
        }
        return Result.ok();
    }

    /**
     * 是否管理员（JWT 过滤器按 ROLE_ + userType 授权，2 为管理员）。
     * 注意不能用 "authentication == null" 判断匿名：Spring Security 的
     * AnonymousAuthenticationFilter 会给匿名请求塞一个匿名 token，永远不为 null。
     */
    private boolean isAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        String adminAuthority = "ROLE_" + UserType.ADMIN.getCode();
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> adminAuthority.equals(authority.getAuthority()));
    }
}
