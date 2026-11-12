package com.example.aispring.service;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.aispring.AiService.PromptManage;
import com.example.aispring.DTO.command.CrisisHandleCommandDTO;
import com.example.aispring.DTO.response.CrisisRecordResponseDTO;
import com.example.aispring.common.PageResult;
import com.example.aispring.common.constant.SensitiveWordBank;
import com.example.aispring.entity.CrisisRecord;
import com.example.aispring.enumClass.CrisisHandleStatus;
import com.example.aispring.enumClass.CrisisRiskLevel;
import com.example.aispring.enumClass.CrisisSourceType;
import com.example.aispring.enumClass.NotificationType;
import com.example.aispring.exception.BusinessException;
import com.example.aispring.mapper.CrisisRecordMapper;
import com.example.aispring.service.convert.CounselingConvert;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class CrisisInterventionService {

    /**
     * 独立于业务 ChatClient（不带 ChatMemory 顾问），避免评级请求写入会话记忆
     */
    @Resource
    private OpenAiChatModel openAiChatModel;

    @Resource
    private CrisisRecordMapper crisisRecordMapper;

    @Resource
    private SystemNotificationService notificationService;

    public record RiskAssessment(CrisisRiskLevel level, String reason) {
    }

    /**
     * 第一道筛查：本地词库匹配，命中才进入 AI 评级
     */
    public List<String> matchSensitiveWords(String content) {
        List<String> hits = new ArrayList<>();
        if (content == null || content.isBlank()) {
            return hits;
        }
        for (String word : SensitiveWordBank.SENSITIVE_WORDS) {
            if (content.contains(word)) {
                hits.add(word);
            }
        }
        return hits;
    }

    /**
     * 第二道筛查：调用大模型评级；不携带 conversationId，避免污染聊天记忆
     */
    public RiskAssessment assessRisk(String content) {
        try {
            String response = ChatClient.builder(openAiChatModel).build()
                    .prompt()
                    .user(String.format(PromptManage.CRISIS_RISK_ASSESS_PROMPT, content))
                    .call()
                    .content();
            JSONObject obj = JSONUtil.parseObj(extractJson(response));
            CrisisRiskLevel level = CrisisRiskLevel.fromCode(obj.getStr("level"));
            String reason = obj.getStr("reason");
            return new RiskAssessment(level, reason == null ? "" : reason);
        } catch (Exception e) {
            log.warn("危机风险评级失败，降级为 MEDIUM: {}", e.getMessage());
            return new RiskAssessment(CrisisRiskLevel.MEDIUM, "AI评级失败，系统保守降级为中风险");
        }
    }

    private String extractJson(String response) {
        if (response == null) {
            throw new IllegalArgumentException("模型返回为空");
        }
        int start = response.indexOf('{');
        int end = response.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new IllegalArgumentException("模型返回中未找到JSON");
        }
        return response.substring(start, end + 1);
    }

    /**
     * 落危机记录；HIGH 风险时通知管理员（人工咨询场景额外通知咨询师）
     */
    @Transactional
    public void recordAndNotify(Long userId, Integer sourceType, Long sourceId, String content,
                                List<String> hits, RiskAssessment assessment, Long extraUserId) {
        crisisRecordMapper.insert(CrisisRecord.builder()
                .userId(userId)
                .sourceType(sourceType)
                .sourceId(sourceId)
                .messageContent(content)
                .matchedWords(String.join(",", hits))
                .riskLevel(assessment.level().getCode())
                .aiAnalysis(assessment.reason())
                .handleStatus(CrisisHandleStatus.PENDING.getCode())
                .createdAt(LocalDateTime.now())
                .build());

        if (!CrisisRiskLevel.HIGH.equals(assessment.level())) {
            return;
        }

        String notice = "检测到用户 " + userId + " 的高危危机信号（来源："
                + CrisisSourceType.descriptionOf(sourceType) + "，来源ID：" + sourceId
                + "），命中敏感词：" + String.join(",", hits) + "。请尽快介入处理。";
        String bizNo = sourceId == null ? null : String.valueOf(sourceId);
        notificationService.notifyAdmins(NotificationType.CRISIS.getCode(), "高危危机预警", notice, bizNo);
        if (extraUserId != null) {
            notificationService.create(NotificationType.CRISIS.getCode(), extraUserId,
                    "咨询中危机预警", notice, bizNo);
        }
    }

    public PageResult<CrisisRecordResponseDTO> pageList(int page, int size, String riskLevel, Integer handleStatus) {
        Page<CrisisRecord> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<CrisisRecord> wrapper = new LambdaQueryWrapper<CrisisRecord>()
                .eq(riskLevel != null && !riskLevel.isBlank(), CrisisRecord::getRiskLevel, riskLevel)
                .eq(handleStatus != null, CrisisRecord::getHandleStatus, handleStatus)
                .orderByDesc(CrisisRecord::getCreatedAt);
        Page<CrisisRecord> result = crisisRecordMapper.selectPage(pageParam, wrapper);
        List<CrisisRecordResponseDTO> records = result.getRecords().stream()
                .map(CounselingConvert::toCrisisRecordResponse)
                .toList();
        return PageResult.of(result.getTotal(), records);
    }

    public CrisisRecordResponseDTO getDetail(Long id) {
        CrisisRecord record = crisisRecordMapper.selectById(id);
        if (record == null) {
            throw new BusinessException("危机记录不存在");
        }
        return CounselingConvert.toCrisisRecordResponse(record);
    }

    public void handle(Long adminId, CrisisHandleCommandDTO dto) {
        CrisisRecord record = crisisRecordMapper.selectById(dto.getCrisisId());
        if (record == null) {
            throw new BusinessException("危机记录不存在");
        }
        crisisRecordMapper.update(null, new LambdaUpdateWrapper<CrisisRecord>()
                .eq(CrisisRecord::getId, dto.getCrisisId())
                .set(CrisisRecord::getHandleStatus, dto.getHandleStatus())
                .set(CrisisRecord::getHandleRemark, dto.getHandleRemark())
                .set(CrisisRecord::getHandledBy, adminId)
                .set(CrisisRecord::getHandledAt, LocalDateTime.now()));
    }
}
