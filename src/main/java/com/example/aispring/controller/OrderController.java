package com.example.aispring.controller;

import com.example.aispring.DTO.command.CancelOrderCommandDTO;
import com.example.aispring.DTO.response.OrderResponseDTO;
import com.example.aispring.common.PageResult;
import com.example.aispring.common.Result;
import com.example.aispring.service.CounselingOrderService;
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
@RequestMapping("/api/order")
public class OrderController {

    @Resource
    private CounselingOrderService orderService;

    @GetMapping("/mine")
    @PreAuthorize("hasRole('1')")
    public Result<PageResult<OrderResponseDTO>> mine(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer status) {
        return Result.ok(orderService.pageMine(JwtTokenUtil.getCurrentUserId(), page, size, status));
    }

    @GetMapping("/{orderNo}")
    @PreAuthorize("hasRole('1')")
    public Result<OrderResponseDTO> detail(@PathVariable String orderNo) {
        return Result.ok(orderService.getDetail(orderNo, JwtTokenUtil.getCurrentUserId()));
    }

    @PostMapping("/cancel")
    @PreAuthorize("hasRole('1')")
    public Result<Void> cancel(@Valid @RequestBody CancelOrderCommandDTO dto) {
        orderService.cancelUnpaid(JwtTokenUtil.getCurrentUserId(), dto.getOrderNo());
        return Result.ok();
    }
}
