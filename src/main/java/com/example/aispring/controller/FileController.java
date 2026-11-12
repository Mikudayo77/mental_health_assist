package com.example.aispring.controller;

import com.example.aispring.common.Result;
import com.example.aispring.service.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

/**
 * 文件上传。前端 ArticleDialog.vue 提交四个字段：
 * file、businessType=ARTICLE、businessId=uuid、businessField=cover
 * 返回的 filePath 形如 /uploads/ARTICLE/2026/11/xx.png，前端用 fileBaseUrl + filePath 显示
 */
@RestController
@RequestMapping("/api/file")
public class FileController {

    @Autowired
    private FileStorageService fileStorageService;

    @PostMapping("/upload")
    public Result<Map<String, String>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "businessType", required = false) String businessType,
            @RequestParam(value = "businessId", required = false) String businessId,
            @RequestParam(value = "businessField", required = false) String businessField) {

        String filePath = fileStorageService.store(file, businessType);

        Map<String, String> data = new HashMap<>();
        data.put("filePath", filePath);
        data.put("fileName", filePath.substring(filePath.lastIndexOf('/') + 1));
        data.put("businessId", businessId == null ? "" : businessId);
        data.put("businessField", businessField == null ? "" : businessField);
        return Result.ok(data);
    }
}
