package com.example.tradewise.service;

import com.example.tradewise.service.MarketAnalysisService.TradingSignal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * 信号状态管理器
 * 为每个信号引入生命周期状态，减少噪音信号
 */
@Component
public class SignalStateManager {
    
    private static final Logger logger = LoggerFactory.getLogger(SignalStateManager.class);
    
    // 信号状态枚举
    public enum SignalState {
        SETUP,          // 设置状态 - 刚刚生成
        TRIGGERED,      // 已触发 - 信号条件满足
        CONFIRMED,      // 已确认 - 经过二次确认
        INVALIDATED,    // 已失效 - 被市场走势否定
        COOLDOWN        // 冷却中 - 避免重复信号
    }
    
    // 信号状态信息
    public static class SignalStatus {
        private SignalState state;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private LocalDateTime expirationTime; // 过期时间
        private String reason; // 状态变更原因
        
        public SignalStatus(SignalState state, String reason) {
            this.state = state;
            this.createdAt = LocalDateTime.now();
            this.updatedAt = LocalDateTime.now();
            this.reason = reason;
            // 默认信号有效期为4小时
            this.expirationTime = LocalDateTime.now().plusHours(4);
        }
        
        // Getters and setters
        public SignalState getState() { return state; }
        public void setState(SignalState state) { 
            this.state = state; 
            this.updatedAt = LocalDateTime.now();
        }
        
        public LocalDateTime getCreatedAt() { return createdAt; }
        public LocalDateTime getUpdatedAt() { return updatedAt; }
        public LocalDateTime getExpirationTime() { return expirationTime; }
        public void setExpirationTime(LocalDateTime expirationTime) { this.expirationTime = expirationTime; }
        
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
        
        public boolean isExpired() {
            return LocalDateTime.now().isAfter(expirationTime);
        }
    }
    
    // 存储信号状态的映射
    private final Map<String, SignalStatus> signalStates = new ConcurrentHashMap<>();
    
    /**
     * 获取信号的当前状态
     */
    public SignalStatus getSignalStatus(String signalId) {
        SignalStatus status = signalStates.get(signalId);
        if (status != null && status.isExpired()) {
            // 如果信号已过期，从状态管理器中移除
            signalStates.remove(signalId);
            logger.debug("信号 {} 已过期，从状态管理器中移除", signalId);
            return null;
        }
        return status;
    }
    
    /**
     * 设置信号状态
     */
    public void setSignalStatus(String signalId, SignalState state, String reason) {
        SignalStatus status = signalStates.get(signalId);
        if (status != null) {
            logger.debug("信号 {} 状态从 {} 变更为 {}，原因: {}", signalId, status.getState(), state, reason);
            status.setState(state);
            status.setReason(reason);
        } else {
            status = new SignalStatus(state, reason);
            signalStates.put(signalId, status);
            logger.debug("创建信号 {} 状态: {}，原因: {}", signalId, state, reason);
        }
    }
    
    /**
     * 根据交易信号对象获取唯一标识符
     */
    public String getSignalId(TradingSignal signal) {
        // 使用符号、指标、价格、时间戳等信息生成唯一标识
        return signal.getSymbol() + "_" + 
               signal.getIndicator() + "_" + 
               String.format("%.4f", signal.getPrice()) + "_" + 
               signal.getTimestamp().toString();
    }
    
    /**
     * 检查信号是否可以被处理（即状态是否允许）
     */
    public boolean isSignalProcessable(String signalId) {
        SignalStatus status = getSignalStatus(signalId);
        if (status == null) {
            // 如果没有状态记录，说明是新信号，可以处理
            return true;
        }
        
        // 只有在SETUP、TRIGGERED或CONFIRMED状态下的信号才可处理
        return status.getState() == SignalState.SETUP || 
               status.getState() == SignalState.TRIGGERED || 
               status.getState() == SignalState.CONFIRMED;
    }
    
    /**
     * 检查信号是否可以被处理（直接传入信号对象）
     */
    public boolean isSignalProcessable(TradingSignal signal) {
        return isSignalProcessable(getSignalId(signal));
    }
    
    /**
     * 更新信号状态为已确认
     */
    public void confirmSignal(TradingSignal signal) {
        String signalId = getSignalId(signal);
        setSignalStatus(signalId, SignalState.CONFIRMED, "经过二次确认");
        logger.info("信号 {} 已确认", signalId);
    }
    
    /**
     * 更新信号状态为已失效
     */
    public void invalidateSignal(TradingSignal signal) {
        String signalId = getSignalId(signal);
        setSignalStatus(signalId, SignalState.INVALIDATED, "被市场走势否定");
        logger.info("信号 {} 已失效", signalId);
    }
    
    /**
     * 将信号设置为冷却状态
     */
    public void cooldownSignal(TradingSignal signal, int hours) {
        String signalId = getSignalId(signal);
        SignalStatus status = getSignalStatus(signalId);
        if (status != null) {
            status.setExpirationTime(LocalDateTime.now().plusHours(hours));
        } else {
            status = new SignalStatus(SignalState.COOLDOWN, "进入冷却期");
            status.setExpirationTime(LocalDateTime.now().plusHours(hours));
            signalStates.put(signalId, status);
        }
        logger.info("信号 {} 进入冷却期，持续 {} 小时", signalId, hours);
    }
    
    /**
     * 获取信号状态的简要描述
     */
    public String getSignalStateDescription(TradingSignal signal) {
        SignalStatus status = getSignalStatus(getSignalId(signal));
        if (status == null) {
            return "新信号";
        }
        return String.format("%s (状态: %s, 原因: %s)", 
            status.getUpdatedAt().toString(), 
            status.getState().name(), 
            status.getReason());
    }
    
    /**
     * 清理过期的信号状态
     */
    public void cleanupExpiredSignals() {
        signalStates.entrySet().removeIf(entry -> entry.getValue().isExpired());
        logger.debug("清理过期信号状态，剩余 {} 个活跃信号", signalStates.size());
    }
}