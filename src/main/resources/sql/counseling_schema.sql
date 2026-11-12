-- ============================================================
-- 心理咨询预约/套餐/支付/退款/人工咨询/危机干预 建表脚本
-- 数据库: mental_health_assistant
-- 执行方式: IDEA Database Console / Navicat / mysql client 手动执行
-- 说明: 项目未接入 Flyway，本脚本需手动执行；全部使用 IF NOT EXISTS，可重复执行
-- ============================================================

-- 1. 咨询师资料表
CREATE TABLE IF NOT EXISTS counseling_counselor (
  id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
  user_id BIGINT NOT NULL COMMENT '关联user表ID',
  real_name VARCHAR(50) NOT NULL COMMENT '真实姓名',
  gender TINYINT DEFAULT NULL COMMENT '性别 1男 2女',
  phone VARCHAR(20) DEFAULT NULL COMMENT '联系电话',
  email VARCHAR(100) DEFAULT NULL COMMENT '联系邮箱',
  qualification_no VARCHAR(64) DEFAULT NULL COMMENT '资质证书编号',
  certification_file_url VARCHAR(255) DEFAULT NULL COMMENT '证书附件URL',
  specialty VARCHAR(255) DEFAULT NULL COMMENT '擅长领域',
  introduction TEXT COMMENT '个人简介',
  years_of_experience INT DEFAULT 0 COMMENT '从业年限',
  audit_status TINYINT NOT NULL DEFAULT 0 COMMENT '审核状态 0待审核 1通过 2拒绝',
  audit_remark VARCHAR(255) DEFAULT NULL COMMENT '审核备注',
  audited_by BIGINT DEFAULT NULL COMMENT '审核管理员ID',
  audited_at DATETIME DEFAULT NULL COMMENT '审核时间',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '启用状态 1启用 0禁用',
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  UNIQUE KEY uk_user_id (user_id),
  KEY idx_audit_status (audit_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='咨询师资料表';

-- 2. 套餐表
CREATE TABLE IF NOT EXISTS counseling_package (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(100) NOT NULL COMMENT '套餐名称',
  description VARCHAR(500) DEFAULT NULL COMMENT '套餐描述',
  duration_minutes INT NOT NULL COMMENT '咨询时长(分钟)',
  price DECIMAL(10,2) NOT NULL COMMENT '价格',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态 1上架 0下架',
  created_by BIGINT DEFAULT NULL COMMENT '创建人(管理员ID)',
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='心理咨询套餐表';

-- 3. 预约表
CREATE TABLE IF NOT EXISTS counseling_appointment (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  appointment_no VARCHAR(32) NOT NULL COMMENT '预约单号',
  user_id BIGINT NOT NULL COMMENT '预约用户ID',
  counselor_id BIGINT NOT NULL COMMENT '咨询师资料ID(counseling_counselor.id)',
  package_id BIGINT NOT NULL COMMENT '套餐ID',
  appointment_start DATETIME NOT NULL COMMENT '预约开始时间',
  appointment_end DATETIME NOT NULL COMMENT '预约结束时间(=开始+套餐时长)',
  status TINYINT NOT NULL DEFAULT 0 COMMENT '状态 0待确认 1已确认 2已完成 3已取消 4已拒绝',
  cancel_reason VARCHAR(255) DEFAULT NULL COMMENT '取消/拒绝原因',
  confirmed_at DATETIME DEFAULT NULL COMMENT '确认时间',
  completed_at DATETIME DEFAULT NULL COMMENT '完成时间',
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  UNIQUE KEY uk_appointment_no (appointment_no),
  KEY idx_user_id (user_id),
  KEY idx_counselor_status (counselor_id, status),
  KEY idx_start (appointment_start)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='咨询预约表';

-- 4. 订单表(order 是 MySQL 保留字, 表名加前缀)
CREATE TABLE IF NOT EXISTS counseling_order (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  order_no VARCHAR(32) NOT NULL COMMENT '订单号',
  user_id BIGINT NOT NULL COMMENT '下单用户ID',
  appointment_id BIGINT NOT NULL COMMENT '预约ID',
  package_id BIGINT NOT NULL COMMENT '套餐ID',
  package_name VARCHAR(100) NOT NULL COMMENT '套餐名称快照',
  package_duration INT NOT NULL COMMENT '时长快照(分钟)',
  amount DECIMAL(10,2) NOT NULL COMMENT '应付金额',
  refunded_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '已退金额',
  status TINYINT NOT NULL DEFAULT 0 COMMENT '状态 0未支付 1已支付 2退款中 3已退款 4已取消',
  paid_at DATETIME DEFAULT NULL COMMENT '支付时间',
  canceled_at DATETIME DEFAULT NULL COMMENT '取消时间',
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  UNIQUE KEY uk_order_no (order_no),
  KEY idx_user_id (user_id),
  KEY idx_appointment_id (appointment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='咨询订单表';

-- 5. 支付记录表
CREATE TABLE IF NOT EXISTS counseling_payment (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  payment_no VARCHAR(32) NOT NULL COMMENT '支付单号',
  order_id BIGINT NOT NULL COMMENT '订单ID',
  order_no VARCHAR(32) NOT NULL COMMENT '订单号(冗余)',
  user_id BIGINT NOT NULL COMMENT '支付用户ID',
  amount DECIMAL(10,2) NOT NULL COMMENT '支付金额',
  pay_method TINYINT NOT NULL DEFAULT 1 COMMENT '支付方式 1模拟支付',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态 1成功 0失败',
  transaction_no VARCHAR(64) DEFAULT NULL COMMENT '流水号(mock生成)',
  paid_at DATETIME DEFAULT NULL COMMENT '支付完成时间',
  created_at DATETIME NOT NULL,
  UNIQUE KEY uk_payment_no (payment_no),
  UNIQUE KEY uk_transaction_no (transaction_no),
  KEY idx_order_id (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付记录表';

-- 6. 退款记录表
CREATE TABLE IF NOT EXISTS counseling_refund (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  refund_no VARCHAR(32) NOT NULL COMMENT '退款单号',
  order_id BIGINT NOT NULL COMMENT '订单ID',
  order_no VARCHAR(32) NOT NULL COMMENT '订单号(冗余)',
  user_id BIGINT NOT NULL COMMENT '申请用户ID',
  refund_amount DECIMAL(10,2) NOT NULL COMMENT '退款金额',
  rule_desc VARCHAR(255) DEFAULT NULL COMMENT '命中的退款规则描述',
  status TINYINT NOT NULL DEFAULT 0 COMMENT '状态 0待审核 1已退款 2已拒绝',
  reason VARCHAR(255) DEFAULT NULL COMMENT '申请原因',
  reject_reason VARCHAR(255) DEFAULT NULL COMMENT '拒绝原因',
  audited_by BIGINT DEFAULT NULL COMMENT '审核人(0=系统自动)',
  applied_at DATETIME NOT NULL COMMENT '申请时间',
  audited_at DATETIME DEFAULT NULL COMMENT '审核时间',
  created_at DATETIME NOT NULL,
  UNIQUE KEY uk_refund_no (refund_no),
  KEY idx_order_id (order_id),
  KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='退款记录表';

-- 7. 人工咨询聊天消息表
CREATE TABLE IF NOT EXISTS counseling_chat_message (
  id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '自增ID,轮询增量游标',
  appointment_id BIGINT NOT NULL COMMENT '预约ID',
  sender_id BIGINT NOT NULL COMMENT '发送者user表ID',
  sender_type TINYINT NOT NULL COMMENT '发送者类型 1用户 2咨询师',
  content TEXT NOT NULL COMMENT '消息内容',
  crisis_flag TINYINT NOT NULL DEFAULT 0 COMMENT '危机标记 0无 1命中敏感词',
  created_at DATETIME NOT NULL,
  KEY idx_appointment_id (appointment_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='人工咨询聊天消息表';

-- 8. 危机记录表
CREATE TABLE IF NOT EXISTS counseling_crisis_record (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL COMMENT '触发用户ID',
  source_type TINYINT NOT NULL COMMENT '来源 1 AI聊天 2 人工聊天',
  source_id BIGINT DEFAULT NULL COMMENT '来源ID(AI会话ID或预约ID)',
  message_content TEXT COMMENT '触发消息内容',
  matched_words VARCHAR(255) DEFAULT NULL COMMENT '命中的敏感词(逗号分隔)',
  risk_level VARCHAR(10) NOT NULL COMMENT '风险等级 LOW/MEDIUM/HIGH',
  ai_analysis TEXT COMMENT '大模型评估理由',
  handle_status TINYINT NOT NULL DEFAULT 0 COMMENT '处理状态 0待处理 1已处理',
  handled_by BIGINT DEFAULT NULL COMMENT '处理人(管理员ID)',
  handled_at DATETIME DEFAULT NULL COMMENT '处理时间',
  handle_remark VARCHAR(255) DEFAULT NULL COMMENT '处理备注',
  created_at DATETIME NOT NULL,
  KEY idx_user_id (user_id),
  KEY idx_risk (risk_level, handle_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='危机干预记录表';

-- 9. 系统通知表
CREATE TABLE IF NOT EXISTS counseling_notification (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL COMMENT '接收人用户ID',
  title VARCHAR(100) NOT NULL COMMENT '通知标题',
  content VARCHAR(500) NOT NULL COMMENT '通知内容',
  type TINYINT NOT NULL COMMENT '类型 1预约 2退款 3危机 4审核',
  biz_no VARCHAR(32) DEFAULT NULL COMMENT '关联业务单号',
  read_flag TINYINT NOT NULL DEFAULT 0 COMMENT '0未读 1已读',
  created_at DATETIME NOT NULL,
  KEY idx_user_read (user_id, read_flag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统通知表';

-- ============================================================
-- 管理员账号: 先通过 /api/user/add 注册普通账号, 再执行:
-- UPDATE `user` SET user_type = 2 WHERE username = 'admin';
-- (BCrypt 密文含随机盐, 不建议在脚本中硬编码)
-- ============================================================
