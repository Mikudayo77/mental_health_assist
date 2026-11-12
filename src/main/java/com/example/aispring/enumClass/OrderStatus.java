package com.example.aispring.enumClass;

import lombok.Getter;

@Getter
public enum OrderStatus {

    UNPAID(0, "未支付"),
    PAID(1, "已支付"),
    REFUNDING(2, "退款中"),
    REFUNDED(3, "已退款"),
    CANCELED(4, "已取消");

    private final Integer code;
    private final String description;

    OrderStatus(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    public static OrderStatus fromCode(Integer code) {
        for (OrderStatus status : OrderStatus.values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知的订单状态代码: " + code);
    }
}
