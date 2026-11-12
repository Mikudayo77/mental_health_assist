package com.example.aispring.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.aispring.DTO.command.AppointmentCreateCommandDTO;
import com.example.aispring.DTO.response.AppointmentResponseDTO;
import com.example.aispring.DTO.response.OrderCreateResponseDTO;
import com.example.aispring.common.PageResult;
import com.example.aispring.entity.CounselingAppointment;
import com.example.aispring.entity.CounselingOrder;
import com.example.aispring.entity.CounselingPackage;
import com.example.aispring.entity.Counselor;
import com.example.aispring.enumClass.AppointmentStatus;
import com.example.aispring.enumClass.CounselorAuditStatus;
import com.example.aispring.enumClass.NotificationType;
import com.example.aispring.enumClass.OrderStatus;
import com.example.aispring.exception.BusinessException;
import com.example.aispring.mapper.CounselingAppointmentMapper;
import com.example.aispring.mapper.CounselingOrderMapper;
import com.example.aispring.service.convert.CounselingConvert;
import com.example.aispring.util.OrderNoGenerator;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AppointmentService {

    @Resource
    private CounselingAppointmentMapper appointmentMapper;

    @Resource
    private CounselingOrderMapper orderMapper;

    @Resource
    private CounselorService counselorService;

    @Resource
    private CounselingPackageService packageService;

    @Resource
    private CounselingOrderService orderService;

    @Resource
    private RefundService refundService;

    @Resource
    private SystemNotificationService notificationService;

    /**
     * 预约下单：同一事务内创建预约与未支付订单
     */
    @Transactional
    public OrderCreateResponseDTO createWithOrder(Long userId, AppointmentCreateCommandDTO dto) {
        CounselingPackage pkg = packageService.getOnSale(dto.getPackageId());
        Counselor counselor = counselorService.getById(dto.getCounselorId());
        if (counselor == null
                || !CounselorAuditStatus.APPROVED.getCode().equals(counselor.getAuditStatus())
                || !Integer.valueOf(1).equals(counselor.getStatus())) {
            throw new BusinessException("咨询师不存在或暂不可预约");
        }

        LocalDateTime start = dto.getAppointmentStart();
        if (!start.isAfter(LocalDateTime.now())) {
            throw new BusinessException("预约时间必须在未来");
        }
        LocalDateTime end = start.plusMinutes(pkg.getDurationMinutes());

        Long conflict = appointmentMapper.selectCount(new LambdaQueryWrapper<CounselingAppointment>()
                .eq(CounselingAppointment::getCounselorId, counselor.getId())
                .in(CounselingAppointment::getStatus,
                        AppointmentStatus.PENDING.getCode(), AppointmentStatus.CONFIRMED.getCode())
                .lt(CounselingAppointment::getAppointmentStart, end)
                .gt(CounselingAppointment::getAppointmentEnd, start));
        if (conflict != null && conflict > 0) {
            throw new BusinessException("该时段咨询师已有预约，请选择其他时间");
        }

        LocalDateTime now = LocalDateTime.now();
        CounselingAppointment appointment = CounselingAppointment.builder()
                .appointmentNo(OrderNoGenerator.gen("AP"))
                .userId(userId)
                .counselorId(counselor.getId())
                .packageId(pkg.getId())
                .appointmentStart(start)
                .appointmentEnd(end)
                .status(AppointmentStatus.PENDING.getCode())
                .createdAt(now)
                .updatedAt(now)
                .build();
        appointmentMapper.insert(appointment);

        CounselingOrder order = orderService.createOrder(userId, appointment, pkg);
        return new OrderCreateResponseDTO(order.getOrderNo(), appointment.getAppointmentNo(), order.getAmount());
    }

    @Transactional
    public void confirm(Long counselorUserId, Long appointmentId) {
        Counselor counselor = requireCounselor(counselorUserId);
        CounselingAppointment appointment = getExisting(appointmentId);
        checkCounselorOwns(counselor, appointment);

        LocalDateTime now = LocalDateTime.now();
        int rows = appointmentMapper.update(null, new LambdaUpdateWrapper<CounselingAppointment>()
                .eq(CounselingAppointment::getId, appointmentId)
                .eq(CounselingAppointment::getStatus, AppointmentStatus.PENDING.getCode())
                .set(CounselingAppointment::getStatus, AppointmentStatus.CONFIRMED.getCode())
                .set(CounselingAppointment::getConfirmedAt, now)
                .set(CounselingAppointment::getUpdatedAt, now));
        if (rows == 0) {
            throw new BusinessException("预约当前状态不可确认");
        }
        notificationService.create(NotificationType.APPOINTMENT.getCode(), appointment.getUserId(),
                "预约已确认",
                "咨询师已确认您的预约，咨询时间：" + appointment.getAppointmentStart(),
                appointment.getAppointmentNo());
    }

    @Transactional
    public void reject(Long counselorUserId, Long appointmentId, String reason) {
        Counselor counselor = requireCounselor(counselorUserId);
        CounselingAppointment appointment = getExisting(appointmentId);
        checkCounselorOwns(counselor, appointment);

        LocalDateTime now = LocalDateTime.now();
        String cancelReason = reason == null || reason.isBlank() ? "咨询师拒绝预约" : reason;
        int rows = appointmentMapper.update(null, new LambdaUpdateWrapper<CounselingAppointment>()
                .eq(CounselingAppointment::getId, appointmentId)
                .eq(CounselingAppointment::getStatus, AppointmentStatus.PENDING.getCode())
                .set(CounselingAppointment::getStatus, AppointmentStatus.REJECTED.getCode())
                .set(CounselingAppointment::getCancelReason, cancelReason)
                .set(CounselingAppointment::getUpdatedAt, now));
        if (rows == 0) {
            throw new BusinessException("预约当前状态不可拒绝");
        }

        CounselingOrder order = orderService.getByAppointmentId(appointmentId);
        refundService.autoRefund(order, appointment, "咨询师拒绝预约，全额退款");

        notificationService.create(NotificationType.APPOINTMENT.getCode(), appointment.getUserId(),
                "预约被拒绝",
                "咨询师拒绝了您的预约，原因：" + cancelReason
                        + (order != null && OrderStatus.PAID.getCode().equals(order.getStatus())
                        ? "。已支付金额将全额退回。" : "。"),
                appointment.getAppointmentNo());
    }

    @Transactional
    public void cancelByUser(Long userId, Long appointmentId, String reason) {
        CounselingAppointment appointment = getExisting(appointmentId);
        if (!appointment.getUserId().equals(userId)) {
            throw new BusinessException("无权操作该预约");
        }
        LocalDateTime now = LocalDateTime.now();
        String cancelReason = reason == null || reason.isBlank() ? "用户取消预约" : reason;
        int rows = appointmentMapper.update(null, new LambdaUpdateWrapper<CounselingAppointment>()
                .eq(CounselingAppointment::getId, appointmentId)
                .eq(CounselingAppointment::getStatus, AppointmentStatus.PENDING.getCode())
                .set(CounselingAppointment::getStatus, AppointmentStatus.CANCELED.getCode())
                .set(CounselingAppointment::getCancelReason, cancelReason)
                .set(CounselingAppointment::getUpdatedAt, now));
        if (rows == 0) {
            throw new BusinessException("预约已确认或已结束，取消请走退款流程");
        }

        CounselingOrder order = orderService.getByAppointmentId(appointmentId);
        if (order != null) {
            if (OrderStatus.PAID.getCode().equals(order.getStatus())) {
                refundService.autoRefund(order, appointment, "用户取消预约，全额退款");
            } else if (OrderStatus.UNPAID.getCode().equals(order.getStatus())) {
                orderMapper.update(null, new LambdaUpdateWrapper<CounselingOrder>()
                        .eq(CounselingOrder::getId, order.getId())
                        .eq(CounselingOrder::getStatus, OrderStatus.UNPAID.getCode())
                        .set(CounselingOrder::getStatus, OrderStatus.CANCELED.getCode())
                        .set(CounselingOrder::getCanceledAt, now)
                        .set(CounselingOrder::getUpdatedAt, now));
            }
        }

        Counselor counselor = counselorService.getById(appointment.getCounselorId());
        if (counselor != null) {
            notificationService.create(NotificationType.APPOINTMENT.getCode(), counselor.getUserId(),
                    "预约已取消",
                    "用户取消了预约，时间：" + appointment.getAppointmentStart() + "，原因：" + cancelReason,
                    appointment.getAppointmentNo());
        }
    }

    @Transactional
    public void complete(Long counselorUserId, Long appointmentId) {
        Counselor counselor = requireCounselor(counselorUserId);
        CounselingAppointment appointment = getExisting(appointmentId);
        checkCounselorOwns(counselor, appointment);

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(appointment.getAppointmentStart())) {
            throw new BusinessException("咨询尚未开始，不能标记完成");
        }
        int rows = appointmentMapper.update(null, new LambdaUpdateWrapper<CounselingAppointment>()
                .eq(CounselingAppointment::getId, appointmentId)
                .eq(CounselingAppointment::getStatus, AppointmentStatus.CONFIRMED.getCode())
                .set(CounselingAppointment::getStatus, AppointmentStatus.COMPLETED.getCode())
                .set(CounselingAppointment::getCompletedAt, now)
                .set(CounselingAppointment::getUpdatedAt, now));
        if (rows == 0) {
            throw new BusinessException("仅已确认的预约可标记完成");
        }
        notificationService.create(NotificationType.APPOINTMENT.getCode(), appointment.getUserId(),
                "咨询已完成",
                "您的咨询已完成，感谢使用心理健康管家。",
                appointment.getAppointmentNo());
    }

    public PageResult<AppointmentResponseDTO> pageMine(Long userId, int page, int size, Integer status) {
        Page<CounselingAppointment> pageParam = new Page<>(page, size);
        Page<CounselingAppointment> result = appointmentMapper.selectPage(pageParam,
                new LambdaQueryWrapper<CounselingAppointment>()
                        .eq(CounselingAppointment::getUserId, userId)
                        .eq(status != null, CounselingAppointment::getStatus, status)
                        .orderByDesc(CounselingAppointment::getAppointmentStart));
        return toPageResult(result);
    }

    public PageResult<AppointmentResponseDTO> pageCounselor(Long counselorUserId, int page, int size, Integer status) {
        Counselor counselor = requireCounselor(counselorUserId);
        Page<CounselingAppointment> pageParam = new Page<>(page, size);
        Page<CounselingAppointment> result = appointmentMapper.selectPage(pageParam,
                new LambdaQueryWrapper<CounselingAppointment>()
                        .eq(CounselingAppointment::getCounselorId, counselor.getId())
                        .eq(status != null, CounselingAppointment::getStatus, status)
                        .orderByDesc(CounselingAppointment::getAppointmentStart));
        return toPageResult(result);
    }

    public AppointmentResponseDTO getDetail(Long appointmentId, Long currentUserId) {
        CounselingAppointment appointment = getExisting(appointmentId);
        boolean isOwner = appointment.getUserId().equals(currentUserId);
        Counselor counselor = counselorService.getApprovedByUserId(currentUserId);
        boolean isCounselor = counselor != null && counselor.getId().equals(appointment.getCounselorId());
        if (!isOwner && !isCounselor) {
            throw new BusinessException("无权查看该预约");
        }
        return toResponse(appointment);
    }

    public CounselingAppointment getExisting(Long appointmentId) {
        CounselingAppointment appointment = appointmentMapper.selectById(appointmentId);
        if (appointment == null) {
            throw new BusinessException("预约不存在");
        }
        return appointment;
    }

    private Counselor requireCounselor(Long userId) {
        Counselor counselor = counselorService.getApprovedByUserId(userId);
        if (counselor == null) {
            throw new BusinessException("当前账号不是有效咨询师");
        }
        return counselor;
    }

    private void checkCounselorOwns(Counselor counselor, CounselingAppointment appointment) {
        if (!counselor.getId().equals(appointment.getCounselorId())) {
            throw new BusinessException("无权操作该预约");
        }
    }

    private PageResult<AppointmentResponseDTO> toPageResult(Page<CounselingAppointment> result) {
        List<AppointmentResponseDTO> records = result.getRecords().stream()
                .map(this::toResponse)
                .toList();
        return PageResult.of(result.getTotal(), records);
    }

    private AppointmentResponseDTO toResponse(CounselingAppointment appointment) {
        String counselorName = null;
        Counselor counselor = counselorService.getById(appointment.getCounselorId());
        if (counselor != null) {
            counselorName = counselor.getRealName();
        }
        String packageName = null;
        CounselingPackage pkg = packageService.getById(appointment.getPackageId());
        if (pkg != null) {
            packageName = pkg.getName();
        }
        return CounselingConvert.toAppointmentResponse(appointment, counselorName, packageName);
    }
}
