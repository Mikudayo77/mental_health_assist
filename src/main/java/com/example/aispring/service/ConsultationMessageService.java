package com.example.aispring.service;

import com.example.aispring.entity.ConsultationMessage;
import com.example.aispring.entity.ConsultationSession;
import com.example.aispring.mapper.ConsultationMessageMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class ConsultationMessageService {
    @Autowired
    private ConsultationMessageMapper consultationMessageMapper;
    public ConsultationMessage saveUserMessage(Long sessionId, String content, String emotion_tag) {
        // 构建用户消息实体
        ConsultationMessage userMessage = ConsultationMessage.builder()
                .sessionId(sessionId)
                .senderType(1)
                .messageType(1)
                .content(content)
                .emotionTag(emotion_tag)
                .createdAt(LocalDateTime.now())
                .build();

        consultationMessageMapper.insert(userMessage);
        return userMessage;
    }
}
