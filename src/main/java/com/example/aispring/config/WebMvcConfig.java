package com.example.aispring.config;

import com.example.aispring.service.FileStorageService;
import jakarta.servlet.MultipartConfigElement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 静态资源与上传限制配置：
 * 1. 把本地 uploads 目录映射到 /uploads/**，让封面图能直接通过 URL 访问
 * 2. 放宽上传体积上限（Spring Boot 默认单文件 1MB，前端允许 5MB 封面）
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private FileStorageService fileStorageService;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path root = fileStorageService.root();
        try {
            Files.createDirectories(root);
        } catch (IOException ignored) {
            // 目录创建失败时留给上传环节再报错
        }
        String location = root.toUri().toString();
        if (!location.endsWith("/")) {
            location = location + "/";
        }
        registry.addResourceHandler("/uploads/**").addResourceLocations(location);
    }

    /**
     * 单文件 10MB、单请求 20MB（Spring Boot 默认 1MB，不够传封面图）
     * Boot 4 已移除 MultipartConfigFactory，直接用 Servlet 构造器
     */
    @Bean
    public MultipartConfigElement multipartConfigElement() {
        long maxFileSize = 10L * 1024 * 1024;
        long maxRequestSize = 20L * 1024 * 1024;
        return new MultipartConfigElement("", maxFileSize, maxRequestSize, 0);
    }
}
