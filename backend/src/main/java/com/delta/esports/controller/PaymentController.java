package com.delta.esports.controller;

import com.delta.esports.common.GlobalExceptionHandler.BusinessException;
import com.delta.esports.common.Result;
import com.delta.esports.dto.PreparePaymentRequest;
import com.delta.esports.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@Tag(name = "支付", description = "微信小程序支付")
@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @Operation(summary = "创建或重新获取小程序支付参数")
    @PostMapping("/orders/{orderId}/prepare")
    public Result<?> prepare(HttpServletRequest request, @PathVariable Long orderId,
                             @RequestBody(required = false) PreparePaymentRequest body) {
        String loginCode = body == null ? null : body.getLoginCode();
        return Result.success(paymentService.prepare(userId(request), orderId, loginCode));
    }

    @Operation(summary = "主动查询并同步支付状态")
    @PostMapping("/orders/{orderId}/query")
    public Result<?> query(HttpServletRequest request, @PathVariable Long orderId) {
        return Result.success(paymentService.queryAndSync(userId(request), orderId));
    }

    @Operation(summary = "开发环境模拟支付成功")
    @PostMapping("/orders/{orderId}/mock-confirm")
    public Result<?> mockConfirm(HttpServletRequest request, @PathVariable Long orderId) {
        return Result.success(paymentService.confirmMock(userId(request), orderId));
    }

    @PostMapping(value = "/callbacks/provider/pay", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.TEXT_PLAIN_VALUE)
    public String payCallback(@RequestParam Map<String, String> callback) {
        return paymentService.handlePayCallback(callback);
    }

    @PostMapping(value = "/callbacks/provider/refund", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.TEXT_PLAIN_VALUE)
    public String refundCallback(@RequestParam Map<String, String> callback) {
        return paymentService.handleRefundCallback(callback);
    }

    private Long userId(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) throw new BusinessException(401, "未登录");
        return userId;
    }
}
