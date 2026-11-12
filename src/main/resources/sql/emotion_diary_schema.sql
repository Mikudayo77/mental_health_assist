-- 情绪日记模块建表语句（AIspring 本地补全）

CREATE TABLE IF NOT EXISTS emotion_diary (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '记录人',
    diary_date DATE NOT NULL COMMENT '记录日期',
    mood_score INT COMMENT '情绪评分 1-10',
    dominant_emotion VARCHAR(20) COMMENT '主要情绪',
    emotion_triggers VARCHAR(1000) COMMENT '情绪触发因素',
    diary_content TEXT COMMENT '日记内容',
    sleep_quality INT COMMENT '睡眠质量 1-5',
    stress_level INT COMMENT '压力水平 1-5',
    ai_emotion_analysis TEXT COMMENT 'AI情绪分析结果(JSON)',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_user_date (user_id, diary_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '情绪日记';
