package com.example.aispring.util;

import com.example.aispring.common.ResultCode;
import com.example.aispring.config.SecurityConfig;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class JwtAuthenticationFilter extends OncePerRequestFilter {
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        // 检查是否为公开路径
        return SecurityConfig.isPublicPATH(requestUri);
    }
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        String requestUri = request.getRequestURI();
        String method = request.getMethod();
        System.out.println(requestUri);
        System.out.println(method);
        //提取token
        String token =JwtTokenUtil.extractTokenFromRequest(request);
        if(StringUtils.hasText(token)){

        }else {
            // 清理上下文
            clearSecurityContext();
            ResponseUtil.writeError(response, ResultCode.ACCESS_UNAUTHORIZED);
            return;
        }
        chain.doFilter(request, response);
    }

    private void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }
}
