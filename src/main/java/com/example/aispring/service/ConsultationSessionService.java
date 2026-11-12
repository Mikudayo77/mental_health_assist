package com.example.aispring.service;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.aispring.DTO.command.ConsultationSessionCreateDTO;
import com.example.aispring.DTO.response.ConsultationMessageResponseDTO;
import com.example.aispring.DTO.response.SessionListResponseDTO;
import com.example.aispring.common.PageResult;
import com.example.aispring.entity.ConsultationSession;
import com.example.aispring.entity.User;
import com.example.aispring.mapper.ConsultationSessionMapper;
import com.example.aispring.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class ConsultationSessionService {
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private ConsultationSessionMapper consultationSessionMapper;
    @Autowired
    private ConsultationMessageService consultationMessageService;

    public ConsultationSession createSession(Long userId, ConsultationSessionCreateDTO createDTO){
      User user= userMapper.selectById(userId);
        if (user != null) {
            // 创建会话记录
            ConsultationSession session = ConsultationSession.builder()
                    .userId(userId)
                    .sessionTitle(createDTO.getSessionTitle())
                    .startedAt(LocalDateTime.now())
                    .build();
            // 如果未提供标题
            if (StrUtil.isBlank(createDTO.getSessionTitle())) {
                session.setSessionTitle(String.format("心理健康管家 - " + DateUtil.format(LocalDateTime.now(), "MM-dd HH:mm")));
            }

            // 插入记录
            consultationSessionMapper.insert(session);
            return session;
        }
return null;

    }

    /**
     * 分页查询会话列表。
     * admin=true 时查全部用户的会话（后台「咨询记录」），否则只查自己的。
     * 并补齐前端卡片需要的昵称、消息数、时长、最后一条消息及时间。
     */
    public PageResult<SessionListResponseDTO> pageSessions(Long userId, boolean admin, Integer pageNum, Integer pageSize) {
        int num = (pageNum == null || pageNum < 1) ? 1 : pageNum;
        int size = (pageSize == null || pageSize < 1) ? 10 : Math.min(pageSize, 50);

        LambdaQueryWrapper<ConsultationSession> queryWrapper = new LambdaQueryWrapper<>();
        if (!admin) {
            queryWrapper.eq(ConsultationSession::getUserId, userId);
        }
        queryWrapper.orderByDesc(ConsultationSession::getStartedAt)
                .orderByDesc(ConsultationSession::getId);

        Page<ConsultationSession> page = consultationSessionMapper.selectPage(new Page<>(num, size), queryWrapper);

        Map<Long, User> userMap = userMap(page.getRecords());
        List<SessionListResponseDTO> records = page.getRecords().stream()
                .map(session -> toListDTO(session, userMap.get(session.getUserId())))
                .toList();

        return PageResult.of(page.getTotal(), records);
    }

    private SessionListResponseDTO toListDTO(ConsultationSession session, User user) {
        Long sessionId = session.getId();
        Integer messageCount = consultationMessageService.getMessageCountBySessionId(sessionId);
        ConsultationMessageResponseDTO lastMessage = consultationMessageService.getLastMessageBySessionId(sessionId);

        long durationMinutes = 0L;
        if (session.getStartedAt() != null && lastMessage != null && lastMessage.getCreatedAt() != null) {
            long minutes = Duration.between(session.getStartedAt(), lastMessage.getCreatedAt()).toMinutes();
            durationMinutes = Math.max(minutes, 0L);
        }

        return SessionListResponseDTO.builder()
                .id(sessionId)
                .sessionTitle(session.getSessionTitle())
                .userNickname(user == null ? "" : user.getDisplayName())
                .startedAt(session.getStartedAt() == null ? "" : DateUtil.format(session.getStartedAt(), "yyyy-MM-dd HH:mm"))
                .lastMessageTime(lastMessage == null || lastMessage.getCreatedAt() == null
                        ? "" : DateUtil.format(lastMessage.getCreatedAt(), "yyyy-MM-dd HH:mm"))
                .messageCount(messageCount == null ? 0 : messageCount)
                .durationMinutes(durationMinutes)
                .lastMessageContent(lastMessage == null || lastMessage.getContent() == null ? "" : lastMessage.getContent())
                .status("ACTIVE")
                .build();
    }

    /**
     * 批量取会话归属用户，用于后台列表显示昵称
     */
    private Map<Long, User> userMap(List<ConsultationSession> sessions) {
        Map<Long, User> map = new HashMap<>();
        List<Long> userIds = sessions.stream()
                .map(ConsultationSession::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (userIds.isEmpty()) {
            return map;
        }
        for (User user : userMapper.selectBatchIds(userIds)) {
            map.put(user.getId(), user);
        }
        return map;
    }

    /**
     * 查询会话，不存在返回 null
     */
    public ConsultationSession getSession(Long sessionId) {
        if (sessionId == null) {
            return null;
        }
        return consultationSessionMapper.selectById(sessionId);
    }

    /**
     * 判断会话是否属于该用户，防止越权读取别人（或咨询师、管理员）的会话
     */
    public boolean belongsTo(Long sessionId, Long userId) {
        ConsultationSession session = getSession(sessionId);
        return session != null && Objects.equals(session.getUserId(), userId);
    }

    /**
     * 是否可以查看该会话：管理员可查看全部，普通用户只能看自己的
     */
    public boolean canAccess(Long sessionId, Long userId, boolean admin) {
        ConsultationSession session = getSession(sessionId);
        if (session == null) {
            return false;
        }
        return admin || Objects.equals(session.getUserId(), userId);
    }

    /**
     * 删除会话及其全部消息，返回是否删除成功
     */
    public boolean deleteSession(Long sessionId, Long userId) {
        if (!belongsTo(sessionId, userId)) {
            return false;
        }
        consultationMessageService.deleteBySessionId(sessionId);
        return consultationSessionMapper.deleteById(sessionId) > 0;
    }
}
