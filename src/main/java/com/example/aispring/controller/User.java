package com.example.aispring.controller;

import com.example.aispring.DTO.command.UserLoginCommandDTO;
import com.example.aispring.DTO.response.UserLoginResponseDTO;
import com.example.aispring.common.Result;
import com.example.aispring.service.UserService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
public class User {
    @Resource
    private UserService userService;

    @PostMapping("/login")

    public Result<UserLoginResponseDTO> login(@Valid @RequestBody UserLoginCommandDTO commandDTO) {
        System.out.println(commandDTO.getUsername());
        System.out.println(commandDTO.getPassword());
        Result<UserLoginResponseDTO> result =  userService.login(commandDTO);
        return result;
    }
}
