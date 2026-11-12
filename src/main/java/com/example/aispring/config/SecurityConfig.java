package com.example.aispring.config;

import cn.hutool.core.text.AntPathMatcher;
import com.example.aispring.util.JwtAuthenticationFilter;
import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    private static final AntPathMatcher antPathMatcher = new AntPathMatcher();
    private static  final  String[] PUBLIC_PATHS = {
            "/","/error","/api/test","/api/user/login","/api/user/add"};
    // 公开的只读接口：只对 GET 放行，避免写接口被一并放开
    private static final String[] PUBLIC_GET_PATHS = {
            "/uploads/**",
            "/api/knowledge/category/tree",
            "/api/knowledge/article/page",
            "/api/knowledge/article/*"};

    public static Boolean isPublicPATH(String requestUri) {
        for (String publicPath : PUBLIC_PATHS) {
            if (antPathMatcher.match(publicPath, requestUri)) {
                return true;
            }
        }
        return false;
    }

    public static Boolean isPublicGetPath(String requestUri) {
        for (String publicPath : PUBLIC_GET_PATHS) {
            if (antPathMatcher.match(publicPath, requestUri)) {
                return true;
            }
        }
        return false;
    }
    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
     http
             //禁用csrf保护
             .csrf(AbstractHttpConfigurer::disable)
             //配置会话管理为无状态
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
             .authorizeHttpRequests(auth -> auth
                     //SSE 流式响应完成后的异步派发重新进入过滤器链，需放行
                     .dispatcherTypeMatchers(DispatcherType.ASYNC).permitAll()
                     //公开的路径，无需登录即可访问
                     .requestMatchers(PUBLIC_PATHS).permitAll()
                     //知识库只读接口与上传文件，GET 免登录
                     .requestMatchers(HttpMethod.GET, PUBLIC_GET_PATHS).permitAll()
                     //其他请求都需要认证
                     .anyRequest().authenticated()

             );
     //添加JWT认证过滤器
        http.addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);
  return http.build();
    }
}