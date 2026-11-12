package com.example.aispring.service;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.example.aispring.common.ResultCode;
import com.example.aispring.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;

/**
 * 本地文件存储。文件落在 app.upload-dir（默认项目根目录下的 uploads），
 * 对外通过 /uploads/** 静态映射访问，所以返回给前端的 filePath 是 "/uploads/xxx" 形式
 */
@Service
public class FileStorageService {

    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    public String store(MultipartFile file, String businessType) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }
        String originalName = StrUtil.blankToDefault(file.getOriginalFilename(), "file");
        String extension = StrUtil.subAfter(originalName, ".", true);
        String safeExtension = StrUtil.isBlank(extension) ? "png" : extension.toLowerCase();

        String type = StrUtil.blankToDefault(businessType, "COMMON").toUpperCase();
        String datePath = DateUtil.format(LocalDateTime.now(), "yyyy/MM");
        String fileName = IdUtil.simpleUUID() + "." + safeExtension;

        String relativePath = "/uploads/" + type + "/" + datePath + "/" + fileName;
        Path target = root().resolve(type).resolve(datePath).resolve(fileName);

        try {
            Files.createDirectories(target.getParent());
            file.transferTo(target.toFile());
        } catch (IOException e) {
            throw new BusinessException("文件保存失败：" + e.getMessage());
        }
        return relativePath;
    }

    /**
     * 静态资源映射需要绝对路径
     */
    public Path root() {
        return Paths.get(uploadDir).toAbsolutePath().normalize();
    }
}
