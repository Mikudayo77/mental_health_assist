package com.example.aispring.service;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.aispring.DTO.command.UserLoginCommandDTO;
import com.example.aispring.DTO.command.UserRegisterCommandDTO;
import com.example.aispring.DTO.response.UserLoginResponseDTO;
import com.example.aispring.entity.User;
import com.example.aispring.enumClass.UserStatus;
import com.example.aispring.enumClass.UserType;
import com.example.aispring.exception.BusinessException;
import com.example.aispring.mapper.UserMapper;
import com.example.aispring.service.convert.UserConvert;
import com.example.aispring.util.JwtTokenUtil;
import jakarta.annotation.Resource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

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


   public UserLoginResponseDTO.UserDetailResponseDTO register(UserRegisterCommandDTO commandDTO) {
       System.out.println(JSONUtil.parseObj(commandDTO));
       // 校验两次密码是否一致
       if(!commandDTO.getPassword().equals(commandDTO.getConfirmPassword())){
           throw new BusinessException("两次输入的密码不一致");
       }
       // 校验用户名是否已存在
       if(userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getUsername, commandDTO.getUsername())) > 0){
           throw new BusinessException("用户名已存在");
       }
       // 校验邮箱是否已被注册
       if(userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getEmail, commandDTO.getEmail())) > 0){
           throw new BusinessException("邮箱已被注册");
       }
       if(!UserType.isValidCode(commandDTO.getUserType())){
           throw new BusinessException("用户类型无效");
       }
       // 创建用户
       String password = commandDTO.getPassword().trim();
       String encodedPassword = passwordEncoder.encode(password);
       User user = UserConvert.registerCommandToEntity(commandDTO, encodedPassword);

       // 插入数据库
       userMapper.insert(user);

       return UserConvert.entityToDetailResponse(user);
   }
}
