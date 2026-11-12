package com.example.aispring.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.aispring.DTO.command.EmotionDiaryCommandDTO;
import com.example.aispring.DTO.response.EmotionDiaryResponseDTO;
import com.example.aispring.common.PageResult;
import com.example.aispring.entity.EmotionDiary;
import com.example.aispring.entity.User;
import com.example.aispring.mapper.EmotionDiaryMapper;
import com.example.aispring.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 情绪日记
 */
@Service
public class EmotionDiaryService {

    @Autowired
    private EmotionDiaryMapper diaryMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private EmotionAnalysisService emotionAnalysisService;

    /**
     * 新增日记。
     * 表上有 (user_id, diary_date) 唯一键——一个用户一天只有一篇记录，
     * 所以当天重复提交视为"修改今日记录"，而不是再插一条。
     */
    public Long create(EmotionDiaryCommandDTO dto, Long userId) {
        LocalDate diaryDate = dto.getDiaryDate() == null ? LocalDate.now() : dto.getDiaryDate();
        LocalDateTime now = LocalDateTime.now();

        EmotionDiary existing = diaryMapper.selectOne(new LambdaQueryWrapper<EmotionDiary>()
                .eq(EmotionDiary::getUserId, userId)
                .eq(EmotionDiary::getDiaryDate, diaryDate)
                .last("limit 1"));

        String analysisText = buildAnalysisText(dto);

        if (existing != null) {
            diaryMapper.update(null, new LambdaUpdateWrapper<EmotionDiary>()
                    .eq(EmotionDiary::getId, existing.getId())
                    .set(EmotionDiary::getMoodScore, dto.getMoodScore())
                    .set(EmotionDiary::getDominantEmotion, dto.getDominantEmotion())
                    .set(EmotionDiary::getEmotionTriggers, dto.getEmotionTriggers())
                    .set(EmotionDiary::getDiaryContent, dto.getDiaryContent())
                    .set(EmotionDiary::getSleepQuality, dto.getSleepQuality())
                    .set(EmotionDiary::getStressLevel, dto.getStressLevel())
                    .set(EmotionDiary::getUpdatedAt, now));
            // 异步让大模型分析这篇日记，结果写回 ai_emotion_analysis（失败不影响提交）
            emotionAnalysisService.analyzeDiaryAsync(existing.getId(), analysisText);
            return existing.getId();
        }

        EmotionDiary diary = EmotionDiary.builder()
                .userId(userId)
                .diaryDate(diaryDate)
                .moodScore(dto.getMoodScore())
                .dominantEmotion(dto.getDominantEmotion())
                .emotionTriggers(dto.getEmotionTriggers())
                .diaryContent(dto.getDiaryContent())
                .sleepQuality(dto.getSleepQuality())
                .stressLevel(dto.getStressLevel())
                .createdAt(now)
                .updatedAt(now)
                .build();
        diaryMapper.insert(diary);
        // 异步让大模型分析这篇日记，结果写回 ai_emotion_analysis（失败不影响提交）
        emotionAnalysisService.analyzeDiaryAsync(diary.getId(), analysisText);
        return diary.getId();
    }

    /**
     * 把表单里的情绪信息拼成给模型看的文本
     */
    private String buildAnalysisText(EmotionDiaryCommandDTO dto) {
        StringBuilder sb = new StringBuilder();
        if (StrUtil.isNotBlank(dto.getDominantEmotion())) {
            sb.append("用户自评主要情绪：").append(dto.getDominantEmotion()).append('\n');
        }
        if (dto.getMoodScore() != null) {
            sb.append("自评情绪评分：").append(dto.getMoodScore()).append("/10\n");
        }
        if (dto.getSleepQuality() != null) {
            sb.append("睡眠质量：").append(dto.getSleepQuality()).append("/5\n");
        }
        if (dto.getStressLevel() != null) {
            sb.append("压力水平：").append(dto.getStressLevel()).append("/5\n");
        }
        if (StrUtil.isNotBlank(dto.getEmotionTriggers())) {
            sb.append("情绪触发因素：").append(dto.getEmotionTriggers()).append('\n');
        }
        if (StrUtil.isNotBlank(dto.getDiaryContent())) {
            sb.append("日记内容：").append(dto.getDiaryContent());
        }
        return sb.toString();
    }

    /**
     * 后台分页。前端分页参数是 current / size，还支持 userId 与 moodScreRange（1-3 / 4-6 / 7-10）
     */
    public PageResult<EmotionDiaryResponseDTO> page(Integer current, Integer size, Long userId, String moodScoreRange) {
        int pageNum = (current == null || current < 1) ? 1 : current;
        int pageSize = (size == null || size < 1) ? 10 : Math.min(size, 50);

        QueryWrapper<EmotionDiary> wrapper = new QueryWrapper<>();
        wrapper.eq(userId != null, "user_id", userId);
        applyMoodRange(wrapper, moodScoreRange);
        wrapper.orderByDesc("diary_date").orderByDesc("id");

        Page<EmotionDiary> page = diaryMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);

        Map<Long, User> userMap = userMap(page.getRecords());
        List<EmotionDiaryResponseDTO> records = page.getRecords().stream()
                .map(diary -> EmotionDiaryResponseDTO.from(diary, userMap.get(diary.getUserId())))
                .toList();

        return PageResult.of(page.getTotal(), records);
    }

    public boolean delete(Long id) {
        return diaryMapper.deleteById(id) > 0;
    }

    /**
     * 解析 "1-3" / "4-6" / "7-10" 这类区间筛选
     */
    private void applyMoodRange(QueryWrapper<EmotionDiary> wrapper, String moodScoreRange) {
        if (StrUtil.isBlank(moodScoreRange) || !moodScoreRange.contains("-")) {
            return;
        }
        String[] parts = moodScoreRange.split("-");
        try {
            int min = Integer.parseInt(parts[0].trim());
            int max = Integer.parseInt(parts[1].trim());
            wrapper.ge("mood_score", min).le("mood_score", max);
        } catch (NumberFormatException ignored) {
            // 非法区间直接忽略，不影响其他条件
        }
    }

    private Map<Long, User> userMap(List<EmotionDiary> diaries) {
        Map<Long, User> map = new HashMap<>();
        List<Long> userIds = diaries.stream()
                .map(EmotionDiary::getUserId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        if (userIds.isEmpty()) {
            return map;
        }
        for (User user : userMapper.selectBatchIds(userIds)) {
            map.put(user.getId(), user);
        }
        return map;
    }
}
