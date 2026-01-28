package com.damien.tradewise.user.controller;

import com.damien.tradewise.admin.entity.TwTraderConfig;
import com.damien.tradewise.admin.mapper.TwTraderConfigMapper;
import com.damien.tradewise.user.dto.TraderDisplayDTO;
import com.damien.tradewise.user.service.TwUserSubscriptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/user/subscriptions")
public class TwUserSubscriptionController {
    
    @Autowired
    private TwUserSubscriptionService subscriptionService;
    
    @Autowired
    private TwTraderConfigMapper traderConfigMapper;
    
    /**
     * 获取所有可订阅的交易员列表
     */
    @GetMapping("/traders")
    public Map<String, Object> getAvailableTraders(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpSession session) {
        
        Long userId = (Long) session.getAttribute("tw_user_id");
        
        int offset = (page - 1) * size;
        List<TwTraderConfig> traders = traderConfigMapper.selectEnabledTraders(offset, size);
        int total = traderConfigMapper.countEnabledTraders();
        
        // 转换为DTO并标记订阅状态
        List<TraderDisplayDTO> traderDTOs = traders.stream().map(trader -> {
            TraderDisplayDTO dto = new TraderDisplayDTO();
            dto.setId(trader.getId());
            dto.setTraderName(trader.getTraderName());
            dto.setAvatarUrl(trader.getAvatarUrl());
            dto.setDescription(trader.getDescription());
            dto.setTags(trader.getTags());
            dto.setTotalOrders(trader.getTotalOrders());
            dto.setTodayOrders(trader.getTodayOrders());
            dto.setLastOrderTime(trader.getLastOrderTime());
            
            if (userId != null) {
                dto.setSubscribed(subscriptionService.isSubscribed(userId, trader.getId()));
            } else {
                dto.setSubscribed(false);
            }
            
            return dto;
        }).collect(Collectors.toList());
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("traders", traderDTOs);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        result.put("totalPages", (int) Math.ceil((double) total / size));
        
        return result;
    }
    
    /**
     * 获取我的订阅列表
     */
    @GetMapping("/my")
    public Map<String, Object> getMySubscriptions(HttpSession session) {
        Long userId = (Long) session.getAttribute("tw_user_id");
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        
        if (userId == null) {
            result.put("subscriptions", new java.util.ArrayList<>());
        } else {
            List<Map<String, Object>> subscriptions = subscriptionService.getUserSubscriptions(userId);
            result.put("subscriptions", subscriptions);
        }
        
        return result;
    }
    
    /**
     * 订阅交易员
     */
    @PostMapping("/subscribe/{traderId}")
    public Map<String, Object> subscribe(@PathVariable Long traderId, HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        
        Long userId = (Long) session.getAttribute("tw_user_id");
        if (userId == null) {
            result.put("success", false);
            result.put("message", "请先登录");
            return result;
        }
        
        try {
            subscriptionService.subscribe(userId, traderId);
            result.put("success", true);
            result.put("message", "订阅成功");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 取消订阅
     */
    @DeleteMapping("/unsubscribe/{traderId}")
    public Map<String, Object> unsubscribe(@PathVariable Long traderId, HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        
        Long userId = (Long) session.getAttribute("tw_user_id");
        if (userId == null) {
            result.put("success", false);
            result.put("message", "请先登录");
            return result;
        }
        
        try {
            subscriptionService.unsubscribe(userId, traderId);
            result.put("success", true);
            result.put("message", "取消订阅成功");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 更新订阅配置
     */
    @PutMapping("/update/{traderId}")
    public Map<String, Object> updateSubscription(
            @PathVariable Long traderId,
            @RequestParam Boolean notifyEmail,
            @RequestParam String status,
            HttpSession session) {
        
        Map<String, Object> result = new HashMap<>();
        
        Long userId = (Long) session.getAttribute("tw_user_id");
        if (userId == null) {
            result.put("success", false);
            result.put("message", "请先登录");
            return result;
        }
        
        try {
            subscriptionService.updateSubscription(userId, traderId, notifyEmail, status);
            result.put("success", true);
            result.put("message", "更新成功");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        
        return result;
    }
}
