package com.example.aispring.util;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.example.aispring.DTO.response.UserLoginResponseDTO;
import com.example.aispring.common.ResultCode;
import com.example.aispring.config.SecurityConfig;
import com.example.aispring.enumClass.UserStatus;
import com.example.aispring.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class JwtAuthenticationFilter extends OncePerRequestFilter {
    @Resource
    private  UserService userService;

    /**
     * 只有完全公开的路径（登录、注册、探活）直接跳过过滤器。
     * 知识库只读接口不能跳过：跳过的话带 token 的管理员也会被当成匿名用户，
     * 结果就是查不到草稿、状态筛选失效。这类路径改为"有 token 就解析，没 token 放行"。
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return SecurityConfig.isPublicPATH(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        String requestUri = request.getRequestURI();
        String method = request.getMethod();
        // 只读公开接口：没有有效 token 也继续往下走，由 Controller 按匿名处理
        boolean publicRead = "GET".equalsIgnoreCase(method) && SecurityConfig.isPublicGetPath(requestUri);

        // 1. 提取 JWT token
        String token = JwtTokenUtil.extractTokenFromRequest(request);
        if (!StringUtils.hasText(token)) {
            clearSecurityContext();
            if (publicRead) {
                chain.doFilter(request, response);
                return;
            }
            ResponseUtil.writeError(response, ResultCode.ACCESS_UNAUTHORIZED);
            return;
        }

        // 2. 验证token并获取用户信息（token 过期/伪造时不能抛异常，否则公开接口也会被打断）
        JwtTokenUtil.TokenVerificationResult validationResult;
        try {
            validationResult = JwtTokenUtil.validateToken(token);
        } catch (JWTVerificationException | IllegalArgumentException e) {
            validationResult = null;
        }

        if (validationResult == null || !validationResult.isValid()) {
            clearSecurityContext();
            if (publicRead) {
                chain.doFilter(request, response);
                return;
            }
            ResponseUtil.writeError(response, ResultCode.TOKEN_INVALID);
            return;
        }

        // 3. 查询用户信息验证用户的状态
        UserLoginResponseDTO.UserDetailResponseDTO user = userService.getUserById(validationResult.getUserId());
        if (user != null && UserStatus.NORMAL.getCode().equals(user.getStatus())) {
            // 4. 创建Spring Security认证对象
            List<SimpleGrantedAuthority> authorities = Collections.singletonList(
                    new SimpleGrantedAuthority("ROLE_" + validationResult.getRoleType())
            );

            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    validationResult.getUsername(), // 用户名作为主体
                    null,
                    authorities
            );

            // 设置认证信息到Spring Security上下文
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // 将token存储到请求属性中
            request.setAttribute("jwtToken", token);
        } else {
            clearSecurityContext();
            if (publicRead) {
                chain.doFilter(request, response);
                return;
            }
            ResponseUtil.writeError(response, ResultCode.TOKEN_ACCESS_FORBIDDEN);
            return;
        }
        chain.doFilter(request, response);
    }

    private void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }
}
