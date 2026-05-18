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

/**
 * 订单控制器 — 玩家下单、打手接单/开始/完成。
 * 所有接口都需要登录（经过 JwtAuthFilter），从 request 中取 userId 和 role。
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /** 玩家下单 — 创建订单，默认状态 pending（待接单） */
    @PostMapping
    public Result<Order> create(@Valid @RequestBody CreateOrderReq req, HttpServletRequest r) {
        Long userId = (Long) r.getAttribute("userId");
        // userId 来自 JWT 令牌，不是前端传的，防止伪造
        return Result.ok(orderService.create(userId, req.getGame(), req.getServiceType(),
                req.getDetail(), req.getPrice(), req.getSourceChannel()));
    }

    /** 玩家查看自己的订单列表 */
    @GetMapping("/my")
    public Result<PageResult<Order>> myOrders(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            HttpServletRequest r) {
        Long userId = (Long) r.getAttribute("userId");
        return Result.ok(orderService.findMyOrders(userId, page, pageSize));
    }

    /**
     * 取消订单 — 玩家可取消自己的订单，客服可取消任意订单。
     * 已完成的订单不可取消。
     */
    @PostMapping("/{id}/cancel")
    public Result<Order> cancel(@PathVariable Long id, HttpServletRequest r) {
        Long userId = (Long) r.getAttribute("userId");
        String role = (String) r.getAttribute("role");
        return Result.ok(orderService.cancel(id, userId, role));
    }

    /** 打手浏览可接订单池 — 只展示 pending 状态的订单 */
    @GetMapping("/pool")
    public Result<PageResult<Order>> pool(
            @RequestParam(required = false) String game,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return Result.ok(orderService.findPool(game, page, pageSize));
    }

    /** 打手查看自己已接的订单 */
    @GetMapping("/my-booster")
    public Result<PageResult<Order>> boosterOrders(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            HttpServletRequest r) {
        Long userId = (Long) r.getAttribute("userId");
        return Result.ok(orderService.findBoosterOrders(userId, page, pageSize));
    }

    /** 打手抢单 — 使用乐观锁，只有第一个请求能成功 */
    @PostMapping("/{id}/claim")
    public Result<Order> claim(@PathVariable Long id, HttpServletRequest r) {
        Long userId = (Long) r.getAttribute("userId");
        return Result.ok(orderService.claim(id, userId));
    }

    /** 打手开始代练 — 状态从 assigned → in_progress */
    @PostMapping("/{id}/start")
    public Result<Order> start(@PathVariable Long id, HttpServletRequest r) {
        Long userId = (Long) r.getAttribute("userId");
        return Result.ok(orderService.start(id, userId));
    }

    /** 打手提交完成 — 状态从 in_progress → completed */
    @PostMapping("/{id}/complete")
    public Result<Order> complete(@PathVariable Long id, HttpServletRequest r) {
        Long userId = (Long) r.getAttribute("userId");
        return Result.ok(orderService.complete(id, userId));
    }
}

/** 创建订单请求体 */
class CreateOrderReq {
    @NotBlank(message = "游戏名不能为空") private String game;
    private String serviceType;       // tech(技术) / entertain(娱乐) / quest(任务)
    private String detail;            // 订单描述
    @NotNull(message = "价格不能为空") private BigDecimal price;
    private String sourceChannel;     // 获客渠道，如 web_h5、wechat_share
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
