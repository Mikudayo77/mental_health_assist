package com.example.aispring.DTO.response;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 会话情绪面板数据，字段对齐前端 consultation.vue 的"情绪花园"
 */
@Data
@Builder
public class SessionEmotionResponseDTO {
    // 主要情绪
    private String primaryEmotion;

    // 情绪强度 0-100
    private Integer emotionScore;

    // 是否为负面情绪
    private Boolean isNegative;

    // 风险等级 0正常 1关注 2预警 3危机
    private Integer riskLevel;

    // 给用户的建议
    private String suggestion;

    // 治愈小行动（前端会取 .length，不能为 null）
    private List<String> improvementSuggestions;

    // 风险提示文案
    private String riskDescription;

    /**
     * 没有任何情绪分析数据时的中性兜底，保证前端面板可正常渲染
     */
    public static SessionEmotionResponseDTO neutral() {
        return SessionEmotionResponseDTO.builder()
                .primaryEmotion("中性")
                .emotionScore(50)
                .isNegative(false)
                .riskLevel(0)
                .suggestion("")
                .improvementSuggestions(new ArrayList<>())
                .riskDescription("")
                .build();
    }
}
