package com.example.aispring.enumClass;

import lombok.Getter;

@Getter
public enum RefundStatus {

    PENDING(0, "待审核"),
    SUCCESS(1, "已退款"),
    REJECTED(2, "已拒绝");

    private final Integer code;
    private final String description;

    RefundStatus(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    public static RefundStatus fromCode(Integer code) {
        for (RefundStatus status : RefundStatus.values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知的退款状态代码: " + code);
    }

    public static String descriptionOf(Integer code) {
        try {
            return fromCode(code).getDescription();
        } catch (IllegalArgumentException e) {
            return "未知";
        }
    }
}
