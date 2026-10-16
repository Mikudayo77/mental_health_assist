package com.example.aispring.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.aispring.DTO.command.UserLoginCommandDTO;
import com.example.aispring.DTO.response.UserLoginResponseDTO;
import com.example.aispring.common.Result;
import com.example.aispring.entity.User;
import com.example.aispring.enumClass.UserStatus;
import com.example.aispring.exception.BusinessException;
import com.example.aispring.mapper.UserMapper;
import com.example.aispring.service.convert.UserConvert;
import com.example.aispring.util.JwtTokenUtil;
import jakarta.annotation.Resource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    @Resource
    private UserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
   public UserLoginResponseDTO login(UserLoginCommandDTO commandDTO) {
       //查询条件
       LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
         queryWrapper.eq(User::getUsername, commandDTO.getUsername())
                .or()
                .eq(User::getEmail, commandDTO.getUsername());
         //调用MP API查询
        User user = userMapper.selectOne(queryWrapper);
        // 判断用户是否存在
        if(user == null){
            throw new BusinessException("用户不存在");
        }
        // 判断密码是否正确
        String inputPassword = commandDTO.getPassword().trim();
        if(!passwordEncoder.matches(inputPassword, user.getPassword())){
            throw new BusinessException("密码错误");
        }
        //检查用户状态
       if(!UserStatus.NORMAL.getCode().equals(user.getStatus())){
           throw new BusinessException("用户已禁用");
       }
       // 生成JWT token
       String token = JwtTokenUtil.generateToken(String.valueOf(user.getId()), user.getUsername(), String.valueOf(user.getUserType()));
       System.out.println(token);
       UserLoginResponseDTO.UserDetailResponseDTO userInfo = UserConvert.entityToDetailResponse(user);
       return UserConvert.entityToLoginResponse(token, userInfo);
   }
}
