package com.example.aispring.controller;

import com.example.aispring.DTO.command.RefundApplyCommandDTO;
import com.example.aispring.DTO.command.RefundAuditCommandDTO;
import com.example.aispring.DTO.response.RefundPreviewResponseDTO;
import com.example.aispring.DTO.response.RefundResponseDTO;
import com.example.aispring.common.PageResult;
import com.example.aispring.common.Result;
import com.example.aispring.service.RefundService;
import com.example.aispring.util.JwtTokenUtil;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/refund")
public class RefundController {

    @Resource
    private RefundService refundService;

    @GetMapping("/preview/{orderId}")
    @PreAuthorize("hasRole('1')")
    public Result<RefundPreviewResponseDTO> preview(@PathVariable Long orderId) {
        return Result.ok(refundService.preview(JwtTokenUtil.getCurrentUserId(), orderId));
    }

    @PostMapping("/apply")
    @PreAuthorize("hasRole('1')")
    public Result<Void> apply(@Valid @RequestBody RefundApplyCommandDTO dto) {
        refundService.apply(JwtTokenUtil.getCurrentUserId(), dto);
        return Result.ok();
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('1')")
    public Result<PageResult<RefundResponseDTO>> mine(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.ok(refundService.pageMine(JwtTokenUtil.getCurrentUserId(), page, size));
    }

    @GetMapping("/page")
    @PreAuthorize("hasRole('2')")
    public Result<PageResult<RefundResponseDTO>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer status) {
        return Result.ok(refundService.pageAll(page, size, status));
    }

    @PostMapping("/audit")
    @PreAuthorize("hasRole('2')")
    public Result<Void> audit(@Valid @RequestBody RefundAuditCommandDTO dto) {
        refundService.audit(JwtTokenUtil.getCurrentUserId(), dto);
        return Result.ok();
    }
}
