package com.example.aispring.DTO.response;

import com.example.aispring.entity.EmotionDiary;
import com.example.aispring.entity.User;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 情绪日记响应体，字段覆盖前端 emotional.vue 列表与详情弹窗
 */
@Data
@Builder
public class EmotionDiaryResponseDTO {
    private Long id;
    private Long userId;
    private String username;
    private String nickname;
    private LocalDate diaryDate;
    private Integer moodScore;
    private String dominantEmotion;
    private String emotionTriggers;
    private String diaryContent;
    private Integer sleepQuality;
    private Integer stressLevel;
    // 前端详情里 JSON.parse(row.aiEmotionAnalysis) 使用
    private String aiEmotionAnalysis;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static EmotionDiaryResponseDTO from(EmotionDiary diary, User user) {
        if (diary == null) {
            return null;
        }
        return EmotionDiaryResponseDTO.builder()
                .id(diary.getId())
                .userId(diary.getUserId())
                .username(user == null ? null : user.getUsername())
                .nickname(user == null ? null : user.getNickname())
                .diaryDate(diary.getDiaryDate())
                .moodScore(diary.getMoodScore())
                .dominantEmotion(diary.getDominantEmotion())
                .emotionTriggers(diary.getEmotionTriggers())
                .diaryContent(diary.getDiaryContent())
                .sleepQuality(diary.getSleepQuality())
                .stressLevel(diary.getStressLevel())
                .aiEmotionAnalysis(diary.getAiEmotionAnalysis())
                .createdAt(diary.getCreatedAt())
                .updatedAt(diary.getUpdatedAt())
                .build();
    }
}
