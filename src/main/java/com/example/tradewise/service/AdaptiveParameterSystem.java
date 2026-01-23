package com.example.tradewise.service;

import com.example.tradewise.config.TradeWiseProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 自适应参数系统
 * 基于近期表现动态优化系统参数
 */
@Component
public class AdaptiveParameterSystem {
    
    private static final Logger logger = LoggerFactory.getLogger(AdaptiveParameterSystem.class);
    
    @Autowired
    private TradeWiseProperties tradeWiseProperties;
    
    // 参数历史记录
    private final Map<String, List<ParameterRecord>> parameterHistory = new ConcurrentHashMap<>();
    
    // 性能追踪记录
    private final List<PerformanceRecord> performanceRecords = new ArrayList<>();
    
    // 当前参数配置
    private final Map<String, Object> currentParameters = new ConcurrentHashMap<>();
    
    public AdaptiveParameterSystem() {
        initializeDefaultParameters();
    }
    
    /**
     * 参数记录类
     */
    private static class ParameterRecord {
        private final Map<String, Object> parameters;
        private final LocalDateTime timestamp;
        
        public ParameterRecord(Map<String, Object> parameters) {
            this.parameters = new HashMap<>(parameters);
            this.timestamp = LocalDateTime.now();
        }
        
        public Map<String, Object> getParameters() { return parameters; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }
    
    /**
     * 性能记录类
     */
    public static class PerformanceRecord {
        private final double winRate;        // 胜率
        private final double profitFactor;   // 盈亏比
        private final double sharpeRatio;    // 夏普比率
        private final double maxDrawdown;    // 最大回撤
        private final LocalDateTime timestamp;
        private final String period;         // 统计周期
        
        public PerformanceRecord(double winRate, double profitFactor, double sharpeRatio, 
                               double maxDrawdown, String period) {
            this.winRate = winRate;
            this.profitFactor = profitFactor;
            this.sharpeRatio = sharpeRatio;
            this.maxDrawdown = maxDrawdown;
            this.timestamp = LocalDateTime.now();
            this.period = period;
        }
        
        // Getters
        public double getWinRate() { return winRate; }
        public double getProfitFactor() { return profitFactor; }
        public double getSharpeRatio() { return sharpeRatio; }
        public double getMaxDrawdown() { return maxDrawdown; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public String getPeriod() { return period; }
    }
    
    /**
     * 初始化默认参数
     */
    private void initializeDefaultParameters() {
        // 信号确认阈值
        currentParameters.put("signal_confirmation_threshold", 2);
        
        // ATR止损乘数
        currentParameters.put("atr_stop_loss_multiplier", 1.5);
        
        // ATR止盈乘数
        currentParameters.put("atr_take_profit_multiplier", 2.0);
        
        // 信号冷却时间（小时）
        currentParameters.put("cooldown_hours_level_1", 2);
        currentParameters.put("cooldown_hours_level_2", 1);
        currentParameters.put("cooldown_hours_level_3", 4);
        
        // 模型权重（用于信号融合）
        currentParameters.put("trend_model_weight", 1.0);
        currentParameters.put("momentum_model_weight", 1.0);
        currentParameters.put("volume_model_weight", 1.0);
        
        logger.info("默认参数已初始化");
    }
    
    /**
     * 基于近期表现优化参数
     */
    public void optimizeParameters() {
        logger.debug("开始参数优化...");
        
        // 获取最近7天的性能数据
        List<PerformanceRecord> recentPerformance = getRecentPerformance(7);
        
        if (recentPerformance.isEmpty()) {
            logger.debug("没有足够的性能数据进行参数优化");
            return;
        }
        
        // 计算平均性能指标
        double avgWinRate = recentPerformance.stream()
                .mapToDouble(PerformanceRecord::getWinRate)
                .average()
                .orElse(0.5);
        
        double avgSharpeRatio = recentPerformance.stream()
                .mapToDouble(PerformanceRecord::getSharpeRatio)
                .average()
                .orElse(1.0);
        
        double maxDrawdown = recentPerformance.stream()
                .mapToDouble(PerformanceRecord::getMaxDrawdown)
                .max()
                .orElse(0.15);
        
        // 根据性能调整参数
        adjustParametersBasedOnPerformance(avgWinRate, avgSharpeRatio, maxDrawdown);
        
        logger.info("参数优化完成");
    }
    
    /**
     * 根据性能指标调整参数
     */
    private void adjustParametersBasedOnPerformance(double winRate, double sharpeRatio, double maxDrawdown) {
        logger.debug("根据性能调整参数 - 胜率: {}, 夏普比率: {}, 最大回撤: {}", winRate, sharpeRatio, maxDrawdown);
        
        // 如果胜率低于45%，提高信号确认阈值
        if (winRate < 0.45) {
            int currentThreshold = (Integer) currentParameters.get("signal_confirmation_threshold");
            if (currentThreshold < 3) { // 最大不超过3
                currentParameters.put("signal_confirmation_threshold", currentThreshold + 1);
                logger.info("胜率较低，提高信号确认阈值至: {}", currentParameters.get("signal_confirmation_threshold"));
            }
        } else if (winRate > 0.65) {
            // 如果胜率很高，可以适当降低阈值以捕获更多机会
            int currentThreshold = (Integer) currentParameters.get("signal_confirmation_threshold");
            if (currentThreshold > 1) {
                currentParameters.put("signal_confirmation_threshold", currentThreshold - 1);
                logger.info("胜率较高，降低信号确认阈值至: {}", currentParameters.get("signal_confirmation_threshold"));
            }
        }
        
        // 如果夏普比率较低，增加止损保护
        if (sharpeRatio < 0.8) {
            double currentMultiplier = (Double) currentParameters.get("atr_stop_loss_multiplier");
            if (currentMultiplier < 2.0) {
                currentMultiplier = Math.min(2.0, currentMultiplier + 0.2);
                currentParameters.put("atr_stop_loss_multiplier", currentMultiplier);
                logger.info("夏普比率较低，增加止损ATR乘数至: {}", currentMultiplier);
            }
        }
        
        // 如果最大回撤过大，降低模型权重
        if (maxDrawdown > 0.15) {
            double currentWeight = (Double) currentParameters.get("trend_model_weight");
            if (currentWeight > 0.5) {
                currentWeight = Math.max(0.5, currentWeight - 0.1);
                currentParameters.put("trend_model_weight", currentWeight);
                currentParameters.put("momentum_model_weight", currentWeight);
                currentParameters.put("volume_model_weight", currentWeight);
                logger.info("最大回撤过大，降低模型权重至: {}", currentWeight);
            }
        }
    }
    
    /**
     * 记录性能数据
     */
    public void recordPerformance(double winRate, double profitFactor, double sharpeRatio, double maxDrawdown) {
        PerformanceRecord record = new PerformanceRecord(winRate, profitFactor, sharpeRatio, maxDrawdown, "daily");
        performanceRecords.add(record);
        
        // 保持最近30天的记录
        if (performanceRecords.size() > 30) {
            performanceRecords.subList(0, performanceRecords.size() - 30).clear();
        }
        
        logger.debug("性能记录已保存 - 胜率: {}, 盈亏比: {}, 夏普比率: {}, 最大回撤: {}", 
            winRate, profitFactor, sharpeRatio, maxDrawdown);
    }
    
    /**
     * 获取最近的性能记录
     */
    private List<PerformanceRecord> getRecentPerformance(int days) {
        LocalDateTime cutoffTime = LocalDateTime.now().minusDays(days);
        
        return performanceRecords.stream()
                .filter(record -> record.getTimestamp().isAfter(cutoffTime))
                .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * 获取当前参数值
     */
    public Object getParameter(String paramName) {
        return currentParameters.get(paramName);
    }
    
    /**
     * 获取信号确认阈值
     */
    public int getSignalConfirmationThreshold() {
        return (Integer) currentParameters.getOrDefault("signal_confirmation_threshold", 2);
    }
    
    /**
     * 获取ATR止损乘数
     */
    public double getAtrStopLossMultiplier() {
        return (Double) currentParameters.getOrDefault("atr_stop_loss_multiplier", 1.5);
    }
    
    /**
     * 获取ATR止盈乘数
     */
    public double getAtrTakeProfitMultiplier() {
        return (Double) currentParameters.getOrDefault("atr_take_profit_multiplier", 2.0);
    }
    
    /**
     * 获取LEVEL_1信号冷却时间
     */
    public int getCooldownHoursLevel1() {
        return (Integer) currentParameters.getOrDefault("cooldown_hours_level_1", 2);
    }
    
    /**
     * 获取LEVEL_2信号冷却时间
     */
    public int getCooldownHoursLevel2() {
        return (Integer) currentParameters.getOrDefault("cooldown_hours_level_2", 1);
    }
    
    /**
     * 获取LEVEL_3信号冷却时间
     */
    public int getCooldownHoursLevel3() {
        return (Integer) currentParameters.getOrDefault("cooldown_hours_level_3", 4);
    }
    
    /**
     * 获取趋势模型权重
     */
    public double getTrendModelWeight() {
        return (Double) currentParameters.getOrDefault("trend_model_weight", 1.0);
    }
    
    /**
     * 获取动量模型权重
     */
    public double getMomentumModelWeight() {
        return (Double) currentParameters.getOrDefault("momentum_model_weight", 1.0);
    }
    
    /**
     * 获取成交量模型权重
     */
    public double getVolumeModelWeight() {
        return (Double) currentParameters.getOrDefault("volume_model_weight", 1.0);
    }
    
    /**
     * 重置为默认参数
     */
    public void resetToDefaults() {
        initializeDefaultParameters();
        logger.info("参数已重置为默认值");
    }
    
    /**
     * 手动设置参数
     */
    public void setParameter(String paramName, Object value) {
        currentParameters.put(paramName, value);
        logger.info("参数 {} 已设置为: {}", paramName, value);
    }
}