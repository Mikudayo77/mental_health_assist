package com.example.aispring.enumClass;

import lombok.Getter;

@Getter
public enum CrisisRiskLevel {

    LOW("LOW", "低风险"),
    MEDIUM("MEDIUM", "中风险"),
    HIGH("HIGH", "高风险");

    private final String code;
    private final String description;

    CrisisRiskLevel(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public static CrisisRiskLevel fromCode(String code) {
        if (code != null) {
            for (CrisisRiskLevel level : CrisisRiskLevel.values()) {
                if (level.getCode().equalsIgnoreCase(code.trim())) {
                    return level;
                }
            }
        }
        throw new IllegalArgumentException("未知的危机风险等级: " + code);
    }

    public static String descriptionOf(String code) {
        try {
            return fromCode(code).getDescription();
        } catch (IllegalArgumentException e) {
            return "未知";
        }
    }
}
