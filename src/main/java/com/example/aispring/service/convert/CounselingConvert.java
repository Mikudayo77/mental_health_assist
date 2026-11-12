package com.example.aispring.service.convert;

import com.example.aispring.DTO.response.AppointmentResponseDTO;
import com.example.aispring.DTO.response.ChatMessageResponseDTO;
import com.example.aispring.DTO.response.CounselorResponseDTO;
import com.example.aispring.DTO.response.CrisisRecordResponseDTO;
import com.example.aispring.DTO.response.NotificationResponseDTO;
import com.example.aispring.DTO.response.OrderResponseDTO;
import com.example.aispring.DTO.response.PackageResponseDTO;
import com.example.aispring.DTO.response.PaymentResponseDTO;
import com.example.aispring.DTO.response.RefundResponseDTO;
import com.example.aispring.entity.CounselingAppointment;
import com.example.aispring.entity.CounselingChatMessage;
import com.example.aispring.entity.CounselingOrder;
import com.example.aispring.entity.CounselingPackage;
import com.example.aispring.entity.CounselingPayment;
import com.example.aispring.entity.CounselingRefund;
import com.example.aispring.entity.Counselor;
import com.example.aispring.entity.CrisisRecord;
import com.example.aispring.entity.SystemNotification;
import com.example.aispring.enumClass.AppointmentStatus;
import com.example.aispring.enumClass.ChatSenderType;
import com.example.aispring.enumClass.CounselorAuditStatus;
import com.example.aispring.enumClass.CrisisHandleStatus;
import com.example.aispring.enumClass.CrisisRiskLevel;
import com.example.aispring.enumClass.CrisisSourceType;
import com.example.aispring.enumClass.NotificationType;
import com.example.aispring.enumClass.OrderStatus;
import com.example.aispring.enumClass.PackageStatus;
import com.example.aispring.enumClass.RefundStatus;

/**
 * 心理咨询业务实体与 DTO 转换（显式逐字段映射，避免敏感字段被顺手带出）
 */
public class CounselingConvert {

    public static CounselorResponseDTO toCounselorResponse(Counselor counselor) {
        if (counselor == null) {
            return null;
        }
        return CounselorResponseDTO.builder()
                .id(counselor.getId())
                .userId(counselor.getUserId())
                .realName(counselor.getRealName())
                .gender(counselor.getGender())
                .phone(counselor.getPhone())
                .email(counselor.getEmail())
                .qualificationNo(counselor.getQualificationNo())
                .certificationFileUrl(counselor.getCertificationFileUrl())
                .specialty(counselor.getSpecialty())
                .introduction(counselor.getIntroduction())
                .yearsOfExperience(counselor.getYearsOfExperience())
                .auditStatus(counselor.getAuditStatus())
                .auditStatusDesc(CounselorAuditStatus.descriptionOf(counselor.getAuditStatus()))
                .auditRemark(counselor.getAuditRemark())
                .status(counselor.getStatus())
                .createdAt(counselor.getCreatedAt())
                .build();
    }

    public static PackageResponseDTO toPackageResponse(CounselingPackage pkg) {
        if (pkg == null) {
            return null;
        }
        return PackageResponseDTO.builder()
                .id(pkg.getId())
                .name(pkg.getName())
                .description(pkg.getDescription())
                .durationMinutes(pkg.getDurationMinutes())
                .price(pkg.getPrice())
                .status(pkg.getStatus())
                .statusDesc(PackageStatus.descriptionOf(pkg.getStatus()))
                .createdAt(pkg.getCreatedAt())
                .build();
    }

    public static AppointmentResponseDTO toAppointmentResponse(CounselingAppointment appointment,
                                                               String counselorName,
                                                               String packageName) {
        if (appointment == null) {
            return null;
        }
        return AppointmentResponseDTO.builder()
                .id(appointment.getId())
                .appointmentNo(appointment.getAppointmentNo())
                .userId(appointment.getUserId())
                .counselorId(appointment.getCounselorId())
                .counselorName(counselorName)
                .packageId(appointment.getPackageId())
                .packageName(packageName)
                .appointmentStart(appointment.getAppointmentStart())
                .appointmentEnd(appointment.getAppointmentEnd())
                .status(appointment.getStatus())
                .statusDesc(AppointmentStatus.descriptionOf(appointment.getStatus()))
                .cancelReason(appointment.getCancelReason())
                .confirmedAt(appointment.getConfirmedAt())
                .completedAt(appointment.getCompletedAt())
                .createdAt(appointment.getCreatedAt())
                .build();
    }

    public static OrderResponseDTO toOrderResponse(CounselingOrder order) {
        if (order == null) {
            return null;
        }
        return OrderResponseDTO.builder()
                .id(order.getId())
                .orderNo(order.getOrderNo())
                .userId(order.getUserId())
                .appointmentId(order.getAppointmentId())
                .packageId(order.getPackageId())
                .packageName(order.getPackageName())
                .packageDuration(order.getPackageDuration())
                .amount(order.getAmount())
                .refundedAmount(order.getRefundedAmount())
                .status(order.getStatus())
                .statusDesc(OrderStatus.fromCode(order.getStatus()).getDescription())
                .paidAt(order.getPaidAt())
                .createdAt(order.getCreatedAt())
                .build();
    }

    public static PaymentResponseDTO toPaymentResponse(CounselingPayment payment) {
        if (payment == null) {
            return null;
        }
        return PaymentResponseDTO.builder()
                .paymentNo(payment.getPaymentNo())
                .orderNo(payment.getOrderNo())
                .amount(payment.getAmount())
                .payMethod(payment.getPayMethod())
                .status(payment.getStatus())
                .transactionNo(payment.getTransactionNo())
                .paidAt(payment.getPaidAt())
                .build();
    }

    public static RefundResponseDTO toRefundResponse(CounselingRefund refund) {
        if (refund == null) {
            return null;
        }
        return RefundResponseDTO.builder()
                .id(refund.getId())
                .refundNo(refund.getRefundNo())
                .orderId(refund.getOrderId())
                .orderNo(refund.getOrderNo())
                .refundAmount(refund.getRefundAmount())
                .ruleDesc(refund.getRuleDesc())
                .status(refund.getStatus())
                .statusDesc(RefundStatus.descriptionOf(refund.getStatus()))
                .reason(refund.getReason())
                .rejectReason(refund.getRejectReason())
                .appliedAt(refund.getAppliedAt())
                .auditedAt(refund.getAuditedAt())
                .build();
    }

    public static ChatMessageResponseDTO toChatMessageResponse(CounselingChatMessage message) {
        if (message == null) {
            return null;
        }
        return ChatMessageResponseDTO.builder()
                .id(message.getId())
                .appointmentId(message.getAppointmentId())
                .senderId(message.getSenderId())
                .senderType(message.getSenderType())
                .senderTypeDesc(ChatSenderType.descriptionOf(message.getSenderType()))
                .content(message.getContent())
                .crisisFlag(message.getCrisisFlag())
                .createdAt(message.getCreatedAt())
                .build();
    }

    public static CrisisRecordResponseDTO toCrisisRecordResponse(CrisisRecord record) {
        if (record == null) {
            return null;
        }
        return CrisisRecordResponseDTO.builder()
                .id(record.getId())
                .userId(record.getUserId())
                .sourceType(record.getSourceType())
                .sourceTypeDesc(CrisisSourceType.descriptionOf(record.getSourceType()))
                .sourceId(record.getSourceId())
                .messageContent(record.getMessageContent())
                .matchedWords(record.getMatchedWords())
                .riskLevel(record.getRiskLevel())
                .riskLevelDesc(CrisisRiskLevel.descriptionOf(record.getRiskLevel()))
                .aiAnalysis(record.getAiAnalysis())
                .handleStatus(record.getHandleStatus())
                .handleStatusDesc(CrisisHandleStatus.descriptionOf(record.getHandleStatus()))
                .handledBy(record.getHandledBy())
                .handledAt(record.getHandledAt())
                .handleRemark(record.getHandleRemark())
                .createdAt(record.getCreatedAt())
                .build();
    }

    public static NotificationResponseDTO toNotificationResponse(SystemNotification notification) {
        if (notification == null) {
            return null;
        }
        return NotificationResponseDTO.builder()
                .id(notification.getId())
                .title(notification.getTitle())
                .content(notification.getContent())
                .type(notification.getType())
                .typeDesc(NotificationType.descriptionOf(notification.getType()))
                .bizNo(notification.getBizNo())
                .readFlag(notification.getReadFlag())
                .createdAt(notification.getCreatedAt())
                .build();
    }

    private CounselingConvert() {
    }
}
