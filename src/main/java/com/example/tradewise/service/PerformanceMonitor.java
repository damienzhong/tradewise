package com.example.tradewise.service;

import com.example.tradewise.service.MarketAnalysisService.TradingSignal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 性能监控服务
 * 追踪系统表现并生成性能报告
 */
@Component
public class PerformanceMonitor {
    
    private static final Logger logger = LoggerFactory.getLogger(PerformanceMonitor.class);
    
    @Autowired
    private AdaptiveParameterSystem adaptiveParameterSystem;
    
    // 信号表现记录
    private final List<SignalPerformanceRecord> signalPerformanceRecords = new ArrayList<>();
    
    // 模型表现统计
    private final Map<String, ModelPerformanceStats> modelPerformanceStats = new ConcurrentHashMap<>();
    
    /**
     * 信号表现记录类
     */
    public static class SignalPerformanceRecord {
        private final String signalId;
        private final String symbol;
        private final String indicator;
        private final double entryPrice;
        private final TradingSignal.SignalType signalType;
        private final int score;
        private final LocalDateTime generatedTime;
        private final LocalDateTime exitTime;
        private final double exitPrice;
        private final double pnl;
        private final boolean profitable;
        private final double maxProfit;
        private final double maxLoss;
        private final long holdingTimeMinutes;
        
        public SignalPerformanceRecord(String signalId, String symbol, String indicator, 
                                     double entryPrice, TradingSignal.SignalType signalType, 
                                     int score, LocalDateTime generatedTime) {
            this.signalId = signalId;
            this.symbol = symbol;
            this.indicator = indicator;
            this.entryPrice = entryPrice;
            this.signalType = signalType;
            this.score = score;
            this.generatedTime = generatedTime;
            this.exitTime = null;
            this.exitPrice = 0.0;
            this.pnl = 0.0;
            this.profitable = false;
            this.maxProfit = 0.0;
            this.maxLoss = 0.0;
            this.holdingTimeMinutes = 0;
        }
        
        // 完整的构造函数用于更新记录
        public SignalPerformanceRecord(String signalId, String symbol, String indicator, 
                                     double entryPrice, TradingSignal.SignalType signalType, 
                                     int score, LocalDateTime generatedTime, LocalDateTime exitTime, 
                                     double exitPrice, double pnl, boolean profitable, 
                                     double maxProfit, double maxLoss, long holdingTimeMinutes) {
            this.signalId = signalId;
            this.symbol = symbol;
            this.indicator = indicator;
            this.entryPrice = entryPrice;
            this.signalType = signalType;
            this.score = score;
            this.generatedTime = generatedTime;
            this.exitTime = exitTime;
            this.exitPrice = exitPrice;
            this.pnl = pnl;
            this.profitable = profitable;
            this.maxProfit = maxProfit;
            this.maxLoss = maxLoss;
            this.holdingTimeMinutes = holdingTimeMinutes;
        }
        
        // Getters
        public String getSignalId() { return signalId; }
        public String getSymbol() { return symbol; }
        public String getIndicator() { return indicator; }
        public double getEntryPrice() { return entryPrice; }
        public TradingSignal.SignalType getSignalType() { return signalType; }
        public int getScore() { return score; }
        public LocalDateTime getGeneratedTime() { return generatedTime; }
        public LocalDateTime getExitTime() { return exitTime; }
        public double getExitPrice() { return exitPrice; }
        public double getPnl() { return pnl; }
        public boolean isProfitable() { return profitable; }
        public double getMaxProfit() { return maxProfit; }
        public double getMaxLoss() { return maxLoss; }
        public long getHoldingTimeMinutes() { return holdingTimeMinutes; }
    }
    
    /**
     * 模型表现统计类
     */
    public static class ModelPerformanceStats {
        private int totalSignals = 0;
        private int profitableSignals = 0;
        private double totalPnL = 0.0;
        private double averagePnL = 0.0;
        private double winRate = 0.0;
        private double profitFactor = 0.0;
        private double maxDrawdown = 0.0;
        private LocalDateTime lastUpdated = LocalDateTime.now();
        
        public synchronized void updateStats(double pnl, boolean profitable) {
            totalSignals++;
            totalPnL += pnl;
            if (profitable) {
                profitableSignals++;
            }
            averagePnL = totalPnL / totalSignals;
            winRate = (double) profitableSignals / totalSignals;
            lastUpdated = LocalDateTime.now();
        }
        
        // Getters
        public int getTotalSignals() { return totalSignals; }
        public int getProfitableSignals() { return profitableSignals; }
        public double getTotalPnL() { return totalPnL; }
        public double getAveragePnL() { return averagePnL; }
        public double getWinRate() { return winRate; }
        public double getProfitFactor() { return profitFactor; }
        public double getMaxDrawdown() { return maxDrawdown; }
        public LocalDateTime getLastUpdated() { return lastUpdated; }
    }
    
    /**
     * 记录信号生成事件
     */
    public void recordSignalGenerated(TradingSignal signal) {
        String signalId = signal.getSymbol() + "_" + signal.getIndicator() + "_" + signal.getTimestamp();
        SignalPerformanceRecord record = new SignalPerformanceRecord(
            signalId,
            signal.getSymbol(),
            signal.getIndicator(),
            signal.getPrice(),
            signal.getSignalType(),
            signal.getScore(),
            signal.getTimestamp()
        );
        
        signalPerformanceRecords.add(record);
        
        // 更新模型统计
        String modelName = signal.getIndicator();
        modelPerformanceStats.computeIfAbsent(modelName, k -> new ModelPerformanceStats());
        
        logger.debug("记录信号生成: {} - {}", signalId, signal.getSignalType());
    }
    
    /**
     * 更新信号结果
     */
    public void updateSignalResult(String signalId, double exitPrice, double pnl, boolean profitable, 
                                 double maxProfit, double maxLoss, long holdingTimeMinutes) {
        SignalPerformanceRecord record = signalPerformanceRecords.stream()
                .filter(r -> r.getSignalId().equals(signalId))
                .findFirst()
                .orElse(null);
        
        if (record != null) {
            SignalPerformanceRecord updatedRecord = new SignalPerformanceRecord(
                record.getSignalId(),
                record.getSymbol(),
                record.getIndicator(),
                record.getEntryPrice(),
                record.getSignalType(),
                record.getScore(),
                record.getGeneratedTime(),
                LocalDateTime.now(),
                exitPrice,
                pnl,
                profitable,
                maxProfit,
                maxLoss,
                holdingTimeMinutes
            );
            
            // 替换原记录
            int index = signalPerformanceRecords.indexOf(record);
            if (index != -1) {
                signalPerformanceRecords.set(index, updatedRecord);
            }
            
            // 更新模型统计
            String modelName = record.getIndicator();
            ModelPerformanceStats stats = modelPerformanceStats.get(modelName);
            if (stats != null) {
                stats.updateStats(pnl, profitable);
            }
            
            logger.debug("更新信号结果: {} - PnL: {}, Profitable: {}", signalId, pnl, profitable);
        } else {
            logger.warn("未找到信号记录: {}", signalId);
        }
    }
    
    /**
     * 生成性能报告
     */
    public PerformanceReport generatePerformanceReport() {
        int totalSignals = signalPerformanceRecords.size();
        int profitableSignals = (int) signalPerformanceRecords.stream()
                .filter(SignalPerformanceRecord::isProfitable)
                .count();
        
        double totalPnL = signalPerformanceRecords.stream()
                .mapToDouble(SignalPerformanceRecord::getPnl)
                .sum();
        
        double averagePnL = totalSignals > 0 ? totalPnL / totalSignals : 0.0;
        double winRate = totalSignals > 0 ? (double) profitableSignals / totalSignals : 0.0;
        
        // 计算最大回撤
        double maxDrawdown = calculateMaxDrawdown();
        
        // 计算盈亏比
        double profitFactor = calculateProfitFactor();
        
        // 计算夏普比率（简化版本）
        double sharpeRatio = calculateSharpeRatio();
        
        PerformanceReport report = new PerformanceReport(
            totalSignals,
            profitableSignals,
            totalPnL,
            averagePnL,
            winRate,
            profitFactor,
            maxDrawdown,
            sharpeRatio,
            LocalDateTime.now()
        );
        
        // 记录到自适应参数系统
        adaptiveParameterSystem.recordPerformance(winRate, profitFactor, sharpeRatio, maxDrawdown);
        
        return report;
    }
    
    /**
     * 计算最大回撤
     */
    private double calculateMaxDrawdown() {
        if (signalPerformanceRecords.isEmpty()) {
            return 0.0;
        }
        
        double peak = 0.0;
        double maxDrawdown = 0.0;
        double equity = 0.0;
        
        for (SignalPerformanceRecord record : signalPerformanceRecords) {
            equity += record.getPnl();
            if (equity > peak) {
                peak = equity;
            }
            double drawdown = peak - equity;
            if (drawdown > maxDrawdown) {
                maxDrawdown = drawdown;
            }
        }
        
        return maxDrawdown;
    }
    
    /**
     * 计算盈亏比
     */
    private double calculateProfitFactor() {
        if (signalPerformanceRecords.isEmpty()) {
            return 1.0;
        }
        
        double grossProfit = signalPerformanceRecords.stream()
                .mapToDouble(r -> Math.max(0, r.getPnl()))
                .sum();
        
        double grossLoss = Math.abs(signalPerformanceRecords.stream()
                .mapToDouble(r -> Math.min(0, r.getPnl()))
                .sum());
        
        return grossLoss > 0 ? grossProfit / grossLoss : grossProfit > 0 ? Double.MAX_VALUE : 1.0;
    }
    
    /**
     * 计算夏普比率（简化版本）
     */
    private double calculateSharpeRatio() {
        if (signalPerformanceRecords.size() < 2) {
            return 0.0;
        }
        
        double avgReturn = signalPerformanceRecords.stream()
                .mapToDouble(SignalPerformanceRecord::getPnl)
                .average()
                .orElse(0.0);
        
        double variance = signalPerformanceRecords.stream()
                .mapToDouble(r -> Math.pow(r.getPnl() - avgReturn, 2))
                .average()
                .orElse(0.0);
        
        double stdDev = Math.sqrt(variance);
        
        return stdDev > 0 ? avgReturn / stdDev : 0.0;
    }
    
    /**
     * 性能报告类
     */
    public static class PerformanceReport {
        private final int totalSignals;
        private final int profitableSignals;
        private final double totalPnL;
        private final double averagePnL;
        private final double winRate;
        private final double profitFactor;
        private final double maxDrawdown;
        private final double sharpeRatio;
        private final LocalDateTime reportTime;
        
        public PerformanceReport(int totalSignals, int profitableSignals, double totalPnL, 
                               double averagePnL, double winRate, double profitFactor, 
                               double maxDrawdown, double sharpeRatio, LocalDateTime reportTime) {
            this.totalSignals = totalSignals;
            this.profitableSignals = profitableSignals;
            this.totalPnL = totalPnL;
            this.averagePnL = averagePnL;
            this.winRate = winRate;
            this.profitFactor = profitFactor;
            this.maxDrawdown = maxDrawdown;
            this.sharpeRatio = sharpeRatio;
            this.reportTime = reportTime;
        }
        
        // Getters
        public int getTotalSignals() { return totalSignals; }
        public int getProfitableSignals() { return profitableSignals; }
        public double getTotalPnL() { return totalPnL; }
        public double getAveragePnL() { return averagePnL; }
        public double getWinRate() { return winRate; }
        public double getProfitFactor() { return profitFactor; }
        public double getMaxDrawdown() { return maxDrawdown; }
        public double getSharpeRatio() { return sharpeRatio; }
        public LocalDateTime getReportTime() { return reportTime; }
        
        @Override
        public String toString() {
            return String.format(
                "Performance Report:\n" +
                "  Total Signals: %d\n" +
                "  Profitable Signals: %d (%.2f%%)\n" +
                "  Total PnL: %.4f\n" +
                "  Average PnL: %.4f\n" +
                "  Win Rate: %.2f%%\n" +
                "  Profit Factor: %.2f\n" +
                "  Max Drawdown: %.4f\n" +
                "  Sharpe Ratio: %.2f\n" +
                "  Report Time: %s",
                totalSignals, profitableSignals, winRate * 100, totalPnL, 
                averagePnL, winRate * 100, profitFactor, maxDrawdown, 
                sharpeRatio, reportTime
            );
        }
    }
    
    /**
     * 获取模型表现统计
     */
    public Map<String, ModelPerformanceStats> getModelPerformanceStats() {
        return new ConcurrentHashMap<>(modelPerformanceStats);
    }
    
    /**
     * 获取最近的信号表现记录
     */
    public List<SignalPerformanceRecord> getRecentSignalPerformance(int limit) {
        int size = signalPerformanceRecords.size();
        int start = Math.max(0, size - limit);
        return new ArrayList<>(signalPerformanceRecords.subList(start, size));
    }
}