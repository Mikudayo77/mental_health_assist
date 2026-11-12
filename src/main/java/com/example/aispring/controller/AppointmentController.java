package com.example.aispring.controller;

import com.example.aispring.DTO.command.AppointmentCreateCommandDTO;
import com.example.aispring.DTO.response.AppointmentResponseDTO;
import com.example.aispring.DTO.response.OrderCreateResponseDTO;
import com.example.aispring.common.PageResult;
import com.example.aispring.common.Result;
import com.example.aispring.service.AppointmentService;
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
@RequestMapping("/api/appointment")
public class AppointmentController {

    @Resource
    private AppointmentService appointmentService;

    @PostMapping("/create")
    @PreAuthorize("hasRole('1')")
    public Result<OrderCreateResponseDTO> create(@Valid @RequestBody AppointmentCreateCommandDTO dto) {
        return Result.ok(appointmentService.createWithOrder(JwtTokenUtil.getCurrentUserId(), dto));
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('1')")
    public Result<PageResult<AppointmentResponseDTO>> mine(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer status) {
        return Result.ok(appointmentService.pageMine(JwtTokenUtil.getCurrentUserId(), page, size, status));
    }

    @GetMapping("/counselor")
    @PreAuthorize("hasRole('3')")
    public Result<PageResult<AppointmentResponseDTO>> counselorAppointments(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer status) {
        return Result.ok(appointmentService.pageCounselor(JwtTokenUtil.getCurrentUserId(), page, size, status));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('1','3')")
    public Result<AppointmentResponseDTO> detail(@PathVariable Long id) {
        return Result.ok(appointmentService.getDetail(id, JwtTokenUtil.getCurrentUserId()));
    }

    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasRole('3')")
    public Result<Void> confirm(@PathVariable Long id) {
        appointmentService.confirm(JwtTokenUtil.getCurrentUserId(), id);
        return Result.ok();
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('3')")
    public Result<Void> reject(@PathVariable Long id, @RequestParam(required = false) String reason) {
        appointmentService.reject(JwtTokenUtil.getCurrentUserId(), id, reason);
        return Result.ok();
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('1')")
    public Result<Void> cancel(@PathVariable Long id, @RequestParam(required = false) String reason) {
        appointmentService.cancelByUser(JwtTokenUtil.getCurrentUserId(), id, reason);
        return Result.ok();
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasRole('3')")
    public Result<Void> complete(@PathVariable Long id) {
        appointmentService.complete(JwtTokenUtil.getCurrentUserId(), id);
        return Result.ok();
    }
}
