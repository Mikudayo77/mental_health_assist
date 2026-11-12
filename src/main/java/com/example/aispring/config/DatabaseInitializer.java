package com.example.aispring.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

/**
 * 启动时补全缺失的表/字段，并打印真实表结构。
 * 背景：库里已有的表可能是历史版本，列名/列数与当前实体不一致（例如缺 sort 列），
 * 直接查询会报 Unknown column。这里先建表（不存在时），再逐列补齐（缺失时），
 * 全程幂等；数据库账号没有 DDL 权限时只记录错误，不影响启动。
 */
@Component
public class DatabaseInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseInitializer.class);

    private static final String[] SCRIPTS = {
            "sql/knowledge_schema.sql",
            "sql/emotion_diary_schema.sql"
    };

    private static final String[] TABLES = {
            "user",
            "knowledge_category",
            "knowledge_article",
            "emotion_diary"
    };

    private record Column(String table, String name, String definition) {
    }

    /**
     * 实体里用到的列，缺哪个补哪个（全部允许 NULL，避免历史数据导致加列失败）
     */
    private static final Column[] REQUIRED_COLUMNS = {
            new Column("knowledge_category", "category_name", "VARCHAR(50) NULL"),
            new Column("knowledge_category", "parent_id", "BIGINT NULL DEFAULT 0"),
            new Column("knowledge_category", "sort", "INT NULL DEFAULT 0"),
            new Column("knowledge_category", "created_at", "DATETIME NULL"),
            new Column("knowledge_category", "updated_at", "DATETIME NULL"),

            new Column("knowledge_article", "title", "VARCHAR(200) NULL"),
            new Column("knowledge_article", "category_id", "BIGINT NULL"),
            new Column("knowledge_article", "cover_image", "VARCHAR(255) NULL"),
            new Column("knowledge_article", "summary", "VARCHAR(1000) NULL"),
            new Column("knowledge_article", "content", "LONGTEXT NULL"),
            new Column("knowledge_article", "tags", "VARCHAR(255) NULL"),
            new Column("knowledge_article", "author_id", "BIGINT NULL"),
            new Column("knowledge_article", "author_name", "VARCHAR(50) NULL"),
            new Column("knowledge_article", "status", "TINYINT NULL DEFAULT 0"),
            new Column("knowledge_article", "read_count", "INT NULL DEFAULT 0"),
            new Column("knowledge_article", "published_at", "DATETIME NULL"),
            new Column("knowledge_article", "created_at", "DATETIME NULL"),
            new Column("knowledge_article", "updated_at", "DATETIME NULL"),

            new Column("emotion_diary", "user_id", "BIGINT NULL"),
            new Column("emotion_diary", "diary_date", "DATE NULL"),
            new Column("emotion_diary", "mood_score", "INT NULL"),
            new Column("emotion_diary", "dominant_emotion", "VARCHAR(20) NULL"),
            new Column("emotion_diary", "emotion_triggers", "VARCHAR(1000) NULL"),
            new Column("emotion_diary", "diary_content", "TEXT NULL"),
            new Column("emotion_diary", "sleep_quality", "INT NULL"),
            new Column("emotion_diary", "stress_level", "INT NULL"),
            new Column("emotion_diary", "ai_emotion_analysis", "TEXT NULL"),
            new Column("emotion_diary", "created_at", "DATETIME NULL"),
            new Column("emotion_diary", "updated_at", "DATETIME NULL")
    };

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    public DatabaseInitializer(DataSource dataSource) {
        this.dataSource = dataSource;
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public void run(ApplicationArguments args) {
        for (String script : SCRIPTS) {
            try {
                ResourceDatabasePopulator populator = new ResourceDatabasePopulator(new ClassPathResource(script));
                populator.setSqlScriptEncoding("UTF-8");
                // 允许单条语句失败，继续补齐后面的列
                populator.setContinueOnError(true);
                populator.execute(dataSource);
                log.info("[init] 建表脚本执行完成：{}", script);
            } catch (Exception e) {
                log.error("[init] 执行 {} 失败，请检查数据库账号权限", script, e);
            }
        }
        ensureColumns();
        for (String table : TABLES) {
            logColumns(table);
        }
    }

    /**
     * 缺列补齐（只加列，不改类型、不删列，保证安全）
     */
    private void ensureColumns() {
        for (Column column : REQUIRED_COLUMNS) {
            try {
                Integer exists = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM information_schema.columns "
                                + "WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?",
                        Integer.class, column.table(), column.name());
                if (exists != null && exists > 0) {
                    continue;
                }
                jdbcTemplate.execute("ALTER TABLE " + column.table() + " ADD COLUMN "
                        + column.name() + " " + column.definition());
                log.warn("[init] {} 缺少列 {}，已自动补上（历史数据该列为空）", column.table(), column.name());
            } catch (Exception e) {
                log.error("[init] 为 {}.{} 补列失败：{}", column.table(), column.name(), e.getMessage());
            }
        }
    }

    /**
     * 打印真实表结构，结构不一致时能一眼看出来
     */
    private void logColumns(String table) {
        try {
            String summary = jdbcTemplate.query(
                    "SELECT column_name, column_type FROM information_schema.columns "
                            + "WHERE table_schema = DATABASE() AND table_name = ? ORDER BY ordinal_position",
                    rs -> {
                        StringBuilder sb = new StringBuilder();
                        while (rs.next()) {
                            sb.append(rs.getString(1)).append(' ').append(rs.getString(2)).append(" | ");
                        }
                        return sb.toString();
                    }, table);
            if (summary == null || summary.isEmpty()) {
                log.error("[init] 表 {} 不存在", table);
            } else {
                log.info("[init] {} 字段：{}", table, summary);
                String ddl = jdbcTemplate.query(
                        "SHOW CREATE TABLE `" + table + "`",
                        rs -> rs.next() ? rs.getString(2) : null);
                if (ddl != null) {
                    log.info("[init] {} 建表语句：{}", table, ddl.replaceAll("\\s+", " "));
                }
            }
        } catch (Exception e) {
            log.error("[init] 读取 {} 结构失败：{}", table, e.getMessage());
        }
    }
}
