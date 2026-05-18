package com.delta.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户实体 — 对应数据库 users 表。
 * 实体类只做数据承载，不写业务逻辑（贫血模型）。
 *
 * 角色(role) 说明：
 *   player  — 玩家（注册默认角色），可下单
 *   booster — 打手，可抢单/开始/完成
 *   cs      — 客服，系统预置，可管理所有订单、派单、修改用户角色
 */
public class User {
    private Long id;
    private String phone;            // 手机号（登录凭证）
    private String password;         // BCrypt加密后的密码
    private String nickname;         // 昵称
    private String role;             // player / booster / cs
    private BigDecimal balance;      // 账户余额
    private Boolean isActive;        // 是否启用
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
