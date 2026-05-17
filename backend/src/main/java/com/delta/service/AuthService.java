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

@Service
public class AuthService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private StringRedisTemplate redis;

    public AuthService(UserMapper userMapper, JwtUtil jwtUtil) {
        this.userMapper = userMapper;
        this.jwtUtil = jwtUtil;
    }

    @Autowired(required = false)
    public void setRedis(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public TokenPair register(String phone, String password) {
        User exist = userMapper.findByPhone(phone);
        if (exist != null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "手机号已注册");
        }

        User user = new User();
        user.setPhone(phone);
        user.setPassword(BCrypt.hashpw(password, BCrypt.gensalt(12)));
        user.setNickname("用户" + phone.substring(phone.length() - 4));
        user.setRole("player");
        userMapper.insert(user);

        return generateTokens(user.getId(), user.getRole());
    }

    public TokenPair login(String phone, String password) {
        User user = userMapper.findByPhone(phone);
        if (user == null || !BCrypt.checkpw(password, user.getPassword())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "手机号或密码错误");
        }
        return generateTokens(user.getId(), user.getRole());
    }

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

    public void logout(Long userId) {
        if (redis != null) {
            redis.delete("refresh:" + userId);
        }
    }

    private TokenPair generateTokens(Long userId, String role) {
        String access = jwtUtil.generateAccessToken(userId, role);
        String refresh = jwtUtil.generateRefreshToken(userId, role);
        if (redis != null) {
            redis.opsForValue().set("refresh:" + userId, refresh, Duration.ofDays(7));
        }
        return new TokenPair(access, refresh);
    }

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
