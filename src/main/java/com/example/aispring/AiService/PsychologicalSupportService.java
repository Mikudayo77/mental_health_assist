package com.example.aispring.AiService;

import com.example.aispring.DTO.command.ConsultationSessionCreateDTO;
import com.example.aispring.DTO.response.ConsultationMessageResponseDTO;
import com.example.aispring.common.constant.CrisisConstant;
import com.example.aispring.entity.ConsultationSession;
import com.example.aispring.enumClass.CrisisRiskLevel;
import com.example.aispring.enumClass.CrisisSourceType;
import com.example.aispring.service.ConsultationMessageService;
import com.example.aispring.service.ConsultationSessionService;
import com.example.aispring.service.CrisisInterventionService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;

@Service
public class PsychologicalSupportService {
    @Autowired
    @Qualifier("open-ai")
    private ChatClient chatClient;

    @Autowired
    private ChatMemory chatMemory;
    @Autowired
    private ConsultationSessionService consultationSessionService;

    @Autowired
    private ConsultationMessageService consultationMessageService;

    @Autowired
    private CrisisInterventionService crisisInterventionService;

    @Autowired
    private com.example.aispring.service.EmotionAnalysisService emotionAnalysisService;

    public StructOutPut.StreamChatSession startSession(Long userId, ConsultationSessionCreateDTO createDTO) {
        // 创建数据库会话记录
        ConsultationSession consultationSession = consultationSessionService.createSession(userId, createDTO);

        // 将初始用户消息保存到Message表
        consultationMessageService.saveUserMessage(consultationSession.getId(), createDTO.getInitialMessage(), null);

        // 创建会话信息
        String sessionId = "session_" + consultationSession.getId();
        return new StructOutPut.StreamChatSession(
                sessionId,
                userId,
                createDTO.getInitialMessage(),
                System.currentTimeMillis(),
                System.currentTimeMillis() + 86400000L, // 24小时
                1,
                "ACTIVE"
        );
}

    public Flux<StructOutPut.ChatFragment> streamPsychologicalChat(Long userId, String sessionId, String userMessage) {
        // 创建响应流
        return Flux.create(sink -> {
            // sink.next("数据1") // 发布数据
            // sink.complete(); // 完成流
            // sink.error(exception); // 发布错误
            Long dbSessionId = extractSessionId(sessionId);
            if (dbSessionId == null) {
                sink.error(new RuntimeException("会话ID格式错误"));
                return;
            }

            // 危机干预：敏感词命中后才调用AI评级，高风险优先推送求助热线
            List<String> crisisHits = crisisInterventionService.matchSensitiveWords(userMessage);
            if (!crisisHits.isEmpty()) {
                CrisisInterventionService.RiskAssessment assessment =
                        crisisInterventionService.assessRisk(userMessage);
                crisisInterventionService.recordAndNotify(userId, CrisisSourceType.AI_CHAT.getCode(),
                        dbSessionId, userMessage, crisisHits, assessment, null);
                if (CrisisRiskLevel.HIGH.equals(assessment.level())) {
                    sink.next(new StructOutPut.ChatFragment(
                            CrisisConstant.CRISIS_HOTLINE_TEXT, CrisisConstant.CRISIS_FRAGMENT_TYPE));
                }
            }
            // 是否为初始消息
            boolean isInitialMessage = false;
            // 检查是否为初始消息，避免重复保存
            Integer messageCount = consultationMessageService.getMessageCountBySessionId(dbSessionId);
            if (messageCount == 1) {
                ConsultationMessageResponseDTO lastMessage = consultationMessageService.getLastMessageBySessionId(dbSessionId);
                if (lastMessage != null && lastMessage.getSenderType() == 1 && userMessage.equals(lastMessage.getContent())) {
                    isInitialMessage = true;
                }
            }
            if (!isInitialMessage) {
                // 保存用户消息到数据库
                consultationMessageService.saveUserMessage(dbSessionId, userMessage, null);
            }

            // 进行流式对话
            // 生成对话记忆管理
            String conversationId = "conversation_" + sessionId;
            // 构建系统提示词
            List<Message> userMessages = new ArrayList<>();
            userMessages.add(new UserMessage(userMessage));
            chatMemory.add(conversationId, userMessages);
            Prompt prompt = new Prompt(List.of(
                    new SystemMessage(PromptManage.PSYCHOLOGICAL_SUPPORT_SYSTEM_PROMPT)
            ));

            //用于存储AI完成的响应
            StringBuilder fullResponse = new StringBuilder();

            // 使用chatClient发送消息到Open AI
            chatClient.prompt(prompt)
                    .user(userMessage)
                    .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, conversationId))
                    .stream()
                    .content()
                    .subscribe(
                            fragment -> {
                                fullResponse.append(fragment);
                                sink.next(new StructOutPut.ChatFragment(
                                        fragment, CrisisConstant.NORMAL_FRAGMENT_TYPE));
                            },
                            sink::error,
                            () -> {
                                String completeRes = fullResponse.toString();
                                // 将AI返回的内容保存到数据库
                                consultationMessageService.saveAimessage(dbSessionId, completeRes, "openai");
                                // 添加AI回复到chatMemory
                                List<Message> aiMessages = new ArrayList<>();
                                aiMessages.add(new AssistantMessage(completeRes));
                                chatMemory.add(conversationId, aiMessages);

                                // 异步做情绪分析并写回会话，供前端"情绪花园"展示（不阻塞流式返回）
                                emotionAnalysisService.analyzeSessionAsync(dbSessionId,
                                        "用户说：" + userMessage + "\nAI回复：" + completeRes);

                                sink.complete();
                            }
                    ); // 订阅并启动流
        });
    }

    // 获取参数中的sessionId
    public Long extractSessionId(String sessionId) {
        if (sessionId != null && sessionId.startsWith("session_")) {
            String idStr = sessionId.substring("session_".length());
            return Long.parseLong(idStr);
        }
        return null;
    }
}