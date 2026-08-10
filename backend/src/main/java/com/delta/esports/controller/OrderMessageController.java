package com.delta.esports.controller;

import com.delta.esports.common.GlobalExceptionHandler.BusinessException;
import com.delta.esports.common.Result;
import com.delta.esports.service.OrderMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/messages")
public class OrderMessageController {
    @Autowired
    private OrderMessageService messageService;

    @GetMapping("/{orderId}")
    public Result<?> list(HttpServletRequest request, @PathVariable Long orderId) {
        return Result.success(messageService.list(getUserId(request), orderId));
    }

    @PostMapping
    public Result<?> send(HttpServletRequest request,
                          @RequestParam Long orderId,
                          @RequestParam String content,
                          @RequestParam(defaultValue = "text") String type) {
        return Result.success(messageService.send(getUserId(request), orderId, content, type));
    }

    private Long getUserId(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) throw new BusinessException(401, "未登录");
        return userId;
    }
}
