package com.example.aispring.AiService;

public class StructOutPut {
    public record StreamChatSession(
            String sessionId,
            Long userHash,
            String initialMessage,
            Long startTime,
            Long expiryTime,
            Integer messageCount,
            String status
    ) {}

    /**
     * 流式聊天片段，type: normal(模型正常输出) / crisis(危机求助热线)
     */
    public record ChatFragment(String content, String type) {}
}
