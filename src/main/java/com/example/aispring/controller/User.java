package com.example.aispring.controller;

import com.example.aispring.DTO.command.UserLoginCommandDTO;
import com.example.aispring.common.Result;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
public class User {
    @PostMapping("/login")

    public Result<String> login(@Valid @RequestBody UserLoginCommandDTO commandDTO) {
        System.out.println(commandDTO.getUsername());
        System.out.println(commandDTO.getPassword());
        return null;
    }
}
