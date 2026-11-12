package com.example.aispring.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.aispring.AiService.PromptManage;
import com.example.aispring.entity.ConsultationSession;
import com.example.aispring.entity.EmotionDiary;
import com.example.aispring.mapper.ConsultationSessionMapper;
import com.example.aispring.mapper.EmotionDiaryMapper;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 情绪分析：调用大模型把一段文本变成结构化情绪结果，存成 JSON。
 *
 * 两个使用场景：
 * 1. AI 咨询：分析一次对话后写入 consultation_session.last_emotion_analysis，供前端"情绪花园"展示
 * 2. 情绪日记：提交后写入 emotion_diary.ai_emotion_analysis，供后台详情展示
 *
 * 全部异步执行 + 失败降级：AI 挂了不能影响主流程（聊天出字、日记提交）。
 */
@Service
public class EmotionAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(EmotionAnalysisService.class);

    @Resource
    private OpenAiChatModel openAiChatModel;
    @Resource
    private ConsultationSessionMapper sessionMapper;
    @Resource
    private EmotionDiaryMapper diaryMapper;

    /**
     * 分析文本，返回规范化的 JSON 字符串；失败返回 null（不抛异常）
     */
    public String analyzeToJson(String text) {
        if (StrUtil.isBlank(text)) {
            return null;
        }
        try {
            String response = ChatClient.builder(openAiChatModel).build()
                    .prompt()
                    .user(String.format(PromptManage.EMOTION_ANALYSIS_PROMPT, text))
                    .call()
                    .content();
            JSONObject raw = JSONUtil.parseObj(extractJson(response));

            List<String> actions = new ArrayList<>();
            JSONArray array = raw.getJSONArray("improvementSuggestions");
            if (array != null) {
                for (Object item : array) {
                    String action = String.valueOf(item);
                    if (StrUtil.isNotBlank(action) && actions.size() < 5) {
                        actions.add(action.trim());
                    }
                }
            }

            // 统一重新组装，避免模型多返回/少返回字段导致前端拿不到值
            JSONObject result = new JSONObject();
            result.set("primaryEmotion", StrUtil.blankToDefault(raw.getStr("primaryEmotion"), "中性"));
            result.set("emotionScore", clamp(raw.getInt("emotionScore", 50), 0, 100));
            result.set("isNegative", raw.getBool("isNegative", false));
            result.set("riskLevel", clamp(raw.getInt("riskLevel", 0), 0, 3));
            result.set("suggestion", StrUtil.blankToDefault(raw.getStr("suggestion"), ""));
            result.set("improvementSuggestions", actions);
            result.set("riskDescription", StrUtil.blankToDefault(raw.getStr("riskDescription"), ""));
            return result.toString();
        } catch (Exception e) {
            log.warn("情绪分析失败：{}", e.getMessage());
            return null;
        }
    }

    /**
     * 异步分析一次 AI 咨询对话，结果写回会话
     */
    public void analyzeSessionAsync(Long sessionId, String text) {
        if (sessionId == null) {
            return;
        }
        CompletableFuture.runAsync(() -> {
            try {
                String json = analyzeToJson(text);
                if (json == null) {
                    return;
                }
                sessionMapper.update(null, new LambdaUpdateWrapper<ConsultationSession>()
                        .eq(ConsultationSession::getId, sessionId)
                        .set(ConsultationSession::getLastEmotionAnalysis, json)
                        .set(ConsultationSession::getLastEmotionUpdatedAt, LocalDateTime.now()));
                log.info("会话 {} 情绪分析已更新", sessionId);
            } catch (Exception e) {
                log.warn("会话 {} 情绪分析落库失败：{}", sessionId, e.getMessage());
            }
        });
    }

    /**
     * 异步分析一篇情绪日记，结果写回日记
     */
    public void analyzeDiaryAsync(Long diaryId, String text) {
        if (diaryId == null) {
            return;
        }
        CompletableFuture.runAsync(() -> {
            try {
                String json = analyzeToJson(text);
                if (json == null) {
                    return;
                }
                diaryMapper.update(null, new LambdaUpdateWrapper<EmotionDiary>()
                        .eq(EmotionDiary::getId, diaryId)
                        .set(EmotionDiary::getAiEmotionAnalysis, json));
                log.info("日记 {} 情绪分析已更新", diaryId);
            } catch (Exception e) {
                log.warn("日记 {} 情绪分析落库失败：{}", diaryId, e.getMessage());
            }
        });
    }

    /**
     * 模型经常把 JSON 包在 ```json 里或者前后加解释，这里只截取第一个 { 到最后一个 }
     */
    private String extractJson(String response) {
        if (response == null) {
            throw new IllegalArgumentException("模型返回为空");
        }
        int start = response.indexOf('{');
        int end = response.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new IllegalArgumentException("模型返回中未找到JSON");
        }
        return response.substring(start, end + 1);
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
