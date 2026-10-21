package com.example.aispring.controller;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.example.aispring.AiService.PsychologicalSupportService;
import com.example.aispring.AiService.StructOutPut;
import com.example.aispring.DTO.command.ConsultationSessionCreateDTO;
import com.example.aispring.common.Result;
import com.example.aispring.util.JwtTokenUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/psychological-chat")
public class PsychologicalChat {
    @Autowired
    private PsychologicalSupportService psychologicalSupportService;
@PostMapping("/session/start")
    public Result<StructOutPut.StreamChatSession> startSession(@Valid @RequestBody ConsultationSessionCreateDTO createDTO){
    // 获取当前用户
    String token = JwtTokenUtil.getCurrentToken();
    DecodedJWT jwt = JwtTokenUtil.verifyToken(token);
    Long userId = jwt.getClaim("userId").asLong();
    StructOutPut.StreamChatSession session = psychologicalSupportService.startSession(userId, createDTO);
    return null;
}
}
