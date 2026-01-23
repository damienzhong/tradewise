package com.example.tradewise.service;

import com.example.tradewise.service.DataEngine.MarketRegime;
import com.example.tradewise.service.MarketAnalysisService.Candlestick;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 智能信号融合引擎
 * 实现DeepSeek六维智能合约交易系统的信号融合思想
 */
public class SignalFusionEngine {
    
    private static final Logger logger = LoggerFactory.getLogger(SignalFusionEngine.class);
    
    /**
     * 信号类型枚举
     */
    public enum SignalModel {
        TREND_MOMENTUM_RESONANCE,    // 趋势动量共振模型
        INSTITUTIONAL_FLOW,          // 机构资金流向模型
        VOLATILITY_BREAKOUT,         // 波动率结构突破模型
        KEY_LEVEL_BATTLEGROUNDS,     // 关键位置博弈模型
        EXTREME_SENTIMENT,           // 市场情绪极端模型
        CORRELATION_ARBITRAGE        // 相关性套利模型
    }
    
    /**
     * 信号决策
     */
    public enum Decision {
        LONG, SHORT, NO_TRADE
    }
    
    /**
     * 交易信号
     */
    public static class TradingSignal {
        private final SignalModel model;
        private final Decision direction;
        private final double strength; // 信号强度 (0-10)
        private final String reason; // 信号原因
        private final double confidence; // 置信度
        private final Map<String, Object> metadata; // 元数据
        
        public TradingSignal(SignalModel model, Decision direction, double strength, 
                           String reason, double confidence, Map<String, Object> metadata) {
            this.model = model;
            this.direction = direction;
            this.strength = strength;
            this.reason = reason;
            this.confidence = confidence;
            this.metadata = metadata != null ? metadata : new HashMap<>();
        }
        
        // Getters
        public SignalModel getModel() { return model; }
        public Decision getDirection() { return direction; }
        public double getStrength() { return strength; }
        public String getReason() { return reason; }
        public double getConfidence() { return confidence; }
        public Map<String, Object> getMetadata() { return metadata; }
    }
    
    /**
     * 最终决策结果
     */
    public static class FusionResult {
        private final Decision finalDecision;
        private final double aggregatedStrength;
        private final double confidence;
        private final String reasoning;
        private final List<TradingSignal> contributingSignals;
        private final double positionSize;
        private final double stopLoss;
        private final double takeProfit;
        
        public FusionResult(Decision finalDecision, double aggregatedStrength, double confidence, 
                          String reasoning, List<TradingSignal> contributingSignals, 
                          double positionSize, double stopLoss, double takeProfit) {
            this.finalDecision = finalDecision;
            this.aggregatedStrength = aggregatedStrength;
            this.confidence = confidence;
            this.reasoning = reasoning;
            this.contributingSignals = contributingSignals;
            this.positionSize = positionSize;
            this.stopLoss = stopLoss;
            this.takeProfit = takeProfit;
        }
        
        // Getters
        public Decision getFinalDecision() { return finalDecision; }
        public double getAggregatedStrength() { return aggregatedStrength; }
        public double getConfidence() { return confidence; }
        public String getReasoning() { return reasoning; }
        public List<TradingSignal> getContributingSignals() { return contributingSignals; }
        public double getPositionSize() { return positionSize; }
        public double getStopLoss() { return stopLoss; }
        public double getTakeProfit() { return takeProfit; }
    }
    
    /**
     * 智能信号融合算法
     */
    public FusionResult fuseSignals(List<TradingSignal> signals, MarketRegime currentRegime, 
                                  String symbol, List<Candlestick> candlesticks) {
        if (signals == null || signals.isEmpty()) {
            return new FusionResult(Decision.NO_TRADE, 0, 0, "没有有效的信号输入", new ArrayList<>(), 0, 0, 0);
        }
        
        // Step 1: 信号聚类 - 按方向聚类
        List<TradingSignal> longSignals = signals.stream()
                .filter(s -> s.getDirection() == Decision.LONG)
                .collect(Collectors.toList());
        
        List<TradingSignal> shortSignals = signals.stream()
                .filter(s -> s.getDirection() == Decision.SHORT)
                .collect(Collectors.toList());
        
        // Step 2: 强度加权计算
        double longStrength = longSignals.stream()
                .mapToDouble(s -> s.getStrength() * s.getConfidence())
                .sum();
        
        double shortStrength = shortSignals.stream()
                .mapToDouble(s -> s.getStrength() * s.getConfidence())
                .sum();
        
        // Step 3: 置信度计算
        int totalSignals = signals.size();
        double confidence = Math.min(1.0, (longSignals.size() + shortSignals.size()) / 6.0 * 0.5);
        
        // Step 4: 市场状态调整
        longStrength = adjustForMarketRegime(longStrength, Decision.LONG, currentRegime);
        shortStrength = adjustForMarketRegime(shortStrength, Decision.SHORT, currentRegime);
        
        // Step 5: 最终决策
        Decision finalDecision;
        String reasoning;
        List<TradingSignal> contributingSignals;
        
        if (longStrength - shortStrength > 2.0) { // 阈值可配置
            finalDecision = Decision.LONG;
            reasoning = String.format("多头信号占优: 强度 %.2f vs %.2f", longStrength, shortStrength);
            contributingSignals = longSignals;
        } else if (shortStrength - longStrength > 2.0) {
            finalDecision = Decision.SHORT;
            reasoning = String.format("空头信号占优: 强度 %.2f vs %.2f", shortStrength, longStrength);
            contributingSignals = shortSignals;
        } else {
            finalDecision = Decision.NO_TRADE;
            reasoning = String.format("信号方向不明确: 多头强度 %.2f, 空头强度 %.2f", longStrength, shortStrength);
            contributingSignals = new ArrayList<>();
        }
        
        // 计算仓位大小和风险管理参数
        double positionSize = 0;
        double stopLoss = 0;
        double takeProfit = 0;
        
        if (finalDecision != Decision.NO_TRADE && !contributingSignals.isEmpty()) {
            // 使用动态风险管理计算
            double aggregatedStrength = finalDecision == Decision.LONG ? longStrength : shortStrength;
            DynamicRiskManager riskManager = new DynamicRiskManager();
            positionSize = riskManager.calculatePosition(contributingSignals, currentRegime, candlesticks);
            
            // 计算止损止盈
            if (!candlesticks.isEmpty()) {
                double currentPrice = candlesticks.get(candlesticks.size() - 1).getClose();
                double atr = calculateATR(candlesticks);
                
                if (finalDecision == Decision.LONG) {
                    stopLoss = currentPrice - (atr * 1.5); // 默认止损
                    takeProfit = currentPrice + (atr * 2.0 * (aggregatedStrength / 10.0)); // 根据强度调整止盈
                } else {
                    stopLoss = currentPrice + (atr * 1.5); // 默认止损
                    takeProfit = currentPrice - (atr * 2.0 * (aggregatedStrength / 10.0)); // 根据强度调整止盈
                }
            }
        }
        
        logger.info("信号融合结果: 决策={}, 强度={}, 置信度={}, 原因={}", 
                   finalDecision, Math.abs(longStrength - shortStrength), confidence, reasoning);
        
        return new FusionResult(finalDecision, Math.abs(longStrength - shortStrength), 
                              confidence, reasoning, contributingSignals, positionSize, stopLoss, takeProfit);
    }
    
    /**
     * 根据市场状态调整信号强度
     */
    private double adjustForMarketRegime(double strength, Decision direction, MarketRegime regime) {
        switch (regime) {
            case STRONG_TREND:
                // 在强趋势中，顺应趋势的信号强度增加
                if ((direction == Decision.LONG && isBullishRegime(regime)) || 
                    (direction == Decision.SHORT && !isBullishRegime(regime))) {
                    return strength * 1.5;
                }
                break;
            case WEAK_TREND:
                // 在弱趋势中，信号强度轻微增加
                return strength * 1.2;
            case RANGE:
                // 在震荡市场中，突破信号强度降低，反转信号强度增加
                if (direction == Decision.LONG || direction == Decision.SHORT) {
                    return strength * 0.8; // 降低突破信号
                }
                break;
            case VOLATILITY_EXPAND:
                // 在波动扩张中，趋势信号更可靠
                return strength * 1.3;
            case VOLATILITY_SQUEEZE:
                // 在波动收缩中，突破信号更有价值
                return strength * 1.4;
        }
        return strength;
    }
    
    /**
     * 判断是否为看涨市场状态
     */
    private boolean isBullishRegime(MarketRegime regime) {
        // 这里简化实现，实际应用中需要更复杂的判断
        return regime == MarketRegime.STRONG_TREND || regime == MarketRegime.WEAK_TREND;
    }
    
    /**
     * 计算ATR
     */
    private double calculateATR(List<Candlestick> candlesticks) {
        if (candlesticks.size() < 14) {
            return 0.0;
        }
        
        // 简化的ATR计算
        double atrSum = 0;
        for (int i = 1; i < Math.min(14, candlesticks.size()); i++) {
            Candlestick prev = candlesticks.get(i - 1);
            Candlestick curr = candlesticks.get(i);
            
            double highLow = curr.getHigh() - curr.getLow();
            double highPrevClose = Math.abs(curr.getHigh() - prev.getClose());
            double lowPrevClose = Math.abs(curr.getLow() - prev.getClose());
            
            double tr = Math.max(Math.max(highLow, highPrevClose), lowPrevClose);
            atrSum += tr;
        }
        
        return atrSum / Math.min(14, candlesticks.size() - 1);
    }
    
    /**
     * 动态风险管理系统
     */
    public static class DynamicRiskManager {
        
        /**
         * 智能仓位计算
         */
        public double calculatePosition(List<TradingSignal> signals, MarketRegime currentRegime, 
                                      List<Candlestick> candlesticks) {
            if (candlesticks.isEmpty()) {
                return 0.0;
            }
            
            double currentPrice = candlesticks.get(candlesticks.size() - 1).getClose();
            
            // 基础风险：账户的2%
            double baseRisk = 0.02; // 2%的风险
            
            // 信号强度调整
            double avgStrength = signals.stream()
                    .mapToDouble(TradingSignal::getStrength)
                    .average()
                    .orElse(0);
            double strengthCoefficient = avgStrength / 10.0; // 0-1
            
            // 市场波动率调整
            double atr = calculateATR(candlesticks);
            double baselineAtr = 0.02 * currentPrice; // 假设基准ATR为价格的2%
            double volatilityCoefficient = Math.min(1.0, baselineAtr / atr);
            
            // 相关性调整（简化实现）
            double correlationCoefficient = 1.0 / (1 + 0); // 没有持仓相关性，系数为1
            
            // 近期表现调整（简化实现）
            double performanceCoefficient = 1.0; // 假设近期表现良好
            
            // 最终仓位计算
            double finalPosition = baseRisk * strengthCoefficient * volatilityCoefficient * 
                                 correlationCoefficient * performanceCoefficient;
            
            // 硬性限制：最大仓位不超过账户的5%
            return Math.min(finalPosition, 0.05);
        }
        
        private double calculateATR(List<Candlestick> candlesticks) {
            if (candlesticks.size() < 14) {
                return 0.0;
            }
            
            // 简化的ATR计算
            double atrSum = 0;
            for (int i = 1; i < Math.min(14, candlesticks.size()); i++) {
                Candlestick prev = candlesticks.get(i - 1);
                Candlestick curr = candlesticks.get(i);
                
                double highLow = curr.getHigh() - curr.getLow();
                double highPrevClose = Math.abs(curr.getHigh() - prev.getClose());
                double lowPrevClose = Math.abs(curr.getLow() - prev.getClose());
                
                double tr = Math.max(Math.max(highLow, highPrevClose), lowPrevClose);
                atrSum += tr;
            }
            
            return atrSum / Math.min(14, candlesticks.size() - 1);
        }
    }
}