package com.example.aispring.enumClass;

import lombok.Getter;

@Getter
public enum NotificationType {

    APPOINTMENT(1, "预约通知"),
    REFUND(2, "退款通知"),
    CRISIS(3, "危机预警"),
    AUDIT(4, "审核通知");

    private final Integer code;
    private final String description;

    NotificationType(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    public static NotificationType fromCode(Integer code) {
        for (NotificationType type : NotificationType.values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知的通知类型代码: " + code);
    }

    public static String descriptionOf(Integer code) {
        try {
            return fromCode(code).getDescription();
        } catch (IllegalArgumentException e) {
            return "未知";
        }
    }
}
