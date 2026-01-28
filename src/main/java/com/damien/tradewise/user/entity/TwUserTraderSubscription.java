package com.damien.tradewise.user.entity;

import java.time.LocalDateTime;

/**
 * 用户订阅交易员实体类
 */
public class TwUserTraderSubscription {
    private Long id;
    private Long userId;
    private Long traderId;
    private Boolean notifyEmail;
    private Boolean autoCopyTrade;
    private String status;
    private LocalDateTime subscribedAt;
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getTraderId() {
        return traderId;
    }

    public void setTraderId(Long traderId) {
        this.traderId = traderId;
    }

    public Boolean getNotifyEmail() {
        return notifyEmail;
    }

    public void setNotifyEmail(Boolean notifyEmail) {
        this.notifyEmail = notifyEmail;
    }

    public Boolean getAutoCopyTrade() {
        return autoCopyTrade;
    }

    public void setAutoCopyTrade(Boolean autoCopyTrade) {
        this.autoCopyTrade = autoCopyTrade;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getSubscribedAt() {
        return subscribedAt;
    }

    public void setSubscribedAt(LocalDateTime subscribedAt) {
        this.subscribedAt = subscribedAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
