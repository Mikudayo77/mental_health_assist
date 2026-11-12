package com.example.aispring.controller;

import com.example.aispring.DTO.command.MockPayCommandDTO;
import com.example.aispring.DTO.response.PaymentResponseDTO;
import com.example.aispring.common.Result;
import com.example.aispring.service.PaymentService;
import com.example.aispring.util.JwtTokenUtil;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    @Resource
    private PaymentService paymentService;

    @PostMapping("/mock-pay")
    @PreAuthorize("hasRole('1')")
    public Result<PaymentResponseDTO> mockPay(@Valid @RequestBody MockPayCommandDTO dto) {
        return Result.ok(paymentService.mockPay(dto.getOrderNo(), JwtTokenUtil.getCurrentUserId()));
    }

    @GetMapping("/{orderNo}")
    @PreAuthorize("hasRole('1')")
    public Result<List<PaymentResponseDTO>> listByOrder(@PathVariable String orderNo) {
        return Result.ok(paymentService.listByOrder(orderNo, JwtTokenUtil.getCurrentUserId()));
    }
}
