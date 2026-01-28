package com.damien.tradewise.user.service;

import com.damien.tradewise.admin.entity.TwTraderConfig;
import com.damien.tradewise.admin.mapper.TwTraderConfigMapper;
import com.damien.tradewise.user.entity.TwUserTraderSubscription;
import com.damien.tradewise.user.mapper.TwUserSubscriptionMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TwUserSubscriptionService {
    
    @Autowired
    private TwUserSubscriptionMapper subscriptionMapper;
    
    @Autowired
    private TwTraderConfigMapper traderConfigMapper;
    
    /**
     * 获取用户的订阅列表
     */
    public List<Map<String, Object>> getUserSubscriptions(Long userId) {
        List<TwUserTraderSubscription> subscriptions = subscriptionMapper.findByUserId(userId);
        
        return subscriptions.stream().map(sub -> {
            TwTraderConfig trader = traderConfigMapper.findById(sub.getTraderId());
            Map<String, Object> map = new HashMap<>();
            map.put("id", sub.getId());
            map.put("traderId", sub.getTraderId());
            map.put("traderName", trader != null ? trader.getTraderName() : "未知");
            map.put("avatarUrl", trader != null ? trader.getAvatarUrl() : null);
            map.put("description", trader != null ? trader.getDescription() : null);
            map.put("totalOrders", trader != null ? trader.getTotalOrders() : 0);
            map.put("todayOrders", trader != null ? trader.getTodayOrders() : 0);
            map.put("notifyEmail", sub.getNotifyEmail());
            map.put("status", sub.getStatus());
            map.put("subscribedAt", sub.getSubscribedAt());
            return map;
        }).collect(Collectors.toList());
    }
    
    /**
     * 订阅交易员
     */
    @Transactional
    public void subscribe(Long userId, Long traderId) {
        // 检查是否已订阅
        if (subscriptionMapper.existsByUserIdAndTraderId(userId, traderId) > 0) {
            throw new RuntimeException("已经订阅过该交易员");
        }
        
        // 检查交易员是否存在且启用
        TwTraderConfig trader = traderConfigMapper.findById(traderId);
        if (trader == null || !trader.getEnabled()) {
            throw new RuntimeException("交易员不存在或未启用");
        }
        
        // 创建订阅
        TwUserTraderSubscription subscription = new TwUserTraderSubscription();
        subscription.setUserId(userId);
        subscription.setTraderId(traderId);
        subscription.setNotifyEmail(true);
        subscription.setAutoCopyTrade(false);
        subscription.setStatus("ACTIVE");
        
        subscriptionMapper.insert(subscription);
    }
    
    /**
     * 取消订阅
     */
    @Transactional
    public void unsubscribe(Long userId, Long traderId) {
        subscriptionMapper.deleteByUserIdAndTraderId(userId, traderId);
    }
    
    /**
     * 更新订阅配置
     */
    @Transactional
    public void updateSubscription(Long userId, Long traderId, Boolean notifyEmail, String status) {
        subscriptionMapper.updateConfig(userId, traderId, notifyEmail, status);
    }
    
    /**
     * 检查是否已订阅
     */
    public boolean isSubscribed(Long userId, Long traderId) {
        return subscriptionMapper.existsByUserIdAndTraderId(userId, traderId) > 0;
    }
}
