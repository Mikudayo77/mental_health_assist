package com.example.aispring.enumClass;

import lombok.Getter;

@Getter
public enum CounselorAuditStatus {

    PENDING(0, "待审核"),
    APPROVED(1, "已通过"),
    REJECTED(2, "已拒绝");

    private final Integer code;
    private final String description;

    CounselorAuditStatus(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    public static CounselorAuditStatus fromCode(Integer code) {
        for (CounselorAuditStatus status : CounselorAuditStatus.values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知的咨询师审核状态代码: " + code);
    }

    public static String descriptionOf(Integer code) {
        try {
            return fromCode(code).getDescription();
        } catch (IllegalArgumentException e) {
            return "未知";
        }
    }
}
