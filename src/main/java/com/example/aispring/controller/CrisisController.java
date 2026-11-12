package com.example.aispring.controller;

import com.example.aispring.DTO.command.CrisisHandleCommandDTO;
import com.example.aispring.DTO.response.CrisisRecordResponseDTO;
import com.example.aispring.common.PageResult;
import com.example.aispring.common.Result;
import com.example.aispring.service.CrisisInterventionService;
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
@RequestMapping("/api/crisis")
public class CrisisController {

    @Resource
    private CrisisInterventionService crisisInterventionService;

    @GetMapping("/page")
    @PreAuthorize("hasRole('2')")
    public Result<PageResult<CrisisRecordResponseDTO>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String riskLevel,
            @RequestParam(required = false) Integer handleStatus) {
        return Result.ok(crisisInterventionService.pageList(page, size, riskLevel, handleStatus));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('2')")
    public Result<CrisisRecordResponseDTO> detail(@PathVariable Long id) {
        return Result.ok(crisisInterventionService.getDetail(id));
    }

    @PostMapping("/{id}/handle")
    @PreAuthorize("hasRole('2')")
    public Result<Void> handle(@PathVariable Long id, @Valid @RequestBody CrisisHandleCommandDTO dto) {
        dto.setCrisisId(id);
        crisisInterventionService.handle(JwtTokenUtil.getCurrentUserId(), dto);
        return Result.ok();
    }
}
