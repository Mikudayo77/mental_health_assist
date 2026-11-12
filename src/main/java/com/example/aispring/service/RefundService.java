package com.example.aispring.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.aispring.DTO.command.RefundApplyCommandDTO;
import com.example.aispring.DTO.command.RefundAuditCommandDTO;
import com.example.aispring.DTO.response.RefundPreviewResponseDTO;
import com.example.aispring.DTO.response.RefundResponseDTO;
import com.example.aispring.common.PageResult;
import com.example.aispring.entity.CounselingAppointment;
import com.example.aispring.entity.CounselingOrder;
import com.example.aispring.entity.CounselingRefund;
import com.example.aispring.enumClass.AppointmentStatus;
import com.example.aispring.enumClass.NotificationType;
import com.example.aispring.enumClass.OrderStatus;
import com.example.aispring.enumClass.RefundStatus;
import com.example.aispring.exception.BusinessException;
import com.example.aispring.mapper.CounselingAppointmentMapper;
import com.example.aispring.mapper.CounselingOrderMapper;
import com.example.aispring.mapper.CounselingRefundMapper;
import com.example.aispring.service.convert.CounselingConvert;
import com.example.aispring.service.engine.RefundRuleEngine;
import com.example.aispring.util.OrderNoGenerator;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class RefundService {

    @Resource
    private CounselingRefundMapper refundMapper;

    @Resource
    private CounselingOrderMapper orderMapper;

    @Resource
    private CounselingAppointmentMapper appointmentMapper;

    @Resource
    private RefundRuleEngine refundRuleEngine;

    @Resource
    private SystemNotificationService notificationService;

    public RefundPreviewResponseDTO preview(Long userId, Long orderId) {
        CounselingOrder order = getOwnedOrder(userId, orderId);
        if (!OrderStatus.PAID.getCode().equals(order.getStatus())) {
            return new RefundPreviewResponseDTO(false, BigDecimal.ZERO.setScale(2),
                    null, "订单当前状态不可申请退款");
        }
        CounselingAppointment appointment = appointmentMapper.selectById(order.getAppointmentId());
        RefundRuleEngine.RefundDecision decision =
                refundRuleEngine.evaluate(order, appointment, LocalDateTime.now());
        return new RefundPreviewResponseDTO(decision.refundable(), decision.refundAmount(),
                decision.ruleDesc(), decision.rejectReason());
    }

    @Transactional
    public void apply(Long userId, RefundApplyCommandDTO dto) {
        CounselingOrder order = getOwnedOrder(userId, dto.getOrderId());
        if (!OrderStatus.PAID.getCode().equals(order.getStatus())) {
            throw new BusinessException("订单当前状态不可申请退款");
        }
        Long pending = refundMapper.selectCount(new LambdaQueryWrapper<CounselingRefund>()
                .eq(CounselingRefund::getOrderId, order.getId())
                .eq(CounselingRefund::getStatus, RefundStatus.PENDING.getCode()));
        if (pending != null && pending > 0) {
            throw new BusinessException("已有退款申请正在审核中");
        }

        CounselingAppointment appointment = appointmentMapper.selectById(order.getAppointmentId());
        LocalDateTime now = LocalDateTime.now();
        RefundRuleEngine.RefundDecision decision =
                refundRuleEngine.evaluate(order, appointment, now);
        if (!decision.refundable()) {
            throw new BusinessException(decision.rejectReason());
        }

        int rows = orderMapper.update(null, new LambdaUpdateWrapper<CounselingOrder>()
                .eq(CounselingOrder::getId, order.getId())
                .eq(CounselingOrder::getStatus, OrderStatus.PAID.getCode())
                .set(CounselingOrder::getStatus, OrderStatus.REFUNDING.getCode())
                .set(CounselingOrder::getUpdatedAt, now));
        if (rows == 0) {
            throw new BusinessException("订单状态已变更，请刷新后重试");
        }

        refundMapper.insert(CounselingRefund.builder()
                .refundNo(OrderNoGenerator.gen("RF"))
                .orderId(order.getId())
                .orderNo(order.getOrderNo())
                .userId(userId)
                .refundAmount(decision.refundAmount())
                .ruleDesc(decision.ruleDesc())
                .status(RefundStatus.PENDING.getCode())
                .reason(dto.getReason())
                .appliedAt(now)
                .createdAt(now)
                .build());

        notificationService.notifyAdmins(NotificationType.REFUND.getCode(),
                "有新的退款申请待审核",
                "订单 " + order.getOrderNo() + " 提交了退款申请，申请金额 "
                        + decision.refundAmount() + " 元（" + decision.ruleDesc() + "）。",
                order.getOrderNo());
    }

    @Transactional
    public void audit(Long adminId, RefundAuditCommandDTO dto) {
        CounselingRefund refund = refundMapper.selectById(dto.getRefundId());
        if (refund == null) {
            throw new BusinessException("退款记录不存在");
        }
        if (!RefundStatus.PENDING.getCode().equals(refund.getStatus())) {
            throw new BusinessException("该退款申请已处理");
        }
        CounselingOrder order = orderMapper.selectById(refund.getOrderId());
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        CounselingAppointment appointment = appointmentMapper.selectById(order.getAppointmentId());
        LocalDateTime now = LocalDateTime.now();

        if (!Boolean.TRUE.equals(dto.getApprove())) {
            rejectRefund(adminId, refund, order, now,
                    dto.getRejectReason() == null ? "管理员驳回" : dto.getRejectReason());
            return;
        }

        // 审核时按当前时间重新计算，防止申请与审核跨时间档导致金额漂移
        RefundRuleEngine.RefundDecision decision =
                refundRuleEngine.evaluate(order, appointment, now);
        if (!decision.refundable()) {
            rejectRefund(adminId, refund, order, now, decision.rejectReason());
            return;
        }

        refundMapper.update(null, new LambdaUpdateWrapper<CounselingRefund>()
                .eq(CounselingRefund::getId, refund.getId())
                .eq(CounselingRefund::getStatus, RefundStatus.PENDING.getCode())
                .set(CounselingRefund::getStatus, RefundStatus.SUCCESS.getCode())
                .set(CounselingRefund::getRefundAmount, decision.refundAmount())
                .set(CounselingRefund::getRuleDesc, decision.ruleDesc())
                .set(CounselingRefund::getAuditedBy, adminId)
                .set(CounselingRefund::getAuditedAt, now));

        BigDecimal refunded = order.getRefundedAmount() == null
                ? BigDecimal.ZERO : order.getRefundedAmount();
        orderMapper.update(null, new LambdaUpdateWrapper<CounselingOrder>()
                .eq(CounselingOrder::getId, order.getId())
                .set(CounselingOrder::getStatus, OrderStatus.REFUNDED.getCode())
                .set(CounselingOrder::getRefundedAmount, refunded.add(decision.refundAmount()))
                .set(CounselingOrder::getUpdatedAt, now));

        cancelAppointment(appointment, "退款审核通过，预约取消", now);

        notificationService.create(NotificationType.REFUND.getCode(), refund.getUserId(),
                "退款成功",
                "您的退款申请已通过，退款金额 " + decision.refundAmount() + " 元。",
                refund.getOrderNo());
    }

    /**
     * 系统自动全额退款（预约被拒绝/用户取消已支付预约）
     */
    @Transactional
    public void autoRefund(CounselingOrder order, CounselingAppointment appointment, String ruleDesc) {
        if (order == null || !OrderStatus.PAID.getCode().equals(order.getStatus())) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        int rows = orderMapper.update(null, new LambdaUpdateWrapper<CounselingOrder>()
                .eq(CounselingOrder::getId, order.getId())
                .eq(CounselingOrder::getStatus, OrderStatus.PAID.getCode())
                .set(CounselingOrder::getStatus, OrderStatus.REFUNDED.getCode())
                .set(CounselingOrder::getRefundedAmount, order.getAmount())
                .set(CounselingOrder::getUpdatedAt, now));
        if (rows == 0) {
            return;
        }
        refundMapper.insert(CounselingRefund.builder()
                .refundNo(OrderNoGenerator.gen("RF"))
                .orderId(order.getId())
                .orderNo(order.getOrderNo())
                .userId(order.getUserId())
                .refundAmount(order.getAmount().setScale(2))
                .ruleDesc(ruleDesc)
                .status(RefundStatus.SUCCESS.getCode())
                .auditedBy(0L)
                .appliedAt(now)
                .auditedAt(now)
                .createdAt(now)
                .build());
        notificationService.create(NotificationType.REFUND.getCode(), order.getUserId(),
                "退款已原路退回",
                "订单 " + order.getOrderNo() + " 已全额退款 " + order.getAmount() + " 元。",
                order.getOrderNo());
    }

    public PageResult<RefundResponseDTO> pageMine(Long userId, int page, int size) {
        Page<CounselingRefund> pageParam = new Page<>(page, size);
        Page<CounselingRefund> result = refundMapper.selectPage(pageParam,
                new LambdaQueryWrapper<CounselingRefund>()
                        .eq(CounselingRefund::getUserId, userId)
                        .orderByDesc(CounselingRefund::getCreatedAt));
        List<RefundResponseDTO> records = result.getRecords().stream()
                .map(CounselingConvert::toRefundResponse)
                .toList();
        return PageResult.of(result.getTotal(), records);
    }

    public PageResult<RefundResponseDTO> pageAll(int page, int size, Integer status) {
        Page<CounselingRefund> pageParam = new Page<>(page, size);
        Page<CounselingRefund> result = refundMapper.selectPage(pageParam,
                new LambdaQueryWrapper<CounselingRefund>()
                        .eq(status != null, CounselingRefund::getStatus, status)
                        .orderByDesc(CounselingRefund::getCreatedAt));
        List<RefundResponseDTO> records = result.getRecords().stream()
                .map(CounselingConvert::toRefundResponse)
                .toList();
        return PageResult.of(result.getTotal(), records);
    }

    private CounselingOrder getOwnedOrder(Long userId, Long orderId) {
        CounselingOrder order = orderMapper.selectOne(new LambdaQueryWrapper<CounselingOrder>()
                .eq(CounselingOrder::getId, orderId)
                .eq(CounselingOrder::getUserId, userId));
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        return order;
    }

    private void rejectRefund(Long adminId, CounselingRefund refund, CounselingOrder order,
                              LocalDateTime now, String reason) {
        refundMapper.update(null, new LambdaUpdateWrapper<CounselingRefund>()
                .eq(CounselingRefund::getId, refund.getId())
                .eq(CounselingRefund::getStatus, RefundStatus.PENDING.getCode())
                .set(CounselingRefund::getStatus, RefundStatus.REJECTED.getCode())
                .set(CounselingRefund::getRejectReason, reason)
                .set(CounselingRefund::getAuditedBy, adminId)
                .set(CounselingRefund::getAuditedAt, now));
        orderMapper.update(null, new LambdaUpdateWrapper<CounselingOrder>()
                .eq(CounselingOrder::getId, order.getId())
                .eq(CounselingOrder::getStatus, OrderStatus.REFUNDING.getCode())
                .set(CounselingOrder::getStatus, OrderStatus.PAID.getCode())
                .set(CounselingOrder::getUpdatedAt, now));
        notificationService.create(NotificationType.REFUND.getCode(), refund.getUserId(),
                "退款申请被拒绝",
                "您的退款申请未通过，原因：" + reason,
                refund.getOrderNo());
    }

    private void cancelAppointment(CounselingAppointment appointment, String reason, LocalDateTime now) {
        if (appointment == null) {
            return;
        }
        boolean cancelable = AppointmentStatus.PENDING.getCode().equals(appointment.getStatus())
                || AppointmentStatus.CONFIRMED.getCode().equals(appointment.getStatus());
        if (!cancelable) {
            return;
        }
        appointmentMapper.update(null, new LambdaUpdateWrapper<CounselingAppointment>()
                .eq(CounselingAppointment::getId, appointment.getId())
                .set(CounselingAppointment::getStatus, AppointmentStatus.CANCELED.getCode())
                .set(CounselingAppointment::getCancelReason, reason)
                .set(CounselingAppointment::getUpdatedAt, now));
    }
}
