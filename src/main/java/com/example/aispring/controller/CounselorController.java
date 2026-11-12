package com.example.aispring.controller;

import com.example.aispring.DTO.command.CounselorApplyCommandDTO;
import com.example.aispring.DTO.command.CounselorAuditCommandDTO;
import com.example.aispring.DTO.response.CounselorResponseDTO;
import com.example.aispring.common.PageResult;
import com.example.aispring.common.Result;
import com.example.aispring.service.CounselorService;
import com.example.aispring.util.JwtTokenUtil;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/counselor")
public class CounselorController {

    @Resource
    private CounselorService counselorService;

    @PostMapping("/apply")
    @PreAuthorize("hasRole('1')")
    public Result<Void> apply(@Valid @RequestBody CounselorApplyCommandDTO dto) {
        counselorService.apply(JwtTokenUtil.getCurrentUserId(), dto);
        return Result.ok();
    }

    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('1','3')")
    public Result<CounselorResponseDTO> my() {
        return Result.ok(counselorService.getByUserId(JwtTokenUtil.getCurrentUserId()));
    }

    @GetMapping("/list")
    @PreAuthorize("hasAnyRole('1','3')")
    public Result<List<CounselorResponseDTO>> list() {
        return Result.ok(counselorService.enabledList());
    }

    @GetMapping("/page")
    @PreAuthorize("hasRole('2')")
    public Result<PageResult<CounselorResponseDTO>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer auditStatus) {
        return Result.ok(counselorService.pageList(page, size, auditStatus));
    }

    @PostMapping("/audit")
    @PreAuthorize("hasRole('2')")
    public Result<Void> audit(@Valid @RequestBody CounselorAuditCommandDTO dto) {
        counselorService.audit(JwtTokenUtil.getCurrentUserId(), dto);
        return Result.ok();
    }
}
