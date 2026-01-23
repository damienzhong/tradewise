package com.example.tradewise.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 经济日历过滤器
 * 用于在重大经济事件期间暂停交易信号，避免在高波动时期产生误导
 */
@Component
public class EconomicCalendarFilter {
    
    private static final Logger logger = LoggerFactory.getLogger(EconomicCalendarFilter.class);
    
    // 存储经济事件数据（实际应用中可以从外部API获取）
    private final Map<String, List<EconomicEvent>> economicEvents = new ConcurrentHashMap<>();
    
    public EconomicCalendarFilter() {
        // 初始化一些常见的高影响经济事件（示例数据）
        initializeSampleEvents();
    }
    
    /**
     * 检查当前时间是否适合交易
     * 在重大经济事件前后30分钟内暂停交易
     */
    public boolean isSafeToTrade(String symbol, LocalDateTime currentTime) {
        // 获取该交易对对应的货币对（如BTCUSDT -> BTC）
        String baseCurrency = symbol.contains("USDT") ? 
            symbol.replace("USDT", "") : 
            symbol.contains("USD") ? symbol.replace("USD", "") : symbol;
        
        List<EconomicEvent> events = economicEvents.getOrDefault(baseCurrency, new ArrayList<>());
        
        if (events.isEmpty()) {
            return true; // 没有相关经济事件，可以交易
        }
        
        LocalDateTime thirtyMinsBefore = currentTime.plusMinutes(30);
        LocalDateTime thirtyMinsAfter = currentTime.minusMinutes(30);
        
        for (EconomicEvent event : events) {
            // 检查事件是否在敏感时间段内
            if (event.getTime().isAfter(thirtyMinsBefore) && 
                event.getTime().isBefore(thirtyMinsAfter) &&
                event.getImpact() == Impact.HIGH) {
                
                logger.warn("检测到高影响经济事件，暂停交易：{} - {}", 
                    event.getEventName(), event.getTime());
                return false; // 高影响事件期间暂停交易
            }
        }
        
        return true;
    }
    
    /**
     * 获取当前时间附近的经济事件警告
     */
    public List<EconomicEvent> getUpcomingEvents(String symbol, LocalDateTime currentTime) {
        String baseCurrency = symbol.contains("USDT") ? 
            symbol.replace("USDT", "") : 
            symbol.contains("USD") ? symbol.replace("USD", "") : symbol;
        
        List<EconomicEvent> events = economicEvents.getOrDefault(baseCurrency, new ArrayList<>());
        List<EconomicEvent> upcomingEvents = new ArrayList<>();
        
        LocalDateTime sixtyMinsBefore = currentTime.plusMinutes(60);
        LocalDateTime sixtyMinsAfter = currentTime.minusMinutes(60);
        
        for (EconomicEvent event : events) {
            if (event.getTime().isAfter(sixtyMinsBefore) && 
                event.getTime().isBefore(sixtyMinsAfter)) {
                upcomingEvents.add(event);
            }
        }
        
        return upcomingEvents;
    }
    
    /**
     * 初始化示例经济事件数据
     * 在实际应用中，这部分应该从外部API获取
     */
    private void initializeSampleEvents() {
        // 示例：添加一些常见经济事件
        List<EconomicEvent> btcEvents = new ArrayList<>();
        
        // 这里是示例数据，实际应用中应从经济日历API获取
        // 比如：CPI、非农就业数据、FOMC会议等
        EconomicEvent exampleEvent = new EconomicEvent(
            LocalDateTime.now().plusHours(1), // 1小时后
            "Non-Farm Payrolls", 
            Impact.HIGH
        );
        btcEvents.add(exampleEvent);
        
        economicEvents.put("BTC", btcEvents);
        economicEvents.put("ETH", btcEvents); // 对于加密货币，可能受相同宏观经济影响
        economicEvents.put("BNB", btcEvents);
        economicEvents.put("SOL", btcEvents);
        
        logger.info("已初始化经济日历过滤器，包含 {} 个货币对的经济事件", economicEvents.size());
    }
    
    /**
     * 添加经济事件
     */
    public void addEconomicEvent(String currency, EconomicEvent event) {
        economicEvents.computeIfAbsent(currency, k -> new ArrayList<>()).add(event);
        logger.debug("添加经济事件: {} for {} at {}", event.getEventName(), currency, event.getTime());
    }
    
    /**
     * 清除指定货币对的经济事件
     */
    public void clearEvents(String currency) {
        economicEvents.remove(currency);
        logger.debug("清除 {} 的经济事件", currency);
    }
    
    /**
     * 经济事件实体
     */
    public static class EconomicEvent {
        private LocalDateTime time;
        private String eventName;
        private Impact impact;
        
        public EconomicEvent(LocalDateTime time, String eventName, Impact impact) {
            this.time = time;
            this.eventName = eventName;
            this.impact = impact;
        }
        
        // Getters and setters
        public LocalDateTime getTime() {
            return time;
        }
        
        public void setTime(LocalDateTime time) {
            this.time = time;
        }
        
        public String getEventName() {
            return eventName;
        }
        
        public void setEventName(String eventName) {
            this.eventName = eventName;
        }
        
        public Impact getImpact() {
            return impact;
        }
        
        public void setImpact(Impact impact) {
            this.impact = impact;
        }
    }
    
    /**
     * 影响程度枚举
     */
    public enum Impact {
        LOW,     // 低影响
        MEDIUM,  // 中等影响
        HIGH     // 高影响
    }
}