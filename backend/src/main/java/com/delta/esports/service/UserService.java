package com.delta.esports.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.delta.esports.common.GlobalExceptionHandler.BusinessException;
import com.delta.esports.common.JwtUtils;
import com.delta.esports.common.PageResult;
import com.delta.esports.dto.LoginRequest;
import com.delta.esports.dto.LoginResponse;
import com.delta.esports.dto.RegisterRequest;
import com.delta.esports.dto.UserResponse;
import com.delta.esports.entity.User;
import com.delta.esports.mapper.UserMapper;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private JwtUtils jwtUtils;

    public LoginResponse login(LoginRequest req) {
        User user = findByPhone(req.getPhone());
        if (user == null) {
            throw new BusinessException(401, "账号或密码错误");
        }
        if (!BCrypt.checkpw(req.getPassword(), user.getPassword())) {
            throw new BusinessException(401, "账号或密码错误");
        }
        if ("banned".equals(user.getStatus())) {
            throw new BusinessException(403, "账号已被禁用");
        }
        return buildAuthResponse(user);
    }

    @Transactional
    public LoginResponse register(RegisterRequest req) {
        User exist = findByPhone(req.getPhone());
        if (exist != null) {
            throw new BusinessException("手机号已注册");
        }
        User user = new User();
        user.setPhone(req.getPhone());
        user.setPassword(BCrypt.hashpw(req.getPassword(), BCrypt.gensalt()));
        user.setNickname(req.getNickname() != null ? req.getNickname() : "用户" + req.getPhone().substring(7));
        String role = "booster".equals(req.getRole()) ? "booster" : "boss";
        user.setRole(role);
        user.setStatus("active");
        userMapper.insert(user);
        return buildAuthResponse(user);
    }

    public LoginResponse refreshToken(String refreshToken) {
        if (jwtUtils.isTokenExpired(refreshToken)) {
            throw new BusinessException(401, "refresh token 已过期，请重新登录");
        }
        Long userId = jwtUtils.getUserIdFromToken(refreshToken);
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(401, "用户不存在");
        }
        return buildAuthResponse(user);
    }

    public UserResponse findById(Long id) {
        User user = userMapper.selectById(id);
        return user != null ? UserResponse.from(user) : null;
    }

    public User findByPhone(String phone) {
        return userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getPhone, phone));
    }

    public PageResult<User> page(int page, int size, String role) {
        LambdaQueryWrapper<User> qw = new LambdaQueryWrapper<>();
        if (role != null && !role.isEmpty()) {
            qw.eq(User::getRole, role);
        }
        qw.orderByDesc(User::getCreatedAt);
        return PageResult.of(userMapper.selectPage(new Page<>(page, size), qw));
    }

    @Transactional
    public void updateStatus(Long id, String status) {
        User user = new User();
        user.setId(id);
        user.setStatus(status);
        userMapper.updateById(user);
    }

    private LoginResponse buildAuthResponse(User user) {
        return LoginResponse.builder()
                .accessToken(jwtUtils.generateToken(user.getId(), user.getRole()))
                .refreshToken(jwtUtils.generateRefreshToken(user.getId()))
                .userId(user.getId())
                .nickname(user.getNickname())
                .role(user.getRole())
                .phone(user.getPhone())
                .avatar(user.getAvatar())
                .balance(user.getBalance())
                .rating(user.getRating())
                .build();
    }
}
