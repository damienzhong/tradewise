package com.example.tradewise.service;

import com.example.tradewise.config.TradeWiseProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 系统健康监控服务
 * 用于跟踪系统运行状态、API调用统计等
 */
@Service
public class SystemHealthService {

    private static final Logger logger = LoggerFactory.getLogger(SystemHealthService.class);

    @Autowired
    private TradeWiseProperties tradeWiseProperties;

    // 系统统计信息
    private final AtomicInteger successfulApiCalls = new AtomicInteger(0);
    private final AtomicInteger failedApiCalls = new AtomicInteger(0);
    private final AtomicLong totalProcessingTime = new AtomicLong(0);
    private volatile LocalDateTime lastSuccessfulCall = null;
    private volatile LocalDateTime lastFailedCall = null;
    private volatile LocalDateTime startTime = null;

    // 跟单监控统计
    private final AtomicInteger ordersProcessed = new AtomicInteger(0);
    private final AtomicInteger tradersMonitored = new AtomicInteger(0);

    // 市场分析统计
    private final AtomicInteger marketAnalysesPerformed = new AtomicInteger(0);
    private final AtomicInteger tradingSignalsGenerated = new AtomicInteger(0);

    @PostConstruct
    public void init() {
        startTime = LocalDateTime.now();
        logger.info("System Health Service initialized at {}", startTime);
    }

    /**
     * 记录成功的API调用
     */
    public void recordSuccessfulApiCall(long processingTimeMs) {
        successfulApiCalls.incrementAndGet();
        totalProcessingTime.addAndGet(processingTimeMs);
        lastSuccessfulCall = LocalDateTime.now();
    }

    /**
     * 记录失败的API调用
     */
    public void recordFailedApiCall() {
        failedApiCalls.incrementAndGet();
        lastFailedCall = LocalDateTime.now();
    }

    /**
     * 记录处理的订单数量
     */
    public void recordOrdersProcessed(int count) {
        ordersProcessed.addAndGet(count);
    }

    /**
     * 记录监控的交易员数量
     */
    public void recordTradersMonitored(int count) {
        tradersMonitored.addAndGet(count);
    }

    /**
     * 记录市场分析执行次数
     */
    public void recordMarketAnalysisPerformed() {
        marketAnalysesPerformed.incrementAndGet();
    }

    /**
     * 记录生成的交易信号数量
     */
    public void recordTradingSignalsGenerated(int count) {
        tradingSignalsGenerated.addAndGet(count);
    }

    /**
     * 获取系统健康状态
     */
    public SystemHealthStatus getSystemHealthStatus() {
        SystemHealthStatus status = new SystemHealthStatus();
        status.setStartTime(startTime);
        status.setUptimeMinutes(java.time.Duration.between(startTime, LocalDateTime.now()).toMinutes());
        status.setSuccessfulApiCalls(successfulApiCalls.get());
        status.setFailedApiCalls(failedApiCalls.get());
        status.setTotalProcessingTimeMs(totalProcessingTime.get());
        status.setAverageProcessingTimeMs(successfulApiCalls.get() > 0 ? 
            totalProcessingTime.get() / successfulApiCalls.get() : 0);
        status.setLastSuccessfulCall(lastSuccessfulCall);
        status.setLastFailedCall(lastFailedCall);
        status.setOrdersProcessed(ordersProcessed.get());
        status.setTradersMonitored(tradersMonitored.get());
        status.setMarketAnalysesPerformed(marketAnalysesPerformed.get());
        status.setTradingSignalsGenerated(tradingSignalsGenerated.get());
        
        // 计算成功率
        int totalCalls = successfulApiCalls.get() + failedApiCalls.get();
        status.setSuccessRate(totalCalls > 0 ? (double) successfulApiCalls.get() / totalCalls * 100 : 100.0);
        
        // 根据各种指标评估健康状况
        status.setOverallHealth(evaluateOverallHealth(status));

        return status;
    }

    /**
     * 评估整体健康状况
     */
    private String evaluateOverallHealth(SystemHealthStatus status) {
        // 检查最近的失败情况
        if (status.getLastFailedCall() != null && 
            java.time.Duration.between(status.getLastFailedCall(), LocalDateTime.now()).toMinutes() < 5) {
            return "WARNING"; // 最近5分钟内有失败
        }

        // 检查成功率
        if (status.getSuccessRate() < 80) {
            return "UNHEALTHY"; // 成功率低于80%
        }

        // 检查API调用频率是否正常
        if (status.getSuccessfulApiCalls() == 0 && 
            java.time.Duration.between(status.getStartTime(), LocalDateTime.now()).toMinutes() > 5) {
            return "WARNING"; // 运行超过5分钟但没有成功调用
        }

        return "HEALTHY";
    }

    /**
     * 重置统计信息
     */
    public void resetStatistics() {
        successfulApiCalls.set(0);
        failedApiCalls.set(0);
        totalProcessingTime.set(0);
        ordersProcessed.set(0);
        tradersMonitored.set(0);
        marketAnalysesPerformed.set(0);
        tradingSignalsGenerated.set(0);
        lastSuccessfulCall = null;
        lastFailedCall = null;
        startTime = LocalDateTime.now();
        
        logger.info("System statistics have been reset");
    }

    /**
     * 系统健康状态信息类
     */
    public static class SystemHealthStatus {
        private LocalDateTime startTime;
        private long uptimeMinutes;
        private int successfulApiCalls;
        private int failedApiCalls;
        private long totalProcessingTimeMs;
        private long averageProcessingTimeMs;
        private LocalDateTime lastSuccessfulCall;
        private LocalDateTime lastFailedCall;
        private double successRate;
        private String overallHealth;
        
        // 业务统计
        private int ordersProcessed;
        private int tradersMonitored;
        private int marketAnalysesPerformed;
        private int tradingSignalsGenerated;

        // Getters and Setters
        public LocalDateTime getStartTime() { return startTime; }
        public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
        
        public long getUptimeMinutes() { return uptimeMinutes; }
        public void setUptimeMinutes(long uptimeMinutes) { this.uptimeMinutes = uptimeMinutes; }
        
        public int getSuccessfulApiCalls() { return successfulApiCalls; }
        public void setSuccessfulApiCalls(int successfulApiCalls) { this.successfulApiCalls = successfulApiCalls; }
        
        public int getFailedApiCalls() { return failedApiCalls; }
        public void setFailedApiCalls(int failedApiCalls) { this.failedApiCalls = failedApiCalls; }
        
        public long getTotalProcessingTimeMs() { return totalProcessingTimeMs; }
        public void setTotalProcessingTimeMs(long totalProcessingTimeMs) { this.totalProcessingTimeMs = totalProcessingTimeMs; }
        
        public long getAverageProcessingTimeMs() { return averageProcessingTimeMs; }
        public void setAverageProcessingTimeMs(long averageProcessingTimeMs) { this.averageProcessingTimeMs = averageProcessingTimeMs; }
        
        public LocalDateTime getLastSuccessfulCall() { return lastSuccessfulCall; }
        public void setLastSuccessfulCall(LocalDateTime lastSuccessfulCall) { this.lastSuccessfulCall = lastSuccessfulCall; }
        
        public LocalDateTime getLastFailedCall() { return lastFailedCall; }
        public void setLastFailedCall(LocalDateTime lastFailedCall) { this.lastFailedCall = lastFailedCall; }
        
        public double getSuccessRate() { return successRate; }
        public void setSuccessRate(double successRate) { this.successRate = successRate; }
        
        public String getOverallHealth() { return overallHealth; }
        public void setOverallHealth(String overallHealth) { this.overallHealth = overallHealth; }
        
        public int getOrdersProcessed() { return ordersProcessed; }
        public void setOrdersProcessed(int ordersProcessed) { this.ordersProcessed = ordersProcessed; }
        
        public int getTradersMonitored() { return tradersMonitored; }
        public void setTradersMonitored(int tradersMonitored) { this.tradersMonitored = tradersMonitored; }
        
        public int getMarketAnalysesPerformed() { return marketAnalysesPerformed; }
        public void setMarketAnalysesPerformed(int marketAnalysesPerformed) { this.marketAnalysesPerformed = marketAnalysesPerformed; }
        
        public int getTradingSignalsGenerated() { return tradingSignalsGenerated; }
        public void setTradingSignalsGenerated(int tradingSignalsGenerated) { this.tradingSignalsGenerated = tradingSignalsGenerated; }
    }
}