package com.example.aispring.service;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.aispring.DTO.command.ArticleCommandDTO;
import com.example.aispring.DTO.response.ArticleResponseDTO;
import com.example.aispring.DTO.response.UserLoginResponseDTO;
import com.example.aispring.common.PageResult;
import com.example.aispring.entity.KnowledgeArticle;
import com.example.aispring.entity.KnowledgeCategory;
import com.example.aispring.mapper.KnowledgeArticleMapper;
import com.example.aispring.mapper.KnowledgeCategoryMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 知识库：分类 + 文章
 */
@Service
public class KnowledgeService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeService.class);

    @Autowired
    private KnowledgeCategoryMapper categoryMapper;
    @Autowired
    private KnowledgeArticleMapper articleMapper;
    @Autowired
    private UserService userService;

    // 允许前端排序的字段白名单，防止 SQL 注入
    private static final Map<String, String> SORT_COLUMNS = Map.of(
            "publishedAt", "published_at",
            "readCount", "read_count",
            "updatedAt", "updated_at",
            "createdAt", "created_at",
            "title", "title"
    );

    /**
     * 分类列表（前端按 id / categoryName 两个字段消费）
     */
    public List<KnowledgeCategory> listCategories() {
        QueryWrapper<KnowledgeCategory> wrapper = new QueryWrapper<>();
        wrapper.orderByAsc("sort").orderByAsc("id");
        return categoryMapper.selectList(wrapper);
    }

    /**
     * 文章分页。
     * admin=true（管理员）按 status 筛选，未筛选则看全部；
     * 其他所有人（匿名访客、已登录的普通用户）一律只能看到已发布文章。
     */
    public PageResult<ArticleResponseDTO> pageArticles(Integer currentPage, Integer size, String title,
                                                       Long categoryId, Integer status, String sortField,
                                                       String sortDirection, boolean admin) {
        int pageNum = (currentPage == null || currentPage < 1) ? 1 : currentPage;
        int pageSize = (size == null || size < 1) ? 10 : Math.min(size, 50);

        QueryWrapper<KnowledgeArticle> wrapper = new QueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(title), "title", title);
        wrapper.eq(categoryId != null, "category_id", categoryId);
        if (admin) {
            wrapper.eq(status != null, "status", status);
        } else {
            // 非管理员只能看已发布，草稿/已下线一律不可见
            wrapper.eq("status", 1);
        }

        // 注意：SORT_COLUMNS 是不可变 Map，Map.of(...).get(null) 会抛 NPE 而不是返回 null
        String column = sortField == null ? null : SORT_COLUMNS.get(sortField);
        if (column == null) {
            wrapper.orderByDesc("updated_at");
        } else if ("asc".equalsIgnoreCase(sortDirection)) {
            wrapper.orderByAsc(column);
        } else {
            wrapper.orderByDesc(column);
        }

        Page<KnowledgeArticle> page;
        try {
            page = articleMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        } catch (Exception e) {
            // 单独打一条，方便在控制台直接看到是哪条 SQL、哪个参数出的问题
            log.error("文章分页查询失败 title={} categoryId={} status={} sortField={} admin={}",
                    title, categoryId, status, sortField, admin, e);
            throw e;
        }
        Map<Long, String> categoryNames = categoryNameMap();

        List<ArticleResponseDTO> records = page.getRecords().stream()
                .map(article -> ArticleResponseDTO.from(article,
                        categoryNames.get(article.getCategoryId()), false))
                .toList();

        return PageResult.of(page.getTotal(), records);
    }

    /**
     * 文章详情，withContent=true 时带正文
     */
    public ArticleResponseDTO getArticle(String id, boolean withContent) {
        KnowledgeArticle article = articleMapper.selectById(id);
        if (article == null) {
            return null;
        }
        return ArticleResponseDTO.from(article, categoryNameMap().get(article.getCategoryId()), withContent);
    }

    /**
     * 阅读量 +1（只统计已发布文章）
     */
    public void increaseReadCount(String id) {
        UpdateWrapper<KnowledgeArticle> wrapper = new UpdateWrapper<>();
        wrapper.eq("id", id).eq("status", 1).setSql("read_count = read_count + 1");
        articleMapper.update(null, wrapper);
    }

    /**
     * 新增文章，id 优先用前端传的 UUID
     */
    public String createArticle(ArticleCommandDTO dto, Long authorId) {
        LocalDateTime now = LocalDateTime.now();
        String id = StrUtil.isNotBlank(dto.getId()) ? dto.getId() : IdUtil.simpleUUID();

        KnowledgeArticle article = KnowledgeArticle.builder()
                .id(id)
                .title(dto.getTitle())
                .categoryId(dto.getCategoryId())
                .coverImage(dto.getCoverImage())
                .summary(dto.getSummary())
                .content(dto.getContent())
                .tags(dto.getTags())
                .authorId(authorId)
                .authorName(resolveAuthorName(authorId))
                .status(0)
                .readCount(0)
                .createdAt(now)
                .updatedAt(now)
                .build();

        articleMapper.insert(article);
        return id;
    }

    /**
     * 编辑文章（不改变状态和阅读量）
     */
    public boolean updateArticle(String id, ArticleCommandDTO dto) {
        KnowledgeArticle existing = articleMapper.selectById(id);
        if (existing == null) {
            return false;
        }
        KnowledgeArticle article = KnowledgeArticle.builder()
                .id(id)
                .title(dto.getTitle())
                .categoryId(dto.getCategoryId())
                .coverImage(dto.getCoverImage())
                .summary(dto.getSummary())
                .content(dto.getContent())
                .tags(dto.getTags())
                .updatedAt(LocalDateTime.now())
                .build();

        articleMapper.updateById(article);
        return true;
    }

    /**
     * 发布 / 下线。status: 0草稿 1已发布 2已下线
     */
    public boolean changeStatus(String id, Integer status) {
        KnowledgeArticle existing = articleMapper.selectById(id);
        if (existing == null || status == null) {
            return false;
        }
        KnowledgeArticle article = KnowledgeArticle.builder()
                .id(id)
                .status(status)
                .updatedAt(LocalDateTime.now())
                .publishedAt(status == 1 ? LocalDateTime.now() : existing.getPublishedAt())
                .build();
        articleMapper.updateById(article);
        return true;
    }

    public boolean deleteArticle(String id) {
        return articleMapper.deleteById(id) > 0;
    }

    private Map<Long, String> categoryNameMap() {
        Map<Long, String> map = new HashMap<>();
        for (KnowledgeCategory category : listCategories()) {
            map.put(category.getId(), category.getCategoryName());
        }
        return map;
    }

    private String resolveAuthorName(Long authorId) {
        if (authorId == null) {
            return "匿名";
        }
        UserLoginResponseDTO.UserDetailResponseDTO user = userService.getUserById(authorId);
        if (user == null) {
            return "匿名";
        }
        if (StrUtil.isNotBlank(user.getNickname())) {
            return user.getNickname();
        }
        return StrUtil.isNotBlank(user.getUsername()) ? user.getUsername() : "匿名";
    }
}
