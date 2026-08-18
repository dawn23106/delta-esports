package com.delta.esports.controller;

import com.delta.esports.common.GlobalExceptionHandler.BusinessException;
import com.delta.esports.common.Result;
import com.delta.esports.config.RequireRole;
import com.delta.esports.service.WithdrawalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;

@Tag(name = "提现", description = "陪陪提现相关接口")
@RestController
@RequestMapping("/api/withdrawals")
public class WithdrawalController {

    @Autowired
    private WithdrawalService withdrawalService;

    private Long userId(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) throw new BusinessException(401, "未登录");
        return userId;
    }

    @Operation(summary = "申请提现（结算满锁定期后可提）")
    @RequireRole("booster")
    @PostMapping
    public Result<?> apply(HttpServletRequest request, @RequestParam BigDecimal amount) {
        return Result.success(withdrawalService.apply(userId(request), amount));
    }

    @Operation(summary = "我的提现记录")
    @RequireRole("booster")
    @GetMapping("/my")
    public Result<?> my(HttpServletRequest request,
                        @RequestParam(defaultValue = "1") int page,
                        @RequestParam(defaultValue = "10") int size) {
        return Result.success(withdrawalService.myList(userId(request), page, size));
    }
}
