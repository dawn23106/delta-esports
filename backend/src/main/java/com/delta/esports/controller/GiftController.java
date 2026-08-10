package com.delta.esports.controller;

import com.delta.esports.common.Result;
import com.delta.esports.config.RequireRole;
import com.delta.esports.service.GiftService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;

@Tag(name = "礼物", description = "礼物赠送相关接口")
@RestController
@RequestMapping("/api/gifts")
public class GiftController {

    @Autowired
    private GiftService giftService;

    @Operation(summary = "赠送礼物")
    @RequireRole("boss")
    @PostMapping({"", "/send"})
    public Result<?> send(HttpServletRequest request,
                          @RequestParam(name = "boosterId", required = false) Long boosterId,
                          @RequestParam(name = "receiverId", required = false) Long receiverId,
                          @RequestParam String giftName,
                          @RequestParam(name = "amount", required = false) BigDecimal amount,
                          @RequestParam(name = "price", required = false) BigDecimal price,
                          @RequestParam(required = false) String message) {
        Long userId = (Long) request.getAttribute("userId");
        Long target = boosterId != null ? boosterId : receiverId;
        BigDecimal actualAmount = amount != null ? amount : price;
        return Result.success(giftService.sendGift(userId, target, giftName, actualAmount, message));
    }

    @GetMapping("/sent")
    public Result<?> sent(HttpServletRequest request,
                          @RequestParam(defaultValue = "1") int page,
                          @RequestParam(defaultValue = "20") int size) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(giftService.sentBy(userId, page, size));
    }
}
