package com.example.aispring.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.aispring.DTO.response.NotificationResponseDTO;
import com.example.aispring.common.PageResult;
import com.example.aispring.entity.SystemNotification;
import com.example.aispring.entity.User;
import com.example.aispring.enumClass.UserType;
import com.example.aispring.exception.BusinessException;
import com.example.aispring.mapper.SystemNotificationMapper;
import com.example.aispring.mapper.UserMapper;
import com.example.aispring.service.convert.CounselingConvert;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SystemNotificationService {

    @Resource
    private SystemNotificationMapper notificationMapper;

    @Resource
    private UserMapper userMapper;

    public void create(Integer type, Long userId, String title, String content, String bizNo) {
        notificationMapper.insert(SystemNotification.builder()
                .userId(userId)
                .type(type)
                .title(title)
                .content(content)
                .bizNo(bizNo)
                .readFlag(0)
                .createdAt(LocalDateTime.now())
                .build());
    }

    /**
     * 通知全部管理员
     */
    public void notifyAdmins(Integer type, String title, String content, String bizNo) {
        List<User> admins = userMapper.selectList(new LambdaQueryWrapper<User>()
                .eq(User::getUserType, UserType.ADMIN.getCode()));
        LocalDateTime now = LocalDateTime.now();
        for (User admin : admins) {
            notificationMapper.insert(SystemNotification.builder()
                    .userId(admin.getId())
                    .type(type)
                    .title(title)
                    .content(content)
                    .bizNo(bizNo)
                    .readFlag(0)
                    .createdAt(now)
                    .build());
        }
    }

    public PageResult<NotificationResponseDTO> pageMine(Long userId, int page, int size, Integer type, Integer readFlag) {
        Page<SystemNotification> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<SystemNotification> wrapper = new LambdaQueryWrapper<SystemNotification>()
                .eq(SystemNotification::getUserId, userId)
                .eq(type != null, SystemNotification::getType, type)
                .eq(readFlag != null, SystemNotification::getReadFlag, readFlag)
                .orderByDesc(SystemNotification::getCreatedAt);
        Page<SystemNotification> result = notificationMapper.selectPage(pageParam, wrapper);
        List<NotificationResponseDTO> records = result.getRecords().stream()
                .map(CounselingConvert::toNotificationResponse)
                .toList();
        return PageResult.of(result.getTotal(), records);
    }

    public Long unreadCount(Long userId) {
        return notificationMapper.selectCount(new LambdaQueryWrapper<SystemNotification>()
                .eq(SystemNotification::getUserId, userId)
                .eq(SystemNotification::getReadFlag, 0));
    }

    public void markRead(Long userId, Long id) {
        SystemNotification notification = notificationMapper.selectById(id);
        if (notification == null || !notification.getUserId().equals(userId)) {
            throw new BusinessException("通知不存在");
        }
        if (Integer.valueOf(1).equals(notification.getReadFlag())) {
            return;
        }
        notificationMapper.update(null, new LambdaUpdateWrapper<SystemNotification>()
                .eq(SystemNotification::getId, id)
                .set(SystemNotification::getReadFlag, 1));
    }
}
