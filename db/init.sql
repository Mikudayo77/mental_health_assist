-- ============================================================
-- 心理健康AI助手 · 数据库初始化脚本
-- 用法：mysql -uroot -p --default-character-set=utf8mb4 < db/init.sql
-- 说明：只包含表结构与最小种子数据（套餐/知识分类/管理员账号），
--       不包含任何用户真实数据。执行后即可直接启动后端。
-- ============================================================

CREATE DATABASE IF NOT EXISTS mental_health_assistant
    DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE mental_health_assistant;

CREATE TABLE `user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户名',
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '邮箱',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '手机号',
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '密码',
  `nickname` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '昵称',
  `avatar` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '头像URL',
  `gender` tinyint DEFAULT NULL COMMENT '性别 0:未知 1:男 2:女',
  `birthday` date DEFAULT NULL COMMENT '生日',
  `user_type` tinyint DEFAULT '1' COMMENT '用户类型 1:普通用户 2:管理员',
  `status` tinyint DEFAULT '1' COMMENT '状态 0:禁用 1:正常',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `username` (`username`) USING BTREE,
  UNIQUE KEY `email` (`email`) USING BTREE,
  UNIQUE KEY `phone` (`phone`) USING BTREE,
  KEY `idx_username` (`username`) USING BTREE,
  KEY `idx_user_type` (`user_type`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=19 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='用户表';
CREATE TABLE `consultation_session` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '会话ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `session_title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '会话标题',
  `started_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `last_emotion_analysis` json DEFAULT NULL COMMENT '最后一次情绪分析结果(JSON格式)',
  `last_emotion_updated_at` datetime DEFAULT NULL COMMENT '最后一次情绪分析更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_user_session` (`user_id`,`started_at`) USING BTREE,
  KEY `idx_last_emotion_updated_at` (`last_emotion_updated_at`) USING BTREE,
  CONSTRAINT `consultation_session_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=37 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='咨询会话表';
CREATE TABLE `consultation_message` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '消息ID',
  `session_id` bigint NOT NULL COMMENT '会话ID',
  `sender_type` tinyint NOT NULL COMMENT '发送者类型 1:用户 2:AI助手',
  `message_type` tinyint DEFAULT '1' COMMENT '消息类型 1:文本',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '消息内容',
  `emotion_tag` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '情绪标签',
  `ai_model` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '使用的AI模型',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_session_message` (`session_id`,`created_at`) USING BTREE,
  CONSTRAINT `consultation_message_ibfk_1` FOREIGN KEY (`session_id`) REFERENCES `consultation_session` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=149 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='咨询消息表';
CREATE TABLE `counseling_package` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(100) NOT NULL COMMENT '套餐名称',
  `description` varchar(500) DEFAULT NULL COMMENT '套餐描述',
  `duration_minutes` int NOT NULL COMMENT '咨询时长(分钟)',
  `price` decimal(10,2) NOT NULL COMMENT '价格',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态 1上架 0下架',
  `created_by` bigint DEFAULT NULL COMMENT '创建人(管理员ID)',
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='心理咨询套餐表';
CREATE TABLE `counseling_counselor` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint NOT NULL COMMENT '关联user表ID',
  `real_name` varchar(50) NOT NULL COMMENT '真实姓名',
  `gender` tinyint DEFAULT NULL COMMENT '性别 1男 2女',
  `phone` varchar(20) DEFAULT NULL COMMENT '联系电话',
  `email` varchar(100) DEFAULT NULL COMMENT '联系邮箱',
  `qualification_no` varchar(64) DEFAULT NULL COMMENT '资质证书编号',
  `certification_file_url` varchar(255) DEFAULT NULL COMMENT '证书附件URL',
  `specialty` varchar(255) DEFAULT NULL COMMENT '擅长领域',
  `introduction` text COMMENT '个人简介',
  `years_of_experience` int DEFAULT '0' COMMENT '从业年限',
  `audit_status` tinyint NOT NULL DEFAULT '0' COMMENT '审核状态 0待审核 1通过 2拒绝',
  `audit_remark` varchar(255) DEFAULT NULL COMMENT '审核备注',
  `audited_by` bigint DEFAULT NULL COMMENT '审核管理员ID',
  `audited_at` datetime DEFAULT NULL COMMENT '审核时间',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '启用状态 1启用 0禁用',
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`),
  KEY `idx_audit_status` (`audit_status`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='咨询师资料表';
CREATE TABLE `counseling_appointment` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `appointment_no` varchar(32) NOT NULL COMMENT '预约单号',
  `user_id` bigint NOT NULL COMMENT '预约用户ID',
  `counselor_id` bigint NOT NULL COMMENT '咨询师资料ID(counseling_counselor.id)',
  `package_id` bigint NOT NULL COMMENT '套餐ID',
  `appointment_start` datetime NOT NULL COMMENT '预约开始时间',
  `appointment_end` datetime NOT NULL COMMENT '预约结束时间(=开始+套餐时长)',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态 0待确认 1已确认 2已完成 3已取消 4已拒绝',
  `cancel_reason` varchar(255) DEFAULT NULL COMMENT '取消/拒绝原因',
  `confirmed_at` datetime DEFAULT NULL COMMENT '确认时间',
  `completed_at` datetime DEFAULT NULL COMMENT '完成时间',
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_appointment_no` (`appointment_no`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_counselor_status` (`counselor_id`,`status`),
  KEY `idx_start` (`appointment_start`)
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='咨询预约表';
CREATE TABLE `counseling_order` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_no` varchar(32) NOT NULL COMMENT '订单号',
  `user_id` bigint NOT NULL COMMENT '下单用户ID',
  `appointment_id` bigint NOT NULL COMMENT '预约ID',
  `package_id` bigint NOT NULL COMMENT '套餐ID',
  `package_name` varchar(100) NOT NULL COMMENT '套餐名称快照',
  `package_duration` int NOT NULL COMMENT '时长快照(分钟)',
  `amount` decimal(10,2) NOT NULL COMMENT '应付金额',
  `refunded_amount` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '已退金额',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态 0未支付 1已支付 2退款中 3已退款 4已取消',
  `paid_at` datetime DEFAULT NULL COMMENT '支付时间',
  `canceled_at` datetime DEFAULT NULL COMMENT '取消时间',
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_appointment_id` (`appointment_id`)
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='咨询订单表';
CREATE TABLE `counseling_payment` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `payment_no` varchar(32) NOT NULL COMMENT '支付单号',
  `order_id` bigint NOT NULL COMMENT '订单ID',
  `order_no` varchar(32) NOT NULL COMMENT '订单号(冗余)',
  `user_id` bigint NOT NULL COMMENT '支付用户ID',
  `amount` decimal(10,2) NOT NULL COMMENT '支付金额',
  `pay_method` tinyint NOT NULL DEFAULT '1' COMMENT '支付方式 1模拟支付',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态 1成功 0失败',
  `transaction_no` varchar(64) DEFAULT NULL COMMENT '流水号(mock生成)',
  `paid_at` datetime DEFAULT NULL COMMENT '支付完成时间',
  `created_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_no` (`payment_no`),
  UNIQUE KEY `uk_transaction_no` (`transaction_no`),
  KEY `idx_order_id` (`order_id`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支付记录表';
CREATE TABLE `counseling_refund` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `refund_no` varchar(32) NOT NULL COMMENT '退款单号',
  `order_id` bigint NOT NULL COMMENT '订单ID',
  `order_no` varchar(32) NOT NULL COMMENT '订单号(冗余)',
  `user_id` bigint NOT NULL COMMENT '申请用户ID',
  `refund_amount` decimal(10,2) NOT NULL COMMENT '退款金额',
  `rule_desc` varchar(255) DEFAULT NULL COMMENT '命中的退款规则描述',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态 0待审核 1已退款 2已拒绝',
  `reason` varchar(255) DEFAULT NULL COMMENT '申请原因',
  `reject_reason` varchar(255) DEFAULT NULL COMMENT '拒绝原因',
  `audited_by` bigint DEFAULT NULL COMMENT '审核人(0=系统自动)',
  `applied_at` datetime NOT NULL COMMENT '申请时间',
  `audited_at` datetime DEFAULT NULL COMMENT '审核时间',
  `created_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_refund_no` (`refund_no`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='退款记录表';
CREATE TABLE `counseling_crisis_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '触发用户ID',
  `source_type` tinyint NOT NULL COMMENT '来源 1 AI聊天 2 人工聊天',
  `source_id` bigint DEFAULT NULL COMMENT '来源ID(AI会话ID或预约ID)',
  `message_content` text COMMENT '触发消息内容',
  `matched_words` varchar(255) DEFAULT NULL COMMENT '命中的敏感词(逗号分隔)',
  `risk_level` varchar(10) NOT NULL COMMENT '风险等级 LOW/MEDIUM/HIGH',
  `ai_analysis` text COMMENT '大模型评估理由',
  `handle_status` tinyint NOT NULL DEFAULT '0' COMMENT '处理状态 0待处理 1已处理',
  `handled_by` bigint DEFAULT NULL COMMENT '处理人(管理员ID)',
  `handled_at` datetime DEFAULT NULL COMMENT '处理时间',
  `handle_remark` varchar(255) DEFAULT NULL COMMENT '处理备注',
  `created_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_risk` (`risk_level`,`handle_status`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='危机干预记录表';
CREATE TABLE `counseling_notification` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '接收人用户ID',
  `title` varchar(100) NOT NULL COMMENT '通知标题',
  `content` varchar(500) NOT NULL COMMENT '通知内容',
  `type` tinyint NOT NULL COMMENT '类型 1预约 2退款 3危机 4审核',
  `biz_no` varchar(32) DEFAULT NULL COMMENT '关联业务单号',
  `read_flag` tinyint NOT NULL DEFAULT '0' COMMENT '0未读 1已读',
  `created_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_user_read` (`user_id`,`read_flag`)
) ENGINE=InnoDB AUTO_INCREMENT=32 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统通知表';
CREATE TABLE `counseling_chat_message` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '自增ID,轮询增量游标',
  `appointment_id` bigint NOT NULL COMMENT '预约ID',
  `sender_id` bigint NOT NULL COMMENT '发送者user表ID',
  `sender_type` tinyint NOT NULL COMMENT '发送者类型 1用户 2咨询师',
  `content` text NOT NULL COMMENT '消息内容',
  `crisis_flag` tinyint NOT NULL DEFAULT '0' COMMENT '危机标记 0无 1命中敏感词',
  `created_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_appointment_id` (`appointment_id`,`id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='人工咨询聊天消息表';
CREATE TABLE `knowledge_category` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '分类ID',
  `parent_id` bigint DEFAULT '0' COMMENT '父分类ID',
  `category_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '分类名称',
  `category_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '分类代码',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '分类描述',
  `sort_order` int DEFAULT '0' COMMENT '排序',
  `status` tinyint DEFAULT '1' COMMENT '状态 0:禁用 1:启用',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `sort` int DEFAULT '0',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `category_code` (`category_code`) USING BTREE,
  KEY `idx_parent_id` (`parent_id`) USING BTREE,
  KEY `idx_sort_order` (`sort_order`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=15 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='知识文章分类表';
CREATE TABLE `knowledge_article` (
  `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '文章ID(UUID)',
  `category_id` bigint NOT NULL COMMENT '分类ID',
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '文章标题',
  `summary` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '文章摘要',
  `content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '文章内容',
  `cover_image` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '封面图片',
  `tags` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '标签',
  `author_id` bigint DEFAULT NULL COMMENT '作者ID',
  `read_count` int DEFAULT '0' COMMENT '阅读次数',
  `status` tinyint DEFAULT '1' COMMENT '状态 1:已发布',
  `published_at` datetime DEFAULT NULL COMMENT '发布时间',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `author_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  KEY `author_id` (`author_id`) USING BTREE,
  KEY `idx_category_article` (`category_id`,`published_at`) USING BTREE,
  KEY `idx_title` (`title`) USING BTREE,
  CONSTRAINT `knowledge_article_ibfk_1` FOREIGN KEY (`category_id`) REFERENCES `knowledge_category` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `knowledge_article_ibfk_2` FOREIGN KEY (`author_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='知识文章表';
CREATE TABLE `emotion_diary` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '日记ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `diary_date` date NOT NULL COMMENT '日记日期',
  `mood_score` tinyint NOT NULL COMMENT '情绪评分(1-10)',
  `dominant_emotion` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '主要情绪',
  `emotion_triggers` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '情绪触发因素',
  `diary_content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '日记内容',
  `sleep_quality` tinyint DEFAULT NULL COMMENT '睡眠质量(1-5)',
  `stress_level` tinyint DEFAULT NULL COMMENT '压力水平(1-5)',
  `ai_emotion_analysis` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT 'AI情绪分析结果(JSON格式)',
  `ai_analysis_updated_at` datetime DEFAULT NULL COMMENT 'AI分析更新时间',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `user_date_unique` (`user_id`,`diary_date`) USING BTREE,
  KEY `idx_user_diary` (`user_id`,`diary_date`) USING BTREE,
  KEY `idx_ai_analysis_time` (`ai_analysis_updated_at`) USING BTREE,
  CONSTRAINT `emotion_diary_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=37 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='情绪日记表';

-- ============================================================
-- 种子数据 1：咨询套餐（预约页需要，否则无可选项）
-- ============================================================
INSERT INTO counseling_package (name, description, duration_minutes, price, status, created_at, updated_at) VALUES
('初次咨询(30分钟)', '适合初步了解与情绪疏导', 30, 99.00, 1, NOW(), NOW()),
('深度咨询(60分钟)', '适合深入探讨心理困扰', 60, 199.00, 1, NOW(), NOW()),
('危机干预(90分钟)', '适合紧急情况下的深度干预', 90, 299.00, 1, NOW(), NOW());

-- ============================================================
-- 种子数据 2：知识库分类（新增文章时需要有分类可选）
-- ============================================================
INSERT INTO knowledge_category (category_name, parent_id, sort, created_at, updated_at) VALUES
('心理健康基础', 0, 1, NOW(), NOW()),
('情绪管理', 0, 2, NOW(), NOW()),
('压力缓解', 0, 3, NOW(), NOW()),
('人际关系', 0, 4, NOW(), NOW());

-- ============================================================
-- 种子数据 3：管理员账号
-- 账号 admin / 密码 123456（BCrypt 加密存储，登录后请尽快修改）
-- 注意 phone 显式写 NULL：该列有唯一索引，空字符串会导致第二个用户注册失败
-- ============================================================
INSERT INTO `user` (username, email, phone, password, nickname, gender, user_type, status, created_at, updated_at)
VALUES ('admin', 'admin@example.com', NULL,
        '$2a$10$5brfEMgPb1Xgf0UUZqP9aedMkYDvC/MDMSS5eIzVj8hZJuZ2ubDLW',
        '管理员', 1, 2, 1, NOW(), NOW());
