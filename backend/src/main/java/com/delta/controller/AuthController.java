package com.delta.controller;

import com.delta.common.Result;
import com.delta.service.AuthService;
import com.delta.service.AuthService.TokenPair;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public Result<TokenPair> register(@Valid @RequestBody AuthRequest req) {
        return Result.ok(authService.register(req.getPhone(), req.getPassword()));
    }

    @PostMapping("/login")
    public Result<TokenPair> login(@Valid @RequestBody AuthRequest req) {
        return Result.ok(authService.login(req.getPhone(), req.getPassword()));
    }

    @PostMapping("/refresh")
    public Result<TokenPair> refresh(@RequestBody RefreshRequest req) {
        return Result.ok(authService.refresh(req.getRefreshToken()));
    }
}

class AuthRequest {
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式错误")
    private String phone;
    @NotBlank(message = "密码不能为空")
    private String password;
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}

class RefreshRequest {
    @NotBlank(message = "refreshToken不能为空")
    private String refreshToken;
    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
}
