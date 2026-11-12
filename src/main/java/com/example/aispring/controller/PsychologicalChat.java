package com.example.aispring.controller;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.example.aispring.AiService.PsychologicalSupportService;
import com.example.aispring.AiService.StructOutPut;
import com.example.aispring.DTO.command.ConsultationSessionCreateDTO;
import com.example.aispring.DTO.command.ConsultationStreamDTO;
import com.example.aispring.DTO.response.ConsultationMessageResponseDTO;
import com.example.aispring.DTO.response.SessionEmotionResponseDTO;
import com.example.aispring.DTO.response.SessionListResponseDTO;
import com.example.aispring.common.PageResult;
import com.example.aispring.common.Result;
import com.example.aispring.common.ResultCode;
import com.example.aispring.entity.ConsultationSession;
import com.example.aispring.enumClass.UserType;
import com.example.aispring.service.ConsultationMessageService;
import com.example.aispring.service.ConsultationSessionService;
import com.example.aispring.util.JwtTokenUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/psychological-chat")
public class PsychologicalChat {
    @Autowired
    private PsychologicalSupportService psychologicalSupportService;
    @Autowired
    private ConsultationSessionService consultationSessionService;
    @Autowired
    private ConsultationMessageService consultationMessageService;
@PostMapping("/session/start")
    public Result<StructOutPut.StreamChatSession> startSession(@Valid @RequestBody ConsultationSessionCreateDTO createDTO){
    // 获取当前用户
    String token = JwtTokenUtil.getCurrentToken();
    DecodedJWT jwt = JwtTokenUtil.verifyToken(token);
    Long userId = jwt.getClaim("userId").asLong();
    StructOutPut.StreamChatSession session = psychologicalSupportService.startSession(userId, createDTO);
    return Result.ok(session);
}
@PostMapping(value = "/stream",produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public Flux<ServerSentEvent<String>> streamChat(@Valid @RequestBody ConsultationStreamDTO streamDTO) {
    // 获取当前用户
    String token = JwtTokenUtil.getCurrentToken();
    DecodedJWT jwt = JwtTokenUtil.verifyToken(token);
    Long userId = jwt.getClaim("userId").asLong();

    if (userId == null) {
        return Flux.just(ServerSentEvent.<String>builder()
                .event("error")
                .data(JSONUtil.toJsonStr(Result.error(ResultCode.UNAUTHORIZED.getCode(), ResultCode.UNAUTHORIZED.getMsg(), "用户未登录")))
                .build());
    }
    // 开始流式对话
    return psychologicalSupportService.streamPsychologicalChat(userId, streamDTO.getSessionId(), streamDTO.getUserMessage())
            .map(fragment -> ServerSentEvent.<String>builder()
                    .event("message")
                    .data(JSONUtil.toJsonStr(Result.ok(Map.of("content", fragment.content(), "type", fragment.type()))))
                    .build())
            .concatWith(Flux.just(ServerSentEvent.<String>builder()
                    .event("done")
                    .data("{}")
                    .build()
            ))
            .delayElements(Duration.ofMillis(50)); // 添加延迟确保流式数据的体验
}

    /**
     * 会话列表。前台传 pageNum/pageSize，后台「咨询记录」传 currentPage/size，两种都接。
     * 管理员看到全部用户的会话，普通用户只看到自己的。
     */
    @GetMapping("/sessions")
    public Result<PageResult<SessionListResponseDTO>> listSessions(
            @RequestParam(value = "pageNum", required = false) Integer pageNum,
            @RequestParam(value = "pageSize", required = false) Integer pageSize,
            @RequestParam(value = "currentPage", required = false) Integer currentPage,
            @RequestParam(value = "size", required = false) Integer size) {
        Long userId = JwtTokenUtil.getCurrentUserId();
        Integer page = pageNum != null ? pageNum : currentPage;
        Integer pageSizeFinal = pageSize != null ? pageSize : size;
        return Result.ok(consultationSessionService.pageSessions(userId, isAdmin(), page, pageSizeFinal));
    }

    /**
     * 某个会话的历史消息（时间正序）
     */
    @GetMapping("/sessions/{sessionId}/messages")
    public Result<List<ConsultationMessageResponseDTO>> listMessages(@PathVariable("sessionId") String sessionId) {
        Long userId = JwtTokenUtil.getCurrentUserId();
        Long dbSessionId = parseSessionId(sessionId);
        if (dbSessionId == null) {
            return Result.error(ResultCode.PARAM_ERROR.getCode(), "会话ID格式错误", null);
        }
        if (!consultationSessionService.canAccess(dbSessionId, userId, isAdmin())) {
            return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "会话不存在或无权访问", null);
        }
        return Result.ok(consultationMessageService.listMessagesBySessionId(dbSessionId));
    }

    /**
     * 删除会话（连同该会话下的消息）
     */
    @DeleteMapping("/sessions/{sessionId}")
    public Result<Void> deleteSession(@PathVariable("sessionId") String sessionId) {
        Long userId = JwtTokenUtil.getCurrentUserId();
        Long dbSessionId = parseSessionId(sessionId);
        if (dbSessionId == null) {
            return Result.error(ResultCode.PARAM_ERROR.getCode(), "会话ID格式错误", null);
        }
        if (!consultationSessionService.deleteSession(dbSessionId, userId)) {
            return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "会话不存在或无权访问", null);
        }
        return Result.ok();
    }

    /**
     * 会话情绪面板：读取 consultation_session.last_emotion_analysis 里保存的分析结果，
     * 没有数据时返回中性兜底，保证前端"情绪花园"能正常渲染。
     * 路径参数兼容 "session_31" 和 "31" 两种写法。
     */
    @GetMapping("/session/{sessionId}/emotion")
    public Result<SessionEmotionResponseDTO> sessionEmotion(@PathVariable("sessionId") String sessionId) {
        Long userId = JwtTokenUtil.getCurrentUserId();
        Long dbSessionId = parseSessionId(sessionId);
        if (dbSessionId == null) {
            return Result.error(ResultCode.PARAM_ERROR.getCode(), "会话ID格式错误", null);
        }
        if (!consultationSessionService.canAccess(dbSessionId, userId, isAdmin())) {
            return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "会话不存在或无权访问", null);
        }
        return Result.ok(readEmotion(consultationSessionService.getSession(dbSessionId)));
    }

    private SessionEmotionResponseDTO readEmotion(ConsultationSession session) {
        if (session == null || StrUtil.isBlank(session.getLastEmotionAnalysis())) {
            return SessionEmotionResponseDTO.neutral();
        }
        try {
            JSONObject json = JSONUtil.parseObj(session.getLastEmotionAnalysis());
            JSONArray actions = json.getJSONArray("improvementSuggestions");
            return SessionEmotionResponseDTO.builder()
                    .primaryEmotion(json.getStr("primaryEmotion", "中性"))
                    .emotionScore(json.getInt("emotionScore", 50))
                    .isNegative(json.getBool("isNegative", false))
                    .riskLevel(json.getInt("riskLevel", 0))
                    .suggestion(json.getStr("suggestion", ""))
                    .improvementSuggestions(actions == null ? new ArrayList<>() : actions.toList(String.class))
                    .riskDescription(json.getStr("riskDescription", ""))
                    .build();
        } catch (Exception e) {
            // 历史数据格式异常时不拖垮整个面板
            return SessionEmotionResponseDTO.neutral();
        }
    }

    /**
     * 兼容 "session_31" 与 "31" 两种会话ID写法
     */
    private Long parseSessionId(String sessionId) {
        if (StrUtil.isBlank(sessionId)) {
            return null;
        }
        String raw = sessionId.startsWith("session_") ? sessionId.substring("session_".length()) : sessionId;
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 当前登录用户是否为管理员（JWT 过滤器按 ROLE_ + userType 授权，2 为管理员）
     */
    private boolean isAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        String adminAuthority = "ROLE_" + UserType.ADMIN.getCode();
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> adminAuthority.equals(authority.getAuthority()));
    }
}
