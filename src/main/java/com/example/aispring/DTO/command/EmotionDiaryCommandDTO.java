package com.example.aispring.DTO.command;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/**
 * 情绪日记提交请求体，字段与前端 emotionDiary.vue 的 diaryForm 一致
 */
@Data
public class EmotionDiaryCommandDTO {

    // 前端传 YYYY-MM-DD
    private LocalDate diaryDate;

    @NotNull(message = "请选择情绪评分")
    @Min(value = 1, message = "情绪评分最低1分")
    @Max(value = 10, message = "情绪评分最高10分")
    private Integer moodScore;

    private String dominantEmotion;

    @Size(max = 1000, message = "情绪触发因素最多1000个字符")
    private String emotionTriggers;

    @Size(max = 2000, message = "日记内容最多2000个字符")
    private String diaryContent;

    @Min(value = 1, message = "睡眠质量最低1")
    @Max(value = 5, message = "睡眠质量最高5")
    private Integer sleepQuality;

    @Min(value = 1, message = "压力水平最低1")
    @Max(value = 5, message = "压力水平最高5")
    private Integer stressLevel;
}
