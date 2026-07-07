package com.delta.esports.controller;

import com.delta.esports.common.GlobalExceptionHandler.BusinessException;
import com.delta.esports.common.Result;
import com.delta.esports.dto.CompleteOrderRequest;
import com.delta.esports.dto.CreateOrderRequest;
import com.delta.esports.entity.Order;
import com.delta.esports.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

@Tag(name = "订单", description = "订单相关接口")
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    private Long getUserId(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) throw new BusinessException(401, "未登录");
        return userId;
    }

    @Operation(summary = "订单大厅（待接订单池）")
    @GetMapping("/pool")
    public Result<?> pool(@RequestParam(defaultValue = "1") int page,
                          @RequestParam(defaultValue = "10") int size) {
        return Result.success(orderService.getPool(page, size));
    }

    @Operation(summary = "我的订单")
    @GetMapping("/my")
    public Result<?> myOrders(HttpServletRequest request,
                              @RequestParam(defaultValue = "1") int page,
                              @RequestParam(defaultValue = "10") int size,
                              @RequestParam(required = false) String status) {
        return Result.success(orderService.getMyOrders(getUserId(request), page, size, status));
    }

    @Operation(summary = "订单详情")
    @GetMapping("/{id}")
    public Result<?> detail(@PathVariable Long id) {
        Order order = orderService.getDetail(id);
        if (order == null) return Result.error("订单不存在");
        return Result.success(order);
    }

    @Operation(summary = "创建订单")
    @PostMapping
    public Result<?> create(HttpServletRequest request, @Valid @RequestBody CreateOrderRequest req) {
        return Result.success(orderService.createOrder(getUserId(request), req));
    }

    @Operation(summary = "陪陪接单")
    @PostMapping("/{id}/claim")
    public Result<?> claim(HttpServletRequest request, @PathVariable Long id) {
        return Result.success(orderService.claimOrder(getUserId(request), id));
    }

    @Operation(summary = "开始服务")
    @PostMapping("/{id}/start")
    public Result<?> start(HttpServletRequest request, @PathVariable Long id) {
        return Result.success(orderService.startOrder(getUserId(request), id));
    }

    @Operation(summary = "陪陪提交成果")
    @PostMapping("/submit")
    public Result<?> submit(HttpServletRequest request, @Valid @RequestBody CompleteOrderRequest req) {
        return Result.success(orderService.submitOrder(getUserId(request), req));
    }

    @Operation(summary = "老板确认完成")
    @PostMapping("/{id}/confirm")
    public Result<?> confirm(HttpServletRequest request, @PathVariable Long id) {
        return Result.success(orderService.bossConfirmOrder(getUserId(request), id));
    }

    @Operation(summary = "取消订单")
    @PostMapping("/{id}/cancel")
    public Result<?> cancel(HttpServletRequest request, @PathVariable Long id) {
        return Result.success(orderService.cancelOrder(getUserId(request), id));
    }
}
