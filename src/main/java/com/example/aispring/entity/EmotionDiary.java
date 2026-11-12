package com.example.aispring.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 情绪日记
 */
@Data
@Builder
@TableName("emotion_diary")
public class EmotionDiary {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    // 记录日期，前端用 YYYY-MM-DD
    private LocalDate diaryDate;

    // 情绪评分 1-10
    private Integer moodScore;

    // 主要情绪：开心/平静/焦虑...
    private String dominantEmotion;

    private String emotionTriggers;

    private String diaryContent;

    // 睡眠质量 1-5
    private Integer sleepQuality;

    // 压力水平 1-5
    private Integer stressLevel;

    // AI 情绪分析结果（JSON 字符串），暂未生成
    private String aiEmotionAnalysis;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
