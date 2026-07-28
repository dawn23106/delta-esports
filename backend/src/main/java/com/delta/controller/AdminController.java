package com.delta.controller;

import com.delta.common.BusinessException;
import com.delta.common.ErrorCode;
import com.delta.common.PageResult;
import com.delta.common.Result;
import com.delta.entity.Order;
import com.delta.mapper.UserMapper;
import com.delta.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * 客服管理后台控制器 — 所有接口都需要 cs 角色。
 * {@link #checkCs} 在每个方法开头调用，非 cs 角色直接返回 403。
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final OrderService orderService;
    private final UserMapper userMapper;

    public AdminController(OrderService orderService, UserMapper userMapper) {
        this.orderService = orderService;
        this.userMapper = userMapper;
    }

    /** 权限校验 — JWT 中的 role 必须为 cs（客服），否则抛 BusinessException → 403 */
    private void checkCs(HttpServletRequest r) {
        String role = (String) r.getAttribute("role");
        if (!"cs".equals(role)) throw new BusinessException(ErrorCode.FORBIDDEN, "仅客服可操作");
    }

    /** 全量订单列表 — 支持按状态和游戏筛选，分页返回 */
    @GetMapping("/orders")
    public Result<PageResult<Order>> orders(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String game,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            HttpServletRequest r) {
        checkCs(r);
        return Result.ok(orderService.findAllOrders(status, game, page, pageSize));
    }

    /**
     * 客服直接创建订单 — 跳过玩家下单流程，直接指定客户和打手。
     * 创建的订单状态为 assigned（已接单），不需要抢单。
     */
    @PostMapping("/orders")
    public Result<Order> createOrder(@Valid @RequestBody CreateOrderByCsReq req, HttpServletRequest r) {
        checkCs(r);
        Long csId = (Long) r.getAttribute("userId");
        return Result.ok(orderService.createByCs(
                req.getCustomerId(), req.getBoosterId(), csId, req.getGame(), req.getDetail(), req.getPrice()));
    }

    /** 派单 — 把待接单的订单分配给指定打手，跳过抢单环节 */
    @PostMapping("/orders/{id}/assign")
    public Result<Order> assign(@PathVariable Long id, @Valid @RequestBody AssignReq req, HttpServletRequest r) {
        checkCs(r);
        Long csId = (Long) r.getAttribute("userId");
        return Result.ok(orderService.assign(id, req.getBoosterId(), csId));
    }

    /** 获取所有打手列表（派单时下拉选择用） */
    @GetMapping("/boosters")
    public Result<?> boosters(HttpServletRequest r) {
        checkCs(r);
        return Result.ok(userMapper.findBoosters());
    }

    /** 用户列表 — 分页展示所有用户 */
    @GetMapping("/users")
    public Result<?> users(@RequestParam(defaultValue = "1") int page,
                           @RequestParam(defaultValue = "20") int pageSize,
                           HttpServletRequest r) {
        checkCs(r);
        int offset = (page - 1) * pageSize;
        return Result.ok(PageResult.of(userMapper.findAll(offset, pageSize), userMapper.count(), page, pageSize));
    }

    /** 修改用户角色 — 如把 player 升级为 booster */
    @PutMapping("/users/{id}/role")
    public Result<?> updateRole(@PathVariable Long id, @RequestParam String role, HttpServletRequest r) {
        checkCs(r);
        if (!"player".equals(role) && !"booster".equals(role) && !"cs".equals(role)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "角色必须是 player、booster 或 cs");
        }
        userMapper.updateRole(id, role);
        return Result.ok();
    }
}

/** 客服创建订单请求体 — 需要指定客户ID和打手ID */
class CreateOrderByCsReq {
    @NotNull private Long customerId;
    @NotNull private Long boosterId;
    @NotBlank private String game;
    private String detail;
    @NotNull private BigDecimal price;
    // getter/setter...
    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public Long getBoosterId() { return boosterId; }
    public void setBoosterId(Long boosterId) { this.boosterId = boosterId; }
    public String getGame() { return game; }
    public void setGame(String game) { this.game = game; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
}

/** 派单请求体 */
class AssignReq {
    @NotNull(message = "打手ID不能为空") private Long boosterId;
    public Long getBoosterId() { return boosterId; }
    public void setBoosterId(Long boosterId) { this.boosterId = boosterId; }
}
