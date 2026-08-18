package com.delta.esports.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.delta.esports.common.GlobalExceptionHandler.BusinessException;
import com.delta.esports.common.JwtUtils;
import com.delta.esports.common.PageResult;
import com.delta.esports.common.PageSupport;
import com.delta.esports.config.PaymentProperties;
import com.delta.esports.dto.LoginRequest;
import com.delta.esports.dto.LoginResponse;
import com.delta.esports.dto.RegisterRequest;
import com.delta.esports.dto.UserResponse;
import com.delta.esports.dto.BoosterSummaryResponse;
import com.delta.esports.entity.User;
import com.delta.esports.mapper.UserMapper;
import com.delta.esports.mapper.OrderMapper;
import com.delta.esports.entity.Order;
import com.delta.esports.payment.WeChatMiniProgramClient;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.UUID;

@Service
public class UserService {

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private JwtUtils jwtUtils;
    @Autowired
    private WeChatMiniProgramClient weChatMiniProgramClient;
    @Autowired
    private PaymentProperties paymentProperties;
    @Autowired
    private OrderMapper orderMapper;

    private static final Set<String> BOOSTER_STATUSES = Set.of("idle", "busy", "offline");

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
        if (jwtUtils.isTokenExpired(refreshToken) || !jwtUtils.isRefreshToken(refreshToken)) {
            throw new BusinessException(401, "refresh token 已过期，请重新登录");
        }
        Long userId = jwtUtils.getUserIdFromToken(refreshToken);
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(401, "用户不存在");
        }
        if ("banned".equals(user.getStatus())) {
            throw new BusinessException(403, "账号已被禁用");
        }
        return buildAuthResponse(user);
    }

    public LoginResponse wxLogin(String code) {
        // 本地模拟支付保留固定演示账号；生产环境必须使用微信返回的真实 openid。
        String openId = paymentProperties.isMockEnabled()
                ? "wx_demo_openid"
                : weChatMiniProgramClient.exchangeCodeForOpenId(code);
        User user = findByOpenId(openId);
        if (user == null) {
            user = new User();
            user.setOpenId(openId);
            user.setPhone(pseudoPhone(openId));
            user.setPassword(BCrypt.hashpw(UUID.randomUUID().toString(), BCrypt.gensalt()));
            user.setNickname("微信用户");
            user.setRole("boss");
            user.setStatus("active");
            try {
                userMapper.insert(user);
            } catch (DuplicateKeyException duplicate) {
                // 同一个微信用户并发首次登录时，唯一索引只允许创建一个账号。
                user = findByOpenId(openId);
                if (user == null) throw duplicate;
            }
        }
        if ("banned".equals(user.getStatus())) throw new BusinessException(403, "账号已被禁用");
        return buildAuthResponse(user);
    }

    public UserResponse findById(Long id) {
        User user = userMapper.selectById(id);
        return user != null ? UserResponse.from(user) : null;
    }

    public User findByPhone(String phone) {
        return userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getPhone, phone));
    }

    public User findByOpenId(String openId) {
        return userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getOpenId, openId));
    }

    public PageResult<UserResponse> page(int page, int size, String role) {
        LambdaQueryWrapper<User> qw = new LambdaQueryWrapper<>();
        if (role != null && !role.isEmpty()) {
            qw.eq(User::getRole, role);
        }
        qw.orderByDesc(User::getCreatedAt);
        Page<User> users = userMapper.selectPage(PageSupport.of(page, size), qw);
        PageResult<UserResponse> result = new PageResult<>();
        result.setRecords(users.getRecords().stream().map(UserResponse::from).collect(Collectors.toList()));
        result.setTotal(users.getTotal());
        result.setPage(users.getCurrent());
        result.setSize(users.getSize());
        result.setPages(users.getPages());
        return result;
    }

    public PageResult<BoosterSummaryResponse> boosterPage(int page, int size) {
        Page<User> users = userMapper.selectPage(
                PageSupport.of(page, size),
                new LambdaQueryWrapper<User>()
                        .eq(User::getRole, "booster")
                        .eq(User::getStatus, "active")
                        .orderByDesc(User::getRating));
        PageResult<BoosterSummaryResponse> result = new PageResult<>();
        result.setRecords(users.getRecords().stream()
                .map(BoosterSummaryResponse::from).collect(Collectors.toList()));
        result.setTotal(users.getTotal());
        result.setPage(users.getCurrent());
        result.setSize(users.getSize());
        result.setPages(users.getPages());
        return result;
    }

    @Transactional
    public void updateStatus(Long id, String status) {
        User user = new User();
        user.setId(id);
        user.setStatus(status);
        userMapper.updateById(user);
    }

    @Transactional
    public void updateBoosterStatus(Long id, String status) {
        if (!BOOSTER_STATUSES.contains(status)) throw new BusinessException(400, "接单状态不合法");
        User current = userMapper.selectById(id);
        if (current == null || !"booster".equals(current.getRole())) {
            throw new BusinessException(403, "仅陪陪可以修改接单状态");
        }
        if ("idle".equals(status)) {
            Long activeOrders = orderMapper.selectCount(new LambdaQueryWrapper<Order>()
                    .eq(Order::getBoosterId, id)
                    .in(Order::getStatus, "assigned", "in_progress", "submitted"));
            if (activeOrders > 0) throw new BusinessException(409, "存在进行中的订单，不能切换为空闲");
        }
        User update = new User();
        update.setId(id);
        update.setBoosterStatus(status);
        userMapper.updateById(update);
    }

    @Transactional
    public void changePassword(Long id, String oldPassword, String newPassword) {
        User user = userMapper.selectById(id);
        if (user == null) throw new BusinessException(404, "用户不存在");
        if (!BCrypt.checkpw(oldPassword, user.getPassword())) {
            throw new BusinessException(400, "原密码不正确");
        }
        User update = new User();
        update.setId(id);
        update.setPassword(BCrypt.hashpw(newPassword, BCrypt.gensalt()));
        userMapper.updateById(update);
    }

    private String pseudoPhone(String openId) {
        String stable = UUID.nameUUIDFromBytes(openId.getBytes(StandardCharsets.UTF_8))
                .toString().replace("-", "");
        return "wx_" + stable.substring(0, 17);
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
