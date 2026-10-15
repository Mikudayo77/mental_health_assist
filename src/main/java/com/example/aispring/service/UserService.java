package com.example.aispring.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.aispring.DTO.command.UserLoginCommandDTO;
import com.example.aispring.DTO.response.UserLoginResponseDTO;
import com.example.aispring.common.Result;
import com.example.aispring.entity.User;
import com.example.aispring.mapper.UserMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    @Resource
    private UserMapper userMapper;
   public Result<UserLoginResponseDTO> login(UserLoginCommandDTO commandDTO) {
       //查询条件
       LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
         queryWrapper.eq(User::getUsername, commandDTO.getUsername())
                .or()
                .eq(User::getEmail, commandDTO.getUsername());
         //调用MP API查询
        User user = userMapper.selectOne(queryWrapper);
        System.out.println(user);
       return null;
   }
}
