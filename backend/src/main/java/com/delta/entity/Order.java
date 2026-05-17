package com.delta.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Order {
    private Long id;
    private String game;
    private String serviceType;     // tech/entertain/quest
    private String detail;
    private BigDecimal price;
    private String status;
    private Long customerId;
    private Long boosterId;
    private Long csId;
    private String sourceChannel;   // 获客渠道
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String customerNickname;
    private String boosterNickname;

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
