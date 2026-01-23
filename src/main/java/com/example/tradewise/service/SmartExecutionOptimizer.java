package com.example.tradewise.service;

import com.example.tradewise.service.MarketAnalysisService.Candlestick;
import com.example.tradewise.service.MarketAnalysisService.TechnicalIndicators;
import com.example.tradewise.service.SignalFusionEngine.FusionResult;
import com.example.tradewise.service.SignalFusionEngine.Decision;

import java.util.*;

/**
 * 智能执行优化器
 * 实现DeepSeek方案中的执行层功能
 */
public class SmartExecutionOptimizer {
    
    /**
     * 执行优化结果
     */
    public static class ExecutionPlan {
        private final Decision decision;
        private final double entryPrice;
        private final double stopLoss;
        private final double takeProfit;
        private final double positionSize;
        private final String entryStrategy;
        private final String exitStrategy;
        private final List<ExecutionStep> executionSteps;
        private final double riskRewardRatio;
        private final String riskLevel;
        
        public ExecutionPlan(Decision decision, double entryPrice, double stopLoss, double takeProfit, 
                           double positionSize, String entryStrategy, String exitStrategy, 
                           List<ExecutionStep> executionSteps, double riskRewardRatio, String riskLevel) {
            this.decision = decision;
            this.entryPrice = entryPrice;
            this.stopLoss = stopLoss;
            this.takeProfit = takeProfit;
            this.positionSize = positionSize;
            this.entryStrategy = entryStrategy;
            this.exitStrategy = exitStrategy;
            this.executionSteps = executionSteps;
            this.riskRewardRatio = riskRewardRatio;
            this.riskLevel = riskLevel;
        }
        
        // Getters
        public Decision getDecision() { return decision; }
        public double getEntryPrice() { return entryPrice; }
        public double getStopLoss() { return stopLoss; }
        public double getTakeProfit() { return takeProfit; }
        public double getPositionSize() { return positionSize; }
        public String getEntryStrategy() { return entryStrategy; }
        public String getExitStrategy() { return exitStrategy; }
        public List<ExecutionStep> getExecutionSteps() { return executionSteps; }
        public double getRiskRewardRatio() { return riskRewardRatio; }
        public String getRiskLevel() { return riskLevel; }
    }
    
    /**
     * 执行步骤
     */
    public static class ExecutionStep {
        private final String stepName;
        private final String description;
        private final double priceLevel;
        private final double quantity;
        private final String condition;
        
        public ExecutionStep(String stepName, String description, double priceLevel, double quantity, String condition) {
            this.stepName = stepName;
            this.description = description;
            this.priceLevel = priceLevel;
            this.quantity = quantity;
            this.condition = condition;
        }
        
        // Getters
        public String getStepName() { return stepName; }
        public String getDescription() { return description; }
        public double getPriceLevel() { return priceLevel; }
        public double getQuantity() { return quantity; }
        public String getCondition() { return condition; }
    }
    
    /**
     * 生成智能执行计划
     */
    public ExecutionPlan generateExecutionPlan(FusionResult fusionResult, String symbol, 
                                            Map<String, List<Candlestick>> multiTimeframeData) {
        Decision decision = fusionResult.getFinalDecision();
        double positionSize = fusionResult.getPositionSize();
        double stopLoss = fusionResult.getStopLoss();
        double takeProfit = fusionResult.getTakeProfit();
        
        if (decision == Decision.NO_TRADE) {
            return new ExecutionPlan(Decision.NO_TRADE, 0, 0, 0, 0, "N/A", "N/A", 
                                   new ArrayList<>(), 0, "N/A");
        }
        
        // 获取当前价格
        List<Candlestick> primaryData = multiTimeframeData.get("5m"); // 使用5分钟数据作为主要参考
        if (primaryData == null || primaryData.isEmpty()) {
            primaryData = multiTimeframeData.get("15m"); // 备选数据
        }
        
        if (primaryData == null || primaryData.isEmpty()) {
            return new ExecutionPlan(decision, 0, stopLoss, takeProfit, positionSize, 
                                   "数据不足", "N/A", new ArrayList<>(), 0, "UNKNOWN");
        }
        
        double currentPrice = primaryData.get(primaryData.size() - 1).getClose();
        
        // 根据信号类型和市场状态选择入场策略
        String entryStrategy = selectEntryStrategy(fusionResult, decision, currentPrice, multiTimeframeData);
        String exitStrategy = selectExitStrategy(fusionResult, decision, currentPrice, multiTimeframeData);
        
        // 计算入场价格（可能不同于当前价格，取决于策略）
        double entryPrice = calculateEntryPrice(decision, currentPrice, entryStrategy, multiTimeframeData);
        
        // 优化止损止盈（如果从融合结果中得到的不合适）
        double optimizedStopLoss = optimizeStopLoss(decision, stopLoss, currentPrice, multiTimeframeData);
        double optimizedTakeProfit = optimizeTakeProfit(decision, takeProfit, currentPrice, 
                                                      positionSize, optimizedStopLoss, multiTimeframeData);
        
        // 计算风险回报比
        double riskRewardRatio = calculateRiskRewardRatio(decision, entryPrice, optimizedTakeProfit, optimizedStopLoss);
        
        // 确定风险等级
        String riskLevel = determineRiskLevel(riskRewardRatio, positionSize, fusionResult.getConfidence());
        
        // 生成执行步骤
        List<ExecutionStep> executionSteps = generateExecutionSteps(decision, entryPrice, 
                                                                 optimizedStopLoss, optimizedTakeProfit, 
                                                                 positionSize, entryStrategy);
        
        return new ExecutionPlan(decision, entryPrice, optimizedStopLoss, optimizedTakeProfit, 
                               positionSize, entryStrategy, exitStrategy, executionSteps, 
                               riskRewardRatio, riskLevel);
    }
    
    /**
     * 选择入场策略
     */
    private String selectEntryStrategy(FusionResult fusionResult, Decision decision, 
                                     double currentPrice, Map<String, List<Candlestick>> multiTimeframeData) {
        // 根据融合结果中的信号来源和市场状态选择策略
        
        if (fusionResult.getContributingSignals().isEmpty()) {
            return "MARKET_ORDER"; // 默认市价单
        }
        
        // 检查主要信号类型
        boolean hasBreakoutSignal = false;
        boolean hasReversalSignal = false;
        boolean hasMomentumSignal = false;
        
        for (SignalFusionEngine.TradingSignal signal : fusionResult.getContributingSignals()) {
            String signalName = signal.getModel().name();
            if (signalName.contains("BREAKOUT") || signalName.contains("VOLATILITY")) {
                hasBreakoutSignal = true;
            } else if (signalName.contains("REVERSAL") || signalName.contains("EXTREME")) {
                hasReversalSignal = true;
            } else if (signalName.contains("MOMENTUM") || signalName.contains("TREND")) {
                hasMomentumSignal = true;
            }
        }
        
        if (hasBreakoutSignal) {
            return decision == Decision.LONG ? "BREAKOUT_ON_PULLBACK" : "BREAKDOWN_ON_PULLBACK";
        } else if (hasReversalSignal) {
            return decision == Decision.LONG ? "REVERSAL_DIP_BUY" : "REVERSAL_TOP_SELL";
        } else if (hasMomentumSignal) {
            return decision == Decision.LONG ? "MOMENTUM_FOLLOW_LONG" : "MOMENTUM_FOLLOW_SHORT";
        } else {
            return "MARKET_ORDER"; // 默认
        }
    }
    
    /**
     * 选择出场策略
     */
    private String selectExitStrategy(FusionResult fusionResult, Decision decision, 
                                    double currentPrice, Map<String, List<Candlestick>> multiTimeframeData) {
        // 根据信号强度和市场状态选择出场策略
        double signalStrength = fusionResult.getAggregatedStrength();
        
        if (signalStrength >= 8.0) {
            // 强信号：使用移动止盈或分批止盈
            return "TRAILING_TAKE_PROFIT";
        } else if (signalStrength >= 5.0) {
            // 中等信号：使用固定止盈
            return "FIXED_TAKE_PROFIT";
        } else {
            // 弱信号：快速止盈或部分止盈
            return "PARTIAL_TAKE_PROFIT";
        }
    }
    
    /**
     * 计算入场价格
     */
    private double calculateEntryPrice(Decision decision, double currentPrice, String strategy,
                                     Map<String, List<Candlestick>> multiTimeframeData) {
        // 根据策略计算最优入场价格
        
        switch (strategy) {
            case "BREAKOUT_ON_PULLBACK":
                // 突破后回踩入场：在关键位附近等待回踩
                return getPullbackEntryPrice(decision, currentPrice, multiTimeframeData);
            case "REVERSAL_DIP_BUY":
                // 反转低位买入：等待更低价格
                return currentPrice * 0.995; // 略低于当前价
            case "REVERSAL_TOP_SELL":
                // 反转高位卖出：等待更高价格
                return currentPrice * 1.005; // 略高于当前价
            case "MOMENTUM_FOLLOW_LONG":
                // 动量跟随做多：当前价格附近
                return currentPrice * 1.001; // 略高于当前价（追涨）
            case "MOMENTUM_FOLLOW_SHORT":
                // 动量跟随做空：当前价格附近
                return currentPrice * 0.999; // 略低于当前价（杀跌）
            case "MARKET_ORDER":
            default:
                // 市价单：当前价格
                return currentPrice;
        }
    }
    
    /**
     * 获取回踩入场价格
     */
    private double getPullbackEntryPrice(Decision decision, double currentPrice, 
                                       Map<String, List<Candlestick>> multiTimeframeData) {
        // 使用4小时和1小时数据找到关键支撑/阻力位
        List<Candlestick> hourlyData = multiTimeframeData.get("1h");
        if (hourlyData == null || hourlyData.size() < 20) {
            return currentPrice; // 数据不足时返回当前价格
        }
        
        // 找到最近的支撑/阻力位
        double[] levels = findKeyLevels(hourlyData);
        double nearestLevel = decision == Decision.LONG ? 
                             findNearestSupport(levels, currentPrice) : 
                             findNearestResistance(levels, currentPrice);
        
        if (nearestLevel != -1) {
            // 在关键位附近等待回踩
            double distance = Math.abs(currentPrice - nearestLevel);
            if (decision == Decision.LONG && currentPrice > nearestLevel && distance < currentPrice * 0.02) {
                // 做多时，在支撑位上方不远处入场
                return nearestLevel * 1.001;
            } else if (decision == Decision.SHORT && currentPrice < nearestLevel && distance < currentPrice * 0.02) {
                // 做空时，在阻力位下方不远处入场
                return nearestLevel * 0.999;
            }
        }
        
        return currentPrice; // 如果找不到合适位置，使用当前价格
    }
    
    /**
     * 找到关键位
     */
    private double[] findKeyLevels(List<Candlestick> data) {
        if (data.size() < 10) {
            return new double[0];
        }
        
        // 简化的关键位识别：使用最近的高低点
        List<Double> levels = new ArrayList<>();
        
        // 获取最近的高低点
        double highestHigh = Double.MIN_VALUE;
        double lowestLow = Double.MAX_VALUE;
        
        for (int i = Math.max(0, data.size() - 20); i < data.size(); i++) {
            Candlestick candle = data.get(i);
            if (candle.getHigh() > highestHigh) highestHigh = candle.getHigh();
            if (candle.getLow() < lowestLow) lowestLow = candle.getLow();
        }
        
        levels.add(highestHigh);
        levels.add(lowestLow);
        
        // 添加中间值
        double midLevel = (highestHigh + lowestLow) / 2;
        levels.add(midLevel);
        
        return levels.stream().mapToDouble(Double::doubleValue).toArray();
    }
    
    /**
     * 找到最近的支撑位
     */
    private double findNearestSupport(double[] levels, double currentPrice) {
        double nearest = -1;
        double minDistance = Double.MAX_VALUE;
        
        for (double level : levels) {
            if (level < currentPrice) { // 只考虑当前价格下方的支撑
                double distance = currentPrice - level;
                if (distance < minDistance) {
                    minDistance = distance;
                    nearest = level;
                }
            }
        }
        
        return nearest;
    }
    
    /**
     * 找到最近的阻力位
     */
    private double findNearestResistance(double[] levels, double currentPrice) {
        double nearest = -1;
        double minDistance = Double.MAX_VALUE;
        
        for (double level : levels) {
            if (level > currentPrice) { // 只考虑当前价格上方的阻力
                double distance = level - currentPrice;
                if (distance < minDistance) {
                    minDistance = distance;
                    nearest = level;
                }
            }
        }
        
        return nearest;
    }
    
    /**
     * 优化止损
     */
    private double optimizeStopLoss(Decision decision, double originalStopLoss, double currentPrice,
                                  Map<String, List<Candlestick>> multiTimeframeData) {
        // 如果原始止损不合理，重新计算
        if (originalStopLoss == 0 || Math.abs(originalStopLoss - currentPrice) / currentPrice > 0.1) {
            // 止损距离过大或为0，使用ATR计算
            List<Candlestick> data = multiTimeframeData.get("1h");
            if (data == null || data.size() < 14) {
                data = multiTimeframeData.values().iterator().next(); // 使用任意可用数据
            }
            
            if (data != null && data.size() >= 14) {
                List<Double> atrList = TechnicalIndicators.calculateATR(data, 14);
                if (!atrList.isEmpty() && atrList.get(atrList.size() - 1) != null) {
                    double atr = atrList.get(atrList.size() - 1);
                    if (decision == Decision.LONG) {
                        return currentPrice - atr * 1.5; // 做多时，止损在当前价格下方1.5倍ATR
                    } else {
                        return currentPrice + atr * 1.5; // 做空时，止损在当前价格上方1.5倍ATR
                    }
                }
            }
        }
        
        return originalStopLoss;
    }
    
    /**
     * 优化止盈
     */
    private double optimizeTakeProfit(Decision decision, double originalTakeProfit, double currentPrice,
                                    double positionSize, double stopLoss, Map<String, List<Candlestick>> multiTimeframeData) {
        // 如果原始止盈不合理，重新计算
        if (originalTakeProfit == 0 || 
            (decision == Decision.LONG && originalTakeProfit <= currentPrice) ||
            (decision == Decision.SHORT && originalTakeProfit >= currentPrice)) {
            
            // 基于风险回报比计算止盈
            double riskDistance = Math.abs(currentPrice - stopLoss);
            double minRiskRewardRatio = 1.5; // 最低风险回报比
            
            if (decision == Decision.LONG) {
                return currentPrice + riskDistance * minRiskRewardRatio;
            } else {
                return currentPrice - riskDistance * minRiskRewardRatio;
            }
        }
        
        return originalTakeProfit;
    }
    
    /**
     * 计算风险回报比
     */
    private double calculateRiskRewardRatio(Decision decision, double entryPrice, 
                                         double takeProfit, double stopLoss) {
        if (stopLoss == 0 || takeProfit == 0 || entryPrice == 0) {
            return 0;
        }
        
        double risk = decision == Decision.LONG ? 
                     Math.abs(entryPrice - stopLoss) : 
                     Math.abs(stopLoss - entryPrice);
        double reward = decision == Decision.LONG ? 
                      Math.abs(takeProfit - entryPrice) : 
                      Math.abs(entryPrice - takeProfit);
        
        return risk > 0 ? reward / risk : 0;
    }
    
    /**
     * 确定风险等级
     */
    private String determineRiskLevel(double riskRewardRatio, double positionSize, double confidence) {
        // 综合考虑风险回报比、仓位大小和信号置信度
        
        if (confidence < 0.5) {
            return "HIGH"; // 低置信度 = 高风险
        }
        
        if (riskRewardRatio < 1.0) {
            return "HIGH"; // 风险回报比小于1 = 高风险
        }
        
        if (positionSize > 0.05) { // 仓位超过5% = 高风险
            return "HIGH";
        }
        
        if (riskRewardRatio >= 2.0 && confidence >= 0.8) {
            return "LOW"; // 高风险回报比和高置信度 = 低风险
        }
        
        return "MEDIUM"; // 中等风险
    }
    
    /**
     * 生成执行步骤
     */
    private List<ExecutionStep> generateExecutionSteps(Decision decision, double entryPrice, 
                                                     double stopLoss, double takeProfit, 
                                                     double positionSize, String entryStrategy) {
        List<ExecutionStep> steps = new ArrayList<>();
        
        // 第一步：入场
        steps.add(new ExecutionStep(
            "ENTRY",
            decision == Decision.LONG ? "执行做多订单" : "执行做空订单",
            entryPrice,
            positionSize,
            String.format("当价格达到%.4f时执行", entryPrice)
        ));
        
        // 第二步：设置止损
        steps.add(new ExecutionStep(
            "STOP_LOSS",
            "设置止损订单",
            stopLoss,
            positionSize,
            String.format("当价格达到%.4f时平仓", stopLoss)
        ));
        
        // 第三步：设置止盈
        if (entryStrategy.contains("TRAILING")) {
            // 移动止盈
            steps.add(new ExecutionStep(
                "TRAILING_TAKE_PROFIT",
                "设置移动止盈订单",
                takeProfit,
                positionSize * 0.5, // 部分止盈
                String.format("当价格达到%.4f时平仓一半仓位", takeProfit)
            ));
            
            // 剩余仓位继续跟踪
            steps.add(new ExecutionStep(
                "CONTINUE_TRADING",
                "剩余仓位继续跟踪利润",
                0, // 动态跟踪
                positionSize * 0.5,
                "使用移动止损跟踪剩余仓位"
            ));
        } else if (entryStrategy.contains("PARTIAL")) {
            // 部分止盈
            double partialTakeProfit = (takeProfit + entryPrice) / 2; // 止盈一半路程
            steps.add(new ExecutionStep(
                "PARTIAL_TAKE_PROFIT",
                "部分止盈订单",
                partialTakeProfit,
                positionSize * 0.5,
                String.format("当价格达到%.4f时平仓一半仓位", partialTakeProfit)
            ));
            
            steps.add(new ExecutionStep(
                "FULL_TAKE_PROFIT",
                "全额止盈订单",
                takeProfit,
                positionSize * 0.5,
                String.format("当价格达到%.4f时平仓剩余仓位", takeProfit)
            ));
        } else {
            // 固定止盈
            steps.add(new ExecutionStep(
                "TAKE_PROFIT",
                "设置止盈订单",
                takeProfit,
                positionSize,
                String.format("当价格达到%.4f时平仓", takeProfit)
            ));
        }
        
        return steps;
    }
}