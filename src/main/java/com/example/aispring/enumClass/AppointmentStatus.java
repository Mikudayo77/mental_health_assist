package com.example.aispring.enumClass;

import lombok.Getter;

@Getter
public enum AppointmentStatus {

    PENDING(0, "待确认"),
    CONFIRMED(1, "已确认"),
    COMPLETED(2, "已完成"),
    CANCELED(3, "已取消"),
    REJECTED(4, "已拒绝");

    private final Integer code;
    private final String description;

    AppointmentStatus(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    public static AppointmentStatus fromCode(Integer code) {
        for (AppointmentStatus status : AppointmentStatus.values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知的预约状态代码: " + code);
    }

    public static String descriptionOf(Integer code) {
        try {
            return fromCode(code).getDescription();
        } catch (IllegalArgumentException e) {
            return "未知";
        }
    }
}
