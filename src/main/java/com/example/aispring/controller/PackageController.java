package com.example.aispring.controller;

import com.example.aispring.DTO.command.PackageCommandDTO;
import com.example.aispring.DTO.response.PackageResponseDTO;
import com.example.aispring.common.PageResult;
import com.example.aispring.common.Result;
import com.example.aispring.service.CounselingPackageService;
import com.example.aispring.util.JwtTokenUtil;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/package")
public class PackageController {

    @Resource
    private CounselingPackageService packageService;

    @PostMapping("/create")
    @PreAuthorize("hasRole('2')")
    public Result<Void> create(@Valid @RequestBody PackageCommandDTO dto) {
        packageService.create(dto, JwtTokenUtil.getCurrentUserId());
        return Result.ok();
    }

    @PutMapping("/update/{id}")
    @PreAuthorize("hasRole('2')")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody PackageCommandDTO dto) {
        packageService.update(id, dto);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('2')")
    public Result<Void> delete(@PathVariable Long id) {
        packageService.delete(id);
        return Result.ok();
    }

    @PutMapping("/status/{id}/{status}")
    @PreAuthorize("hasRole('2')")
    public Result<Void> changeStatus(@PathVariable Long id, @PathVariable Integer status) {
        packageService.changeStatus(id, status);
        return Result.ok();
    }

    @GetMapping("/page")
    @PreAuthorize("hasRole('2')")
    public Result<PageResult<PackageResponseDTO>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer status) {
        return Result.ok(packageService.pageList(page, size, name, status));
    }

    @GetMapping("/list")
    @PreAuthorize("hasAnyRole('1','3')")
    public Result<List<PackageResponseDTO>> list() {
        return Result.ok(packageService.onSaleList());
    }
}
