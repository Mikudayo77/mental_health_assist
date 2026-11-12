package com.example.aispring.service;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.aispring.DTO.response.PaymentResponseDTO;
import com.example.aispring.entity.CounselingAppointment;
import com.example.aispring.entity.CounselingOrder;
import com.example.aispring.entity.CounselingPayment;
import com.example.aispring.entity.Counselor;
import com.example.aispring.enumClass.NotificationType;
import com.example.aispring.enumClass.OrderStatus;
import com.example.aispring.enumClass.PaymentStatus;
import com.example.aispring.exception.BusinessException;
import com.example.aispring.mapper.CounselingAppointmentMapper;
import com.example.aispring.mapper.CounselingOrderMapper;
import com.example.aispring.mapper.CounselingPaymentMapper;
import com.example.aispring.mapper.CounselorMapper;
import com.example.aispring.service.convert.CounselingConvert;
import com.example.aispring.util.OrderNoGenerator;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PaymentService {

    @Resource
    private CounselingOrderMapper orderMapper;

    @Resource
    private CounselingPaymentMapper paymentMapper;

    @Resource
    private CounselingAppointmentMapper appointmentMapper;

    @Resource
    private CounselorMapper counselorMapper;

    @Resource
    private SystemNotificationService notificationService;

    // 模拟支付失败概率 0~1，0 表示总是成功
    @Value("${payment.mock.fail-rate:0}")
    private double failRate;

    /**
     * 模拟支付：乐观更新订单状态防重复支付，成功后落支付流水并通知咨询师
     */
    @Transactional
    public PaymentResponseDTO mockPay(String orderNo, Long userId) {
        CounselingOrder order = orderMapper.selectOne(new LambdaQueryWrapper<CounselingOrder>()
                .eq(CounselingOrder::getOrderNo, orderNo)
                .eq(CounselingOrder::getUserId, userId));
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        LocalDateTime now = LocalDateTime.now();
        int rows = orderMapper.update(null, new LambdaUpdateWrapper<CounselingOrder>()
                .eq(CounselingOrder::getId, order.getId())
                .eq(CounselingOrder::getStatus, OrderStatus.UNPAID.getCode())
                .set(CounselingOrder::getStatus, OrderStatus.PAID.getCode())
                .set(CounselingOrder::getPaidAt, now)
                .set(CounselingOrder::getUpdatedAt, now));
        if (rows == 0) {
            throw new BusinessException("订单状态异常或已支付");
        }
        if (failRate > 0 && RandomUtil.randomDouble() < failRate) {
            throw new BusinessException("模拟支付失败，请重试");
        }

        CounselingPayment payment = CounselingPayment.builder()
                .paymentNo(OrderNoGenerator.gen("PAY"))
                .orderId(order.getId())
                .orderNo(orderNo)
                .userId(userId)
                .amount(order.getAmount())
                .payMethod(1)
                .status(PaymentStatus.SUCCESS.getCode())
                .transactionNo("MOCK" + OrderNoGenerator.gen(""))
                .paidAt(now)
                .createdAt(now)
                .build();
        paymentMapper.insert(payment);

        notifyCounselor(order, now);
        return CounselingConvert.toPaymentResponse(payment);
    }

    private void notifyCounselor(CounselingOrder order, LocalDateTime now) {
        CounselingAppointment appointment = appointmentMapper.selectById(order.getAppointmentId());
        if (appointment == null) {
            return;
        }
        Counselor counselor = counselorMapper.selectById(appointment.getCounselorId());
        if (counselor == null) {
            return;
        }
        notificationService.create(NotificationType.APPOINTMENT.getCode(), counselor.getUserId(),
                "您有新的预约待确认",
                "用户已完成支付，请及时确认预约。预约时间：" + appointment.getAppointmentStart(),
                appointment.getAppointmentNo());
    }

    public List<PaymentResponseDTO> listByOrder(String orderNo, Long userId) {
        CounselingOrder order = orderMapper.selectOne(new LambdaQueryWrapper<CounselingOrder>()
                .eq(CounselingOrder::getOrderNo, orderNo)
                .eq(CounselingOrder::getUserId, userId));
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        return paymentMapper.selectList(new LambdaQueryWrapper<CounselingPayment>()
                        .eq(CounselingPayment::getOrderId, order.getId())
                        .orderByDesc(CounselingPayment::getCreatedAt))
                .stream()
                .map(CounselingConvert::toPaymentResponse)
                .toList();
    }
}
