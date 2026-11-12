package com.example.aispring.enumClass;

import lombok.Getter;

@Getter
public enum CrisisSourceType {

    AI_CHAT(1, "AI聊天"),
    HUMAN_CHAT(2, "人工咨询");

    private final Integer code;
    private final String description;

    CrisisSourceType(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    public static CrisisSourceType fromCode(Integer code) {
        for (CrisisSourceType type : CrisisSourceType.values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知的危机来源类型: " + code);
    }

    public static String descriptionOf(Integer code) {
        try {
            return fromCode(code).getDescription();
        } catch (IllegalArgumentException e) {
            return "未知";
        }
    }
}
