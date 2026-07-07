package com.delta.esports.controller;

import com.delta.esports.common.Result;
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
    @PostMapping("/send")
    public Result<?> send(HttpServletRequest request,
                          @RequestParam Long receiverId,
                          @RequestParam String giftName,
                          @RequestParam BigDecimal price,
                          @RequestParam(required = false) String message) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(giftService.sendGift(userId, receiverId, giftName, price, message));
    }
}
