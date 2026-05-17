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

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final OrderService orderService;
    private final UserMapper userMapper;

    public AdminController(OrderService orderService, UserMapper userMapper) {
        this.orderService = orderService;
        this.userMapper = userMapper;
    }

    private void checkCs(HttpServletRequest r) {
        String role = (String) r.getAttribute("role");
        if (!"cs".equals(role)) throw new BusinessException(ErrorCode.FORBIDDEN, "仅客服可操作");
    }

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

    @PostMapping("/orders")
    public Result<Order> createOrder(@Valid @RequestBody CreateOrderByCsReq req, HttpServletRequest r) {
        checkCs(r);
        Long csId = (Long) r.getAttribute("userId");
        return Result.ok(orderService.createByCs(
                req.getCustomerId(), req.getBoosterId(), csId, req.getGame(), req.getDetail(), req.getPrice()));
    }

    @PostMapping("/orders/{id}/assign")
    public Result<Order> assign(@PathVariable Long id, @Valid @RequestBody AssignReq req, HttpServletRequest r) {
        checkCs(r);
        Long csId = (Long) r.getAttribute("userId");
        return Result.ok(orderService.assign(id, req.getBoosterId(), csId));
    }

    @GetMapping("/boosters")
    public Result<?> boosters() { return Result.ok(userMapper.findBoosters()); }

    @GetMapping("/users")
    public Result<?> users(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int pageSize) {
        int offset = (page - 1) * pageSize;
        return Result.ok(PageResult.of(userMapper.findAll(offset, pageSize), userMapper.count(), page, pageSize));
    }

    @PutMapping("/users/{id}/role")
    public Result<?> updateRole(@PathVariable Long id, @RequestParam String role, HttpServletRequest r) {
        checkCs(r);
        userMapper.updateRole(id, role);
        return Result.ok();
    }
}

class CreateOrderByCsReq {
    @NotNull private Long customerId;
    @NotNull private Long boosterId;
    @NotBlank private String game;
    private String detail;
    @NotNull private BigDecimal price;
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

class AssignReq {
    @NotNull(message = "打手ID不能为空") private Long boosterId;
    public Long getBoosterId() { return boosterId; }
    public void setBoosterId(Long boosterId) { this.boosterId = boosterId; }
}
