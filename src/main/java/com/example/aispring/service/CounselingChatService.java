package com.example.aispring.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.aispring.DTO.command.ChatSendCommandDTO;
import com.example.aispring.DTO.response.ChatMessageResponseDTO;
import com.example.aispring.entity.CounselingAppointment;
import com.example.aispring.entity.CounselingChatMessage;
import com.example.aispring.entity.Counselor;
import com.example.aispring.enumClass.AppointmentStatus;
import com.example.aispring.enumClass.ChatSenderType;
import com.example.aispring.enumClass.CrisisSourceType;
import com.example.aispring.exception.BusinessException;
import com.example.aispring.mapper.CounselingAppointmentMapper;
import com.example.aispring.mapper.CounselingChatMessageMapper;
import com.example.aispring.service.convert.CounselingConvert;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CounselingChatService {

    @Resource
    private CounselingChatMessageMapper chatMessageMapper;

    @Resource
    private CounselingAppointmentMapper appointmentMapper;

    @Resource
    private CounselorService counselorService;

    @Resource
    private CrisisInterventionService crisisInterventionService;

    private record Identity(Integer senderType, Long counselorUserId) {
    }

    /**
     * 发送消息：身份由服务端判定，用户消息命中敏感词时触发危机干预
     */
    @Transactional
    public ChatMessageResponseDTO sendMessage(Long userId, ChatSendCommandDTO dto) {
        CounselingAppointment appointment = getExistingAppointment(dto.getAppointmentId());
        Identity identity = resolveIdentity(appointment, userId);
        checkChatWindow(appointment);

        int crisisFlag = 0;
        if (ChatSenderType.USER.getCode().equals(identity.senderType())) {
            List<String> hits = crisisInterventionService.matchSensitiveWords(dto.getContent());
            if (!hits.isEmpty()) {
                CrisisInterventionService.RiskAssessment assessment =
                        crisisInterventionService.assessRisk(dto.getContent());
                crisisInterventionService.recordAndNotify(userId,
                        CrisisSourceType.HUMAN_CHAT.getCode(), appointment.getId(),
                        dto.getContent(), hits, assessment, identity.counselorUserId());
                crisisFlag = 1;
            }
        }

        CounselingChatMessage message = CounselingChatMessage.builder()
                .appointmentId(appointment.getId())
                .senderId(userId)
                .senderType(identity.senderType())
                .content(dto.getContent())
                .crisisFlag(crisisFlag)
                .createdAt(LocalDateTime.now())
                .build();
        chatMessageMapper.insert(message);
        return CounselingConvert.toChatMessageResponse(message);
    }

    /**
     * 增量拉取消息：afterId 为上次拿到的最大消息 id，首次传 0
     */
    public List<ChatMessageResponseDTO> pullMessages(Long userId, Long appointmentId, Long afterId, int limit) {
        CounselingAppointment appointment = getExistingAppointment(appointmentId);
        resolveIdentity(appointment, userId);
        long cursor = afterId == null ? 0L : afterId;
        int safeLimit = Math.max(1, Math.min(limit, 200));
        return chatMessageMapper.selectList(new LambdaQueryWrapper<CounselingChatMessage>()
                        .eq(CounselingChatMessage::getAppointmentId, appointmentId)
                        .gt(CounselingChatMessage::getId, cursor)
                        .orderByAsc(CounselingChatMessage::getId)
                        .last("limit " + safeLimit))
                .stream()
                .map(CounselingConvert::toChatMessageResponse)
                .toList();
    }

    private CounselingAppointment getExistingAppointment(Long appointmentId) {
        CounselingAppointment appointment = appointmentMapper.selectById(appointmentId);
        if (appointment == null) {
            throw new BusinessException("预约不存在");
        }
        return appointment;
    }

    private Identity resolveIdentity(CounselingAppointment appointment, Long userId) {
        if (appointment.getUserId().equals(userId)) {
            return new Identity(ChatSenderType.USER.getCode(), null);
        }
        Counselor counselor = counselorService.getApprovedByUserId(userId);
        if (counselor != null && counselor.getId().equals(appointment.getCounselorId())) {
            return new Identity(ChatSenderType.COUNSELOR.getCode(), counselor.getUserId());
        }
        throw new BusinessException("无权访问该预约");
    }

    /**
     * 聊天窗口：已确认预约，且处于 [开始前15分钟, 结束后30分钟]
     */
    private void checkChatWindow(CounselingAppointment appointment) {
        if (!AppointmentStatus.CONFIRMED.getCode().equals(appointment.getStatus())) {
            throw new BusinessException("预约未确认或已结束，暂不能咨询");
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(appointment.getAppointmentStart().minusMinutes(15))) {
            throw new BusinessException("咨询尚未开始，可提前15分钟进入");
        }
        if (now.isAfter(appointment.getAppointmentEnd().plusMinutes(30))) {
            throw new BusinessException("咨询已结束，聊天窗口已关闭");
        }
    }
}
