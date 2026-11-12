package com.example.aispring.controller;

import com.example.aispring.DTO.response.NotificationResponseDTO;
import com.example.aispring.common.PageResult;
import com.example.aispring.common.Result;
import com.example.aispring.service.SystemNotificationService;
import com.example.aispring.util.JwtTokenUtil;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notification")
public class NotificationController {

    @Resource
    private SystemNotificationService notificationService;

    @GetMapping("/mine")
    public Result<PageResult<NotificationResponseDTO>> mine(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer type,
            @RequestParam(required = false) Integer readFlag) {
        return Result.ok(notificationService.pageMine(
                JwtTokenUtil.getCurrentUserId(), page, size, type, readFlag));
    }

    @GetMapping("/unread-count")
    public Result<Long> unreadCount() {
        return Result.ok(notificationService.unreadCount(JwtTokenUtil.getCurrentUserId()));
    }

    @PutMapping("/{id}/read")
    public Result<Void> markRead(@PathVariable Long id) {
        notificationService.markRead(JwtTokenUtil.getCurrentUserId(), id);
        return Result.ok();
    }
}
