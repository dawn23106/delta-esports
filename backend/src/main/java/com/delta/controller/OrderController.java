package com.delta.controller;

import com.delta.common.PageResult;
import com.delta.common.Result;
import com.delta.entity.Order;
import com.delta.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public Result<Order> create(@Valid @RequestBody CreateOrderReq req, HttpServletRequest r) {
        Long userId = (Long) r.getAttribute("userId");
        return Result.ok(orderService.create(userId, req.getGame(), req.getServiceType(),
                req.getDetail(), req.getPrice(), req.getSourceChannel()));
    }

    @GetMapping("/my")
    public Result<PageResult<Order>> myOrders(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            HttpServletRequest r) {
        Long userId = (Long) r.getAttribute("userId");
        return Result.ok(orderService.findMyOrders(userId, page, pageSize));
    }

    @PostMapping("/{id}/cancel")
    public Result<Order> cancel(@PathVariable Long id, HttpServletRequest r) {
        Long userId = (Long) r.getAttribute("userId");
        String role = (String) r.getAttribute("role");
        return Result.ok(orderService.cancel(id, userId, role));
    }

    @GetMapping("/pool")
    public Result<PageResult<Order>> pool(
            @RequestParam(required = false) String game,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return Result.ok(orderService.findPool(game, page, pageSize));
    }

    @GetMapping("/my-booster")
    public Result<PageResult<Order>> boosterOrders(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            HttpServletRequest r) {
        Long userId = (Long) r.getAttribute("userId");
        return Result.ok(orderService.findBoosterOrders(userId, page, pageSize));
    }

    @PostMapping("/{id}/claim")
    public Result<Order> claim(@PathVariable Long id, HttpServletRequest r) {
        Long userId = (Long) r.getAttribute("userId");
        return Result.ok(orderService.claim(id, userId));
    }

    @PostMapping("/{id}/start")
    public Result<Order> start(@PathVariable Long id, HttpServletRequest r) {
        Long userId = (Long) r.getAttribute("userId");
        return Result.ok(orderService.start(id, userId));
    }

    @PostMapping("/{id}/complete")
    public Result<Order> complete(@PathVariable Long id, HttpServletRequest r) {
        Long userId = (Long) r.getAttribute("userId");
        return Result.ok(orderService.complete(id, userId));
    }
}

class CreateOrderReq {
    @NotBlank(message = "游戏名不能为空") private String game;
    private String serviceType;
    private String detail;
    @NotNull(message = "价格不能为空") private BigDecimal price;
    private String sourceChannel;
    public String getGame() { return game; }
    public void setGame(String game) { this.game = game; }
    public String getServiceType() { return serviceType; }
    public void setServiceType(String serviceType) { this.serviceType = serviceType; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public String getSourceChannel() { return sourceChannel; }
    public void setSourceChannel(String sourceChannel) { this.sourceChannel = sourceChannel; }
}
