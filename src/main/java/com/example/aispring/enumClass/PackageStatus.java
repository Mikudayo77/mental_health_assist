package com.example.aispring.enumClass;

import lombok.Getter;

@Getter
public enum PackageStatus {

    OFF(0, "已下架"),
    ON(1, "已上架");

    private final Integer code;
    private final String description;

    PackageStatus(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    public static PackageStatus fromCode(Integer code) {
        for (PackageStatus status : PackageStatus.values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知的套餐状态代码: " + code);
    }

    public static String descriptionOf(Integer code) {
        try {
            return fromCode(code).getDescription();
        } catch (IllegalArgumentException e) {
            return "未知";
        }
    }
}
