package com.example.tradewise.service;

import com.example.tradewise.service.DataEngine.MarketRegime;
import com.example.tradewise.service.DataEngine.SignalState;
import com.example.tradewise.service.MarketAnalysisService.Candlestick;

import java.util.*;
import java.util.Map;
import java.util.Optional;

/**
 * 信号检测器接口 - 实现插件化信号检测
 * 
 * 这个接口允许添加新信号而不需要修改主流程
 */
public interface SignalDetector {
    
    /**
     * 信号类型枚举
     */
    enum SignalType {
        BOS_BREAKOUT,      // 结构突破
        FAKE_BREAKOUT,     // 假突破
        VOLATILITY_SQUEEZE, // 波动率挤压
        TREND_FOLLOWING,   // 趋势跟随
        REVERSAL         // 反转信号
    }
    
    /**
     * 获取信号类型
     */
    SignalType getType();
    
    /**
     * 检查信号是否允许在指定市场状态下运行
     */
    boolean allowedIn(MarketRegime regime);
    
    /**
     * 检测信号
     * 
     * @param ctx 市场上下文
     * @return 检测到的信号，如果未检测到则返回Optional.empty()
     */
    Optional<SignalDetectionResult> detect(MarketContext ctx);
    
    /**
     * 市场上下文
     */
    class MarketContext {
        private final String symbol;
        private final Map<String, List<Candlestick>> multiTimeframeData;
        private final MarketRegime currentRegime;
        private final SignalState signalState;
        
        public MarketContext(String symbol, Map<String, List<Candlestick>> multiTimeframeData, 
                           MarketRegime currentRegime, SignalState signalState) {
            this.symbol = symbol;
            this.multiTimeframeData = multiTimeframeData;
            this.currentRegime = currentRegime;
            this.signalState = signalState;
        }
        
        // Getters
        public String getSymbol() { return symbol; }
        public Map<String, List<Candlestick>> getMultiTimeframeData() { return multiTimeframeData; }
        public MarketRegime getCurrentRegime() { return currentRegime; }
        public SignalState getSignalState() { return signalState; }
    }
    
    /**
     * 信号检测结果
     */
    class SignalDetectionResult {
        private final SignalType type;
        private final String indicator;
        private final double strength; // 信号强度 (0-10)
        private final String reason; // 信号原因
        private final String suggestion; // 操作建议
        private final double price; // 触发价格
        private final Map<String, Object> explanation; // 信号解释信息
        
        public SignalDetectionResult(SignalType type, String indicator, double strength, 
                                   String reason, String suggestion, double price, 
                                   Map<String, Object> explanation) {
            this.type = type;
            this.indicator = indicator;
            this.strength = strength;
            this.reason = reason;
            this.suggestion = suggestion;
            this.price = price;
            this.explanation = explanation != null ? explanation : Collections.emptyMap();
        }
        
        // Getters
        public SignalType getType() { return type; }
        public String getIndicator() { return indicator; }
        public double getStrength() { return strength; }
        public String getReason() { return reason; }
        public String getSuggestion() { return suggestion; }
        public double getPrice() { return price; }
        public Map<String, Object> getExplanation() { return explanation; }
    }
}