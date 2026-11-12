package com.example.aispring.controller;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.example.aispring.DTO.command.UserLoginCommandDTO;
import com.example.aispring.DTO.command.UserRegisterCommandDTO;
import com.example.aispring.DTO.response.UserLoginResponseDTO;
import com.example.aispring.common.Result;
import com.example.aispring.service.UserService;
import com.example.aispring.util.JwtTokenUtil;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
public class User {
    @Resource
    private UserService userService;

    @PostMapping("/login")

    public Result<UserLoginResponseDTO> login(@Valid @RequestBody UserLoginCommandDTO commandDTO) {
        System.out.println(commandDTO.getUsername());
        System.out.println(commandDTO.getPassword());
        UserLoginResponseDTO result = userService.login(commandDTO);
        return Result.ok(result);
    }

    @PostMapping("/add")
    public Result<UserLoginResponseDTO.UserDetailResponseDTO> register(@Valid @RequestBody UserRegisterCommandDTO commandDTO) {
        UserLoginResponseDTO.UserDetailResponseDTO result = userService.register(commandDTO);
        return Result.ok(result);
    }

    @PostMapping("/logout")
    public Result<Void> logout() {
        // JWT 无状态，服务端不保存会话，前端负责清掉本地 token 与用户信息
        return Result.ok();
    }

    @GetMapping("/current")
    public Result<UserLoginResponseDTO.UserDetailResponseDTO> getCurrentUser() {
//         如何从token中解析出用户的id
        String token = JwtTokenUtil.getCurrentToken();
        DecodedJWT jwt = JwtTokenUtil.verifyToken(token);
        Long userId = jwt.getClaim("userId").asLong();
        // 调用service层获取用户详情
        UserLoginResponseDTO.UserDetailResponseDTO result = userService.getUserById(userId);
        return Result.ok(result);

    }
}
