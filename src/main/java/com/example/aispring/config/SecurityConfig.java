package com.example.aispring.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    private static  final  String[] PUBLIC_PATHS = {
            "/","/error","/api/test","/api/user/login","/api/user/add"};
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
     http
             //禁用csrf保护
             .csrf(AbstractHttpConfigurer::disable)
             //配置会话管理为无状态
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
             .authorizeHttpRequests(auth -> auth
                     //公开的路径，无需登录即可访问
                     .requestMatchers(PUBLIC_PATHS).permitAll()
                     //其他请求都需要认证
                     .anyRequest().authenticated()

             );
  return http.build();
    }
}