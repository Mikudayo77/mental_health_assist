package com.example.aispring.service;

import com.example.aispring.DTO.command.UserLoginCommandDTO;
import com.example.aispring.DTO.response.UserLoginResponseDTO;
import com.example.aispring.common.Result;
import org.springframework.stereotype.Service;

@Service
public class UserService {
   public Result<UserLoginResponseDTO> login(UserLoginCommandDTO commandDTO) {
       return null;
   }
}
