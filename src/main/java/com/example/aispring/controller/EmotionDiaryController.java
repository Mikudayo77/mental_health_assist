package com.example.aispring.controller;

import com.example.aispring.DTO.command.EmotionDiaryCommandDTO;
import com.example.aispring.DTO.response.EmotionDiaryResponseDTO;
import com.example.aispring.common.PageResult;
import com.example.aispring.common.Result;
import com.example.aispring.common.ResultCode;
import com.example.aispring.service.EmotionDiaryService;
import com.example.aispring.util.JwtTokenUtil;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 情绪日记：用户端提交 + 后台分页/删除
 * 路径与前端 src/api/frontend.js、src/api/admin.js 对应
 */
@RestController
@RequestMapping("/api/emotion-diary")
public class EmotionDiaryController {

    @Autowired
    private EmotionDiaryService emotionDiaryService;

    /**
     * 用户提交今日情绪日记
     */
    @PostMapping
    public Result<Long> add(@Valid @RequestBody EmotionDiaryCommandDTO dto) {
        Long userId = JwtTokenUtil.getCurrentUserId();
        return Result.ok(emotionDiaryService.create(dto, userId));
    }

    /**
     * 后台分页。前端参数：current / size / userId / moodScreRange
     */
    @GetMapping("/admin/page")
    public Result<PageResult<EmotionDiaryResponseDTO>> page(
            @RequestParam(value = "current", defaultValue = "1") Integer current,
            @RequestParam(value = "size", defaultValue = "10") Integer size,
            @RequestParam(value = "userId", required = false) Long userId,
            @RequestParam(value = "moodScreRange", required = false) String moodScoreRange) {
        return Result.ok(emotionDiaryService.page(current, size, userId, moodScoreRange));
    }

    /**
     * 后台删除日记
     */
    @DeleteMapping("/admin/{id}")
    public Result<Void> delete(@PathVariable("id") Long id) {
        if (!emotionDiaryService.delete(id)) {
            return Result.error(ResultCode.BUSINESS_ERROR.getCode(), "日记不存在", null);
        }
        return Result.ok();
    }
}
