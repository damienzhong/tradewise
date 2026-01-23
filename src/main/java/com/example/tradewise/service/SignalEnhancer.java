package com.example.tradewise.service;

import com.example.tradewise.service.MarketAnalysisService.TradingSignal;
import com.example.tradewise.service.MarketAnalysisService.Candlestick;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 信号增强器
 * 专门用于增强信号，计算完整的交易计划
 */
@Service
public class SignalEnhancer {
    
    private static final Logger logger = LoggerFactory.getLogger(SignalEnhancer.class);
    
    @Autowired
    private MarketDataService marketDataService;
    
    /**
     * 增强交易信号，计算完整的交易计划
     */
    public TradingSignal enhance(TradingSignal signal, List<Candlestick> candlesticks) {
        TradingSignal enhanced = new TradingSignal(
            signal.getSymbol(),
            signal.getSignalType(),
            signal.getIndicator(),
            signal.getPrice(),
            signal.getReason(),
            signal.getSuggestion(),
            signal.getConfidence()
        );
        
        // 复制原始信号的所有属性
        enhanced.setScore(signal.getScore());
        enhanced.setSignalLevel(signal.getSignalLevel());
        enhanced.setSignalExplanation(signal.getSignalExplanation() != null ? 
            new java.util.HashMap<>(signal.getSignalExplanation()) : new java.util.HashMap<>());
        enhanced.setScoreDetails(signal.getScoreDetails() != null ?
            new java.util.HashMap<>(signal.getScoreDetails()) : new java.util.HashMap<>());
        
        // 计算ATR用于风险管理
        List<Double> atrList = MarketAnalysisService.TechnicalIndicators.calculateATR(candlesticks, 14);
        double currentAtr = atrList.get(atrList.size() - 1) != null ? atrList.get(atrList.size() - 1) : 0;
        
        // 1. 计算止损
        enhanced.setStopLoss(calculateStopLoss(enhanced, candlesticks, currentAtr));
        
        // 2. 计算止盈
        enhanced.setTakeProfit(calculateTakeProfit(enhanced, candlesticks, currentAtr));
        
        // 3. 计算风险回报比
        double riskRewardRatio = calculateRiskRewardRatio(enhanced);
        enhanced.getSignalExplanation().put("risk_reward_ratio", riskRewardRatio);
        
        // 4. 计算建议仓位
        double positionSize = calculatePositionSize(enhanced, candlesticks);
        enhanced.setPositionSize(positionSize);
        
        // 5. 计算预期收益率
        double expectedReturn = calculateExpectedReturn(enhanced);
        enhanced.setRoiPercentage(expectedReturn);
        
        // 6. 计算合约相关参数
        calculateContractParameters(enhanced, candlesticks, currentAtr);
        
        logger.debug("信号增强完成: {} - {}", signal.getSymbol(), signal.getIndicator());
        
        return enhanced;
    }
    
    /**
     * 计算止损位
     */
    private double calculateStopLoss(TradingSignal signal, List<Candlestick> candlesticks, double atr) {
        if (candlesticks.isEmpty() || atr <= 0) {
            // 如果无法计算ATR，则使用默认的止损策略
            double defaultStopLoss;
            if (signal.getSignalType() == TradingSignal.SignalType.BUY) {
                defaultStopLoss = signal.getPrice() * 0.98; // 2%止损
            } else {
                defaultStopLoss = signal.getPrice() * 1.02; // 2%止损
            }
            return defaultStopLoss;
        }
        
        // 根据信号类型和ATR计算止损
        double atrMultiplier = 1.5; // 默认ATR乘数
        
        // 根据信号质量调整ATR乘数
        if (signal.getScore() >= 8) {
            atrMultiplier = 1.2; // 高质量信号，较小的止损
        } else if (signal.getScore() >= 6) {
            atrMultiplier = 1.5; // 中等质量信号
        } else {
            atrMultiplier = 2.0; // 低质量信号，较大的止损
        }
        
        double stopLoss;
        if (signal.getSignalType() == TradingSignal.SignalType.BUY) {
            stopLoss = signal.getPrice() - (atr * atrMultiplier);
        } else {
            stopLoss = signal.getPrice() + (atr * atrMultiplier);
        }
        
        return stopLoss;
    }
    
    /**
     * 计算止盈位
     */
    private double calculateTakeProfit(TradingSignal signal, List<Candlestick> candlesticks, double atr) {
        if (atr <= 0) {
            // 如果无法计算ATR，则使用默认的止盈策略
            double defaultTakeProfit;
            if (signal.getSignalType() == TradingSignal.SignalType.BUY) {
                defaultTakeProfit = signal.getPrice() * 1.04; // 4%止盈
            } else {
                defaultTakeProfit = signal.getPrice() * 0.96; // 4%止盈
            }
            return defaultTakeProfit;
        }
        
        // 根据信号质量和风险回报比计算止盈
        double riskDistance = Math.abs(signal.getPrice() - signal.getStopLoss());
        double riskRewardRatio;
        
        if (signal.getScore() >= 8) {
            riskRewardRatio = 3.0; // 高质量信号，3:1风险回报比
        } else if (signal.getScore() >= 6) {
            riskRewardRatio = 2.5; // 中等质量信号，2.5:1风险回报比
        } else {
            riskRewardRatio = 2.0; // 低质量信号，2:1风险回报比
        }
        
        double takeProfit;
        if (signal.getSignalType() == TradingSignal.SignalType.BUY) {
            takeProfit = signal.getPrice() + (riskDistance * riskRewardRatio);
        } else {
            takeProfit = signal.getPrice() - (riskDistance * riskRewardRatio);
        }
        
        return takeProfit;
    }
    
    /**
     * 计算风险回报比
     */
    private double calculateRiskRewardRatio(TradingSignal signal) {
        double entryPrice = signal.getPrice();
        double stopLoss = signal.getStopLoss();
        double takeProfit = signal.getTakeProfit();
        
        if (stopLoss == 0 || takeProfit == 0) {
            return 0.0;
        }
        
        double risk = Math.abs(entryPrice - stopLoss);
        double reward = Math.abs(takeProfit - entryPrice);
        
        if (risk == 0) {
            return 0.0;
        }
        
        return reward / risk;
    }
    
    /**
     * 计算建议仓位
     */
    private double calculatePositionSize(TradingSignal signal, List<Candlestick> candlesticks) {
        // 基础仓位计算 - 根据信号评分调整
        double baseRiskPercentage;
        if (signal.getScore() >= 8) {
            baseRiskPercentage = 0.02; // 高质量信号，2%风险
        } else if (signal.getScore() >= 6) {
            baseRiskPercentage = 0.01; // 中等质量信号，1%风险
        } else {
            baseRiskPercentage = 0.005; // 低质量信号，0.5%风险
        }
        
        // 假设账户余额为10,000 USDT（实际应用中应从配置或数据库获取）
        double accountBalance = 10000.0;
        
        // 计算风险金额
        double riskAmount = accountBalance * baseRiskPercentage;
        
        // 计算风险距离
        double riskDistance = Math.abs(signal.getPrice() - signal.getStopLoss());
        
        // 计算仓位价值
        double positionValue = riskAmount / riskDistance;
        
        // 设置最小和最大仓位限制
        double minPosition = accountBalance * 0.001; // 最小0.1%仓位
        double maxPosition = accountBalance * 0.05;  // 最大5%仓位
        
        return Math.max(minPosition, Math.min(positionValue, maxPosition));
    }
    
    /**
     * 计算预期收益率
     */
    private double calculateExpectedReturn(TradingSignal signal) {
        double riskRewardRatio = calculateRiskRewardRatio(signal);
        double riskPercentage = Math.abs(signal.getPrice() - signal.getStopLoss()) / signal.getPrice() * 100;
        
        if (signal.getSignalType() == TradingSignal.SignalType.BUY) {
            return riskPercentage * riskRewardRatio; // 预期收益百分比
        } else {
            return riskPercentage * riskRewardRatio; // 预期收益百分比（空单也是正数）
        }
    }
    
    /**
     * 计算合约相关参数
     */
    private void calculateContractParameters(TradingSignal signal, List<Candlestick> candlesticks, double atr) {
        // 根据信号质量确定杠杆
        int leverage;
        if (signal.getScore() >= 8) {
            leverage = 20; // 高质量信号，较高杠杆
        } else if (signal.getScore() >= 6) {
            leverage = 10; // 中等质量信号，适中杠杆
        } else {
            leverage = 5;  // 低质量信号，保守杠杆
        }
        
        signal.setLeverage(leverage);
        
        // 计算所需保证金（假设账户余额为10,000 USDT）
        double accountBalance = 10000.0;
        double marginRequired = (signal.getPositionSize() * signal.getPrice()) / leverage;
        signal.setMarginRequired(marginRequired);
        
        // 计算爆仓价格
        double liquidationPrice;
        if (signal.getSignalType() == TradingSignal.SignalType.BUY) {
            // 多单爆仓价格 = 入场价 - (保证金余额 / 仓位规模)
            liquidationPrice = signal.getPrice() - (accountBalance / (leverage * signal.getPositionSize()));
        } else {
            // 空单爆仓价格 = 入场价 + (保证金余额 / 仓位规模)
            liquidationPrice = signal.getPrice() + (accountBalance / (leverage * signal.getPositionSize()));
        }
        signal.setLiquidationPrice(liquidationPrice);
    }
}