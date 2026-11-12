package com.example.aispring.controller;

import com.example.aispring.DTO.command.ChatSendCommandDTO;
import com.example.aispring.DTO.response.ChatMessageResponseDTO;
import com.example.aispring.common.Result;
import com.example.aispring.service.CounselingChatService;
import com.example.aispring.util.JwtTokenUtil;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/counseling-chat")
public class CounselingChatController {

    @Resource
    private CounselingChatService counselingChatService;

    @PostMapping("/send")
    @PreAuthorize("hasAnyRole('1','3')")
    public Result<ChatMessageResponseDTO> send(@Valid @RequestBody ChatSendCommandDTO dto) {
        return Result.ok(counselingChatService.sendMessage(JwtTokenUtil.getCurrentUserId(), dto));
    }

    @GetMapping("/messages")
    @PreAuthorize("hasAnyRole('1','3')")
    public Result<List<ChatMessageResponseDTO>> messages(
            @RequestParam Long appointmentId,
            @RequestParam(defaultValue = "0") Long afterId,
            @RequestParam(defaultValue = "100") int limit) {
        return Result.ok(counselingChatService.pullMessages(
                JwtTokenUtil.getCurrentUserId(), appointmentId, afterId, limit));
    }
}
