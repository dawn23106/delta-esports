package com.delta.esports.controller;

import com.delta.esports.common.GlobalExceptionHandler.BusinessException;
import com.delta.esports.common.Result;
import com.delta.esports.config.RequireRole;
import com.delta.esports.dto.ChangePasswordRequest;
import com.delta.esports.service.UserService;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public Result<?> me(HttpServletRequest request) {
        return Result.success(userService.findById(userId(request)));
    }

    @GetMapping("/boosters")
    public Result<?> boosters(@RequestParam(defaultValue = "1") int page,
                              @RequestParam(defaultValue = "20") int size) {
        return Result.success(userService.boosterPage(page, size));
    }

    @RequireRole("booster")
    @PutMapping("/booster/status")
    public Result<?> updateBoosterStatus(HttpServletRequest request, @RequestParam String status) {
        userService.updateBoosterStatus(userId(request), status);
        return Result.success();
    }

    @PutMapping("/me/password")
    public Result<?> changePassword(HttpServletRequest request,
                                    @Valid @RequestBody ChangePasswordRequest body) {
        userService.changePassword(userId(request), body.getOldPassword(), body.getNewPassword());
        return Result.success();
    }

    private Long userId(HttpServletRequest request) {
        Long id = (Long) request.getAttribute("userId");
        if (id == null) throw new BusinessException(401, "未登录");
        return id;
    }
}
