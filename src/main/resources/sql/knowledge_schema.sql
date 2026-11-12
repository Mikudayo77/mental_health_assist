-- 知识库模块建表语句（AIspring 本地补全）
-- 说明：id 使用 VARCHAR(36)，因为前端新建文章时传入的是 crypto.randomUUID() 生成的字符串

CREATE TABLE IF NOT EXISTS knowledge_category (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    category_name VARCHAR(50) NOT NULL COMMENT '分类名称',
    parent_id BIGINT DEFAULT 0 COMMENT '父分类ID，0为顶级',
    sort INT DEFAULT 0 COMMENT '排序值',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_category_name (category_name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '知识库分类';

CREATE TABLE IF NOT EXISTS knowledge_article (
    id VARCHAR(36) NOT NULL PRIMARY KEY COMMENT '文章ID（前端UUID）',
    title VARCHAR(200) NOT NULL COMMENT '标题',
    category_id BIGINT COMMENT '分类ID',
    cover_image VARCHAR(255) COMMENT '封面图相对路径，如 /uploads/ARTICLE/2026/11/xx.png',
    summary VARCHAR(1000) COMMENT '摘要',
    content LONGTEXT COMMENT '正文HTML',
    tags VARCHAR(255) COMMENT '标签，逗号分隔',
    author_id BIGINT COMMENT '作者用户ID',
    author_name VARCHAR(50) COMMENT '作者展示名',
    status TINYINT DEFAULT 0 COMMENT '0草稿 1已发布 2已下线',
    read_count INT DEFAULT 0 COMMENT '阅读量',
    published_at DATETIME COMMENT '发布时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_category (category_id),
    KEY idx_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '知识库文章';

-- 初始分类：仅当分类表为空时才写入，避免历史库已有分类时产生重复
INSERT INTO knowledge_category (category_name, parent_id, sort)
SELECT * FROM (
    SELECT '心理健康常识' AS category_name, 0 AS parent_id, 1 AS sort
    UNION ALL SELECT '情绪管理', 0, 2
    UNION ALL SELECT '睡眠与放松', 0, 3
) seed
WHERE NOT EXISTS (SELECT 1 FROM knowledge_category);
