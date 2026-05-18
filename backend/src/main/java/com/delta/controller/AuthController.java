package com.delta.controller;

import com.delta.common.Result;
import com.delta.service.AuthService;
import com.delta.service.AuthService.TokenPair;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器 — 处理注册、登录、刷新Token。
 * 不走 JWT 拦截器，白名单放行（见 JwtAuthFilter.WHITELIST）。
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    // 构造器注入（比 @Autowired 字段注入更好，方便单元测试）
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /** 手机号注册，成功直接返回双Token免二次登录 */
    @PostMapping("/register")
    public Result<TokenPair> register(@Valid @RequestBody AuthRequest req) {
        return Result.ok(authService.register(req.getPhone(), req.getPassword()));
    }

    /** 手机号+密码登录 */
    @PostMapping("/login")
    public Result<TokenPair> login(@Valid @RequestBody AuthRequest req) {
        return Result.ok(authService.login(req.getPhone(), req.getPassword()));
    }

    /**
     * 刷新Token — AccessToken过期后用RefreshToken换新的。
     * RefreshToken会校验Redis中缓存的版本，如果不一致则拒绝（防盗用）。
     */
    @PostMapping("/refresh")
    public Result<TokenPair> refresh(@RequestBody RefreshRequest req) {
        return Result.ok(authService.refresh(req.getRefreshToken()));
    }
}

/** 登录/注册请求体 — 手机号+密码 */
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

/** 刷新Token请求体 */
class RefreshRequest {
    @NotBlank(message = "refreshToken不能为空")
    private String refreshToken;
    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
}
