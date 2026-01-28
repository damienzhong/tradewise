package com.damien.tradewise.user.dto;

import java.time.LocalDateTime;

/**
 * 交易员展示DTO（用于用户端）
 */
public class TraderDisplayDTO {
    private Long id;
    private String traderName;
    private String avatarUrl;
    private String description;
    private String tags;
    private Integer totalOrders;
    private Integer todayOrders;
    private LocalDateTime lastOrderTime;
    private Boolean subscribed;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getTraderName() { return traderName; }
    public void setTraderName(String traderName) { this.traderName = traderName; }
    
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
    
    public Integer getTotalOrders() { return totalOrders; }
    public void setTotalOrders(Integer totalOrders) { this.totalOrders = totalOrders; }
    
    public Integer getTodayOrders() { return todayOrders; }
    public void setTodayOrders(Integer todayOrders) { this.todayOrders = todayOrders; }
    
    public LocalDateTime getLastOrderTime() { return lastOrderTime; }
    public void setLastOrderTime(LocalDateTime lastOrderTime) { this.lastOrderTime = lastOrderTime; }
    
    public Boolean getSubscribed() { return subscribed; }
    public void setSubscribed(Boolean subscribed) { this.subscribed = subscribed; }
}
