package com.delta.service;

import com.delta.common.BusinessException;
import com.delta.common.ErrorCode;
import com.delta.entity.User;
import com.delta.mapper.UserMapper;
import com.delta.security.JwtUtil;
import io.jsonwebtoken.Claims;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 认证服务 — 注册、登录、Token刷新、登出。
 *
 * JWT 双Token机制：
 *   AccessToken  15分钟，存前端内存，调接口时用
 *   RefreshToken 7天，存 Redis + 前端 localStorage，用于续期
 *
 * Redis 降级：如果 Redis 没启动，刷新和登出功能不可用，但不影响登录注册。
 */
@Service
public class AuthService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private StringRedisTemplate redis;

    public AuthService(UserMapper userMapper, JwtUtil jwtUtil) {
        this.userMapper = userMapper;
        this.jwtUtil = jwtUtil;
    }

    // required=false — Redis没启动时也能启动项目，redis字段为null
    @Autowired(required = false)
    public void setRedis(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /** 注册 — BCrypt加密密码（强度12），自动生成昵称"用户+后四位" */
    public TokenPair register(String phone, String password) {
        User exist = userMapper.findByPhone(phone);
        if (exist != null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "手机号已注册");
        }

        User user = new User();
        user.setPhone(phone);
        user.setPassword(BCrypt.hashpw(password, BCrypt.gensalt(12)));
        // BCrypt.gensalt(12): 强度参数，12表示2^12次哈希迭代，强度越高越安全但越慢
        user.setNickname("用户" + phone.substring(phone.length() - 4));
        user.setRole("player");
        userMapper.insert(user);

        return generateTokens(user.getId(), user.getRole());
    }

    /** 登录 — BCrypt验密，成功返回双Token */
    public TokenPair login(String phone, String password) {
        User user = userMapper.findByPhone(phone);
        if (user == null || !BCrypt.checkpw(password, user.getPassword())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "手机号或密码错误");
        }
        if (Boolean.FALSE.equals(user.getIsActive())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "账号已停用");
        }
        return generateTokens(user.getId(), user.getRole());
    }

    /**
     * 刷新Token — 用RefreshToken换新的双Token。
     * Redis版本校验：如果Redis中缓存的RefreshToken与传入不一致，说明Token被盗用或已过期。
     */
    public TokenPair refresh(String refreshToken) {
        try {
            Claims claims = jwtUtil.parseRefreshToken(refreshToken);
            Long userId = Long.parseLong(claims.getSubject());

            if (redis != null) {
                String cached = redis.opsForValue().get("refresh:" + userId);
                if (!refreshToken.equals(cached)) {
                    throw new BusinessException(ErrorCode.UNAUTHORIZED, "token已失效");
                }
            }
            return generateTokens(userId, (String) claims.get("role"));
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "token无效");
        }
    }

    /** 登出 — 删除Redis中的RefreshToken */
    public void logout(Long userId) {
        if (redis != null) {
            redis.delete("refresh:" + userId);
        }
    }

    /** 生成双Token并缓存RefreshToken到Redis */
    private TokenPair generateTokens(Long userId, String role) {
        String access = jwtUtil.generateAccessToken(userId, role);
        String refresh = jwtUtil.generateRefreshToken(userId, role);
        if (redis != null) {
            redis.opsForValue().set("refresh:" + userId, refresh, Duration.ofDays(7));
        }
        return new TokenPair(access, refresh);
    }

    /** 双Token内部类 — AccessToken短时效用于接口调用，RefreshToken长时效用于续期 */
    public static class TokenPair {
        private final String accessToken;
        private final String refreshToken;
        public TokenPair(String accessToken, String refreshToken) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
        }
        public String getAccessToken() { return accessToken; }
        public String getRefreshToken() { return refreshToken; }
    }
}
