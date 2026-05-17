package com.delta.controller;

import com.delta.common.Result;
import com.delta.entity.User;
import com.delta.mapper.UserMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserMapper userMapper;

    public UserController(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @GetMapping("/me")
    public Result<User> me(HttpServletRequest req) {
        Long userId = (Long) req.getAttribute("userId");
        return Result.ok(userMapper.findById(userId));
    }
}
