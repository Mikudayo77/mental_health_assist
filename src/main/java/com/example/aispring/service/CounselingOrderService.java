package com.example.aispring.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.aispring.DTO.response.OrderResponseDTO;
import com.example.aispring.common.PageResult;
import com.example.aispring.entity.CounselingAppointment;
import com.example.aispring.entity.CounselingOrder;
import com.example.aispring.entity.CounselingPackage;
import com.example.aispring.entity.CounselingPayment;
import com.example.aispring.entity.CounselingRefund;
import com.example.aispring.enumClass.AppointmentStatus;
import com.example.aispring.enumClass.OrderStatus;
import com.example.aispring.exception.BusinessException;
import com.example.aispring.mapper.CounselingAppointmentMapper;
import com.example.aispring.mapper.CounselingOrderMapper;
import com.example.aispring.mapper.CounselingPaymentMapper;
import com.example.aispring.mapper.CounselingRefundMapper;
import com.example.aispring.service.convert.CounselingConvert;
import com.example.aispring.util.OrderNoGenerator;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class CounselingOrderService {

    @Resource
    private CounselingOrderMapper orderMapper;

    @Resource
    private CounselingAppointmentMapper appointmentMapper;

    @Resource
    private CounselingPaymentMapper paymentMapper;

    @Resource
    private CounselingRefundMapper refundMapper;

    /**
     * 下单（由预约服务在创建预约后调用，同一事务内）
     */
    public CounselingOrder createOrder(Long userId, CounselingAppointment appointment, CounselingPackage pkg) {
        LocalDateTime now = LocalDateTime.now();
        CounselingOrder order = CounselingOrder.builder()
                .orderNo(OrderNoGenerator.gen("CO"))
                .userId(userId)
                .appointmentId(appointment.getId())
                .packageId(pkg.getId())
                .packageName(pkg.getName())
                .packageDuration(pkg.getDurationMinutes())
                .amount(pkg.getPrice())
                .refundedAmount(BigDecimal.ZERO.setScale(2))
                .status(OrderStatus.UNPAID.getCode())
                .createdAt(now)
                .updatedAt(now)
                .build();
        orderMapper.insert(order);
        return order;
    }

    public PageResult<OrderResponseDTO> pageMine(Long userId, int page, int size, Integer status) {
        Page<CounselingOrder> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<CounselingOrder> wrapper = new LambdaQueryWrapper<CounselingOrder>()
                .eq(CounselingOrder::getUserId, userId)
                .eq(status != null, CounselingOrder::getStatus, status)
                .orderByDesc(CounselingOrder::getCreatedAt);
        Page<CounselingOrder> result = orderMapper.selectPage(pageParam, wrapper);
        List<OrderResponseDTO> records = result.getRecords().stream()
                .map(CounselingConvert::toOrderResponse)
                .toList();
        return PageResult.of(result.getTotal(), records);
    }

    public OrderResponseDTO getDetail(String orderNo, Long userId) {
        CounselingOrder order = orderMapper.selectOne(new LambdaQueryWrapper<CounselingOrder>()
                .eq(CounselingOrder::getOrderNo, orderNo)
                .eq(CounselingOrder::getUserId, userId));
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        OrderResponseDTO dto = CounselingConvert.toOrderResponse(order);
        dto.setPayments(paymentMapper.selectList(new LambdaQueryWrapper<CounselingPayment>()
                        .eq(CounselingPayment::getOrderId, order.getId())
                        .orderByDesc(CounselingPayment::getCreatedAt))
                .stream().map(CounselingConvert::toPaymentResponse).toList());
        dto.setRefunds(refundMapper.selectList(new LambdaQueryWrapper<CounselingRefund>()
                        .eq(CounselingRefund::getOrderId, order.getId())
                        .orderByDesc(CounselingRefund::getCreatedAt))
                .stream().map(CounselingConvert::toRefundResponse).toList());
        return dto;
    }

    /**
     * 取消未支付订单（同时取消对应的待确认预约）
     */
    @Transactional
    public void cancelUnpaid(Long userId, String orderNo) {
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
                .set(CounselingOrder::getStatus, OrderStatus.CANCELED.getCode())
                .set(CounselingOrder::getCanceledAt, now)
                .set(CounselingOrder::getUpdatedAt, now));
        if (rows == 0) {
            throw new BusinessException("订单已支付或已取消，无法取消");
        }
        appointmentMapper.update(null, new LambdaUpdateWrapper<CounselingAppointment>()
                .eq(CounselingAppointment::getId, order.getAppointmentId())
                .eq(CounselingAppointment::getStatus, AppointmentStatus.PENDING.getCode())
                .set(CounselingAppointment::getStatus, AppointmentStatus.CANCELED.getCode())
                .set(CounselingAppointment::getCancelReason, "用户取消未支付订单")
                .set(CounselingAppointment::getUpdatedAt, now));
    }

    public CounselingOrder getByOrderNo(String orderNo, Long userId) {
        return orderMapper.selectOne(new LambdaQueryWrapper<CounselingOrder>()
                .eq(CounselingOrder::getOrderNo, orderNo)
                .eq(CounselingOrder::getUserId, userId));
    }

    public CounselingOrder getByAppointmentId(Long appointmentId) {
        return orderMapper.selectOne(new LambdaQueryWrapper<CounselingOrder>()
                .eq(CounselingOrder::getAppointmentId, appointmentId));
    }
}
