package com.example.aispring.enumClass;

import lombok.Getter;

@Getter
public enum CrisisHandleStatus {

    PENDING(0, "待处理"),
    HANDLED(1, "已处理");

    private final Integer code;
    private final String description;

    CrisisHandleStatus(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    public static CrisisHandleStatus fromCode(Integer code) {
        for (CrisisHandleStatus status : CrisisHandleStatus.values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知的危机处理状态代码: " + code);
    }

    public static String descriptionOf(Integer code) {
        try {
            return fromCode(code).getDescription();
        } catch (IllegalArgumentException e) {
            return "未知";
        }
    }
}
