package com.example.aispring.service.engine;

import com.example.aispring.entity.CounselingAppointment;
import com.example.aispring.entity.CounselingOrder;
import com.example.aispring.enumClass.AppointmentStatus;
import com.example.aispring.enumClass.OrderStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 退款规则引擎（纯函数，无数据库依赖）
 * 规则：
 * 1. 咨询师未确认 -> 全额退
 * 2. 已确认且距预约开始 >24h -> 全额退
 * 3. 已确认且距预约开始 2~24h -> 退 50%
 * 4. <2h / 已开始 / 已完成 / 已取消 / 已拒绝 -> 不可退
 */
@Component
public class RefundRuleEngine {

    public record RefundDecision(boolean refundable,
                                 BigDecimal refundAmount,
                                 String ruleDesc,
                                 String rejectReason) {

        public static RefundDecision full(BigDecimal amount, String desc) {
            return new RefundDecision(true, amount.setScale(2, RoundingMode.HALF_UP), desc, null);
        }

        public static RefundDecision partial(BigDecimal amount, String desc) {
            return new RefundDecision(true, amount.setScale(2, RoundingMode.HALF_UP), desc, null);
        }

        public static RefundDecision reject(String reason) {
            return new RefundDecision(false, BigDecimal.ZERO.setScale(2), null, reason);
        }
    }

    public RefundDecision evaluate(CounselingOrder order, CounselingAppointment appointment, LocalDateTime now) {
        if (order == null) {
            return RefundDecision.reject("订单不存在");
        }
        boolean paidLike = OrderStatus.PAID.getCode().equals(order.getStatus())
                || OrderStatus.REFUNDING.getCode().equals(order.getStatus());
        if (!paidLike) {
            return RefundDecision.reject("订单当前状态不可退款");
        }
        if (appointment == null) {
            return RefundDecision.reject("预约不存在");
        }

        if (AppointmentStatus.PENDING.getCode().equals(appointment.getStatus())) {
            return RefundDecision.full(order.getAmount(), "咨询师尚未确认，全额退款");
        }

        if (AppointmentStatus.CONFIRMED.getCode().equals(appointment.getStatus())) {
            long minutes = Duration.between(now, appointment.getAppointmentStart()).toMinutes();
            if (minutes > 24 * 60) {
                return RefundDecision.full(order.getAmount(), "距预约开始超过24小时，全额退款");
            }
            if (minutes >= 2 * 60) {
                BigDecimal half = order.getAmount().multiply(new BigDecimal("0.5"));
                return RefundDecision.partial(half, "距预约开始2~24小时，退款50%");
            }
            return RefundDecision.reject("距预约开始不足2小时或咨询已开始，不可退款");
        }

        return RefundDecision.reject("预约已结束或已取消，不可退款");
    }
}
