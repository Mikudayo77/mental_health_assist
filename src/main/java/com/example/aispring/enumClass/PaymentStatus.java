package com.example.aispring.enumClass;

import lombok.Getter;

@Getter
public enum PaymentStatus {

    FAIL(0, "失败"),
    SUCCESS(1, "成功");

    private final Integer code;
    private final String description;

    PaymentStatus(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    public static PaymentStatus fromCode(Integer code) {
        for (PaymentStatus status : PaymentStatus.values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知的支付状态代码: " + code);
    }
}
