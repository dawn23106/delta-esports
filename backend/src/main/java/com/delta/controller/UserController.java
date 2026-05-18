package com.delta.controller;

import com.delta.common.Result;
import com.delta.entity.User;
import com.delta.mapper.UserMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

/**
 * 用户控制器 — 获取当前用户信息。
 * userId 从 JWT 中解析，不需要前端传入，防止越权查询。
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserMapper userMapper;

    public UserController(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    /** 获取当前登录用户信息（前端顶部栏显示昵称、角色等） */
    @GetMapping("/me")
    public Result<User> me(HttpServletRequest req) {
        Long userId = (Long) req.getAttribute("userId");
        return Result.ok(userMapper.findById(userId));
    }
}
