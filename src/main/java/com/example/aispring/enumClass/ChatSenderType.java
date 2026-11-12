package com.example.aispring.enumClass;

import lombok.Getter;

/**
 * 人工咨询消息发送者类型
 * 注意：AI 聊天 consultation_message 的 sender_type=2 表示 AI 助手，与本枚举语义独立
 */
@Getter
public enum ChatSenderType {

    USER(1, "用户"),
    COUNSELOR(2, "咨询师");

    private final Integer code;
    private final String description;

    ChatSenderType(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    public static ChatSenderType fromCode(Integer code) {
        for (ChatSenderType type : ChatSenderType.values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知的聊天发送者类型: " + code);
    }

    public static String descriptionOf(Integer code) {
        try {
            return fromCode(code).getDescription();
        } catch (IllegalArgumentException e) {
            return "未知";
        }
    }
}
