package com.delta.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单实体 — 对应数据库 orders 表。
 *
 * 状态(status) 流转：
 *   pending     — 待接单（玩家下单后的初始状态）
 *   assigned    — 已接单（打手抢到或客服派单）
 *   in_progress — 进行中（打手点开始）
 *   completed   — 已完成（打手点完成）
 *   cancelled   — 已取消（玩家或客服取消）
 *
 * 服务类型(serviceType)：
 *   tech      — 技术代练（上分、竞技等）
 *   entertain — 娱乐陪玩
 *   quest     — 任务代练（刷任务、打材料）
 */
public class Order {
    private Long id;
    private String game;             // 游戏名（如"三角洲行动"）
    private String serviceType;      // tech / entertain / quest
    private String detail;           // 订单描述
    private BigDecimal price;        // 价格
    private String status;           // pending / assigned / in_progress / completed / cancelled
    private Long customerId;         // 下单玩家ID
    private Long boosterId;          // 接单打手ID（初始null）
    private Long csId;               // 操作客服ID（客服创建/派单时记录）
    private String sourceChannel;    // 获客渠道（web_h5、wechat_share等）
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 以下字段不在订单表中，是连表查询时填充的冗余字段（用于前端展示）
    private String customerNickname; // 玩家昵称
    private String boosterNickname;  // 打手昵称

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getGame() { return game; }
    public void setGame(String game) { this.game = game; }
    public String getServiceType() { return serviceType; }
    public void setServiceType(String serviceType) { this.serviceType = serviceType; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public Long getBoosterId() { return boosterId; }
    public void setBoosterId(Long boosterId) { this.boosterId = boosterId; }
    public Long getCsId() { return csId; }
    public void setCsId(Long csId) { this.csId = csId; }
    public String getSourceChannel() { return sourceChannel; }
    public void setSourceChannel(String sourceChannel) { this.sourceChannel = sourceChannel; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public String getCustomerNickname() { return customerNickname; }
    public void setCustomerNickname(String customerNickname) { this.customerNickname = customerNickname; }
    public String getBoosterNickname() { return boosterNickname; }
    public void setBoosterNickname(String boosterNickname) { this.boosterNickname = boosterNickname; }
}
