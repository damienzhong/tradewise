package com.example.tradewise.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SignalMonitorService {

    private static final Logger logger = LoggerFactory.getLogger(SignalMonitorService.class);

    private final Map<String, SignalStats> dailyStats = new ConcurrentHashMap<>();
    private LocalDateTime lastResetTime = LocalDateTime.now();

    public static class SignalStats {
        private int rawSignalsCount = 0;
        private int enhancedSignalsCount = 0;
        private int filteredSignalsCount = 0;
        private int sentSignalsCount = 0;
        private LocalDateTime lastSignalTime;
        private List<String> recentSignals = new ArrayList<>();
        private Map<Integer, Integer> scoreDistribution = new java.util.HashMap<>(); // 评分分布

        public void addRecentSignal(String signalInfo) {
            recentSignals.add(signalInfo);
            if (recentSignals.size() > 10) {
                recentSignals.remove(0);
            }
        }

        public int getRawSignalsCount() { return rawSignalsCount; }
        public int getEnhancedSignalsCount() { return enhancedSignalsCount; }
        public int getFilteredSignalsCount() { return filteredSignalsCount; }
        public int getSentSignalsCount() { return sentSignalsCount; }
        public LocalDateTime getLastSignalTime() { return lastSignalTime; }
        public List<String> getRecentSignals() { return recentSignals; }
        public Map<Integer, Integer> getScoreDistribution() { return scoreDistribution; }
    }

    public void recordRawSignals(String symbol, int count) {
        getOrCreateStats(symbol).rawSignalsCount += count;
    }

    public void recordEnhancedSignals(String symbol, int count, int score) {
        SignalStats stats = getOrCreateStats(symbol);
        stats.enhancedSignalsCount += count;
        stats.scoreDistribution.put(score, stats.scoreDistribution.getOrDefault(score, 0) + count);
    }

    public void recordFilteredSignals(String symbol, int count) {
        getOrCreateStats(symbol).filteredSignalsCount += count;
    }

    public void recordSentSignal(String symbol, String signalInfo) {
        SignalStats stats = getOrCreateStats(symbol);
        stats.sentSignalsCount++;
        stats.lastSignalTime = LocalDateTime.now();
        stats.addRecentSignal(signalInfo);
    }

    private SignalStats getOrCreateStats(String symbol) {
        return dailyStats.computeIfAbsent(symbol, k -> new SignalStats());
    }

    public Map<String, SignalStats> getAllStats() {
        checkAndResetDaily();
        return new HashMap<>(dailyStats);
    }

    public SignalStats getStats(String symbol) {
        checkAndResetDaily();
        return dailyStats.getOrDefault(symbol, new SignalStats());
    }

    public Map<String, Object> getSummary() {
        checkAndResetDaily();
        
        Map<String, Object> summary = new HashMap<>();
        int totalRaw = 0, totalEnhanced = 0, totalFiltered = 0, totalSent = 0;
        LocalDateTime lastSignal = null;
        
        for (SignalStats stats : dailyStats.values()) {
            totalRaw += stats.getRawSignalsCount();
            totalEnhanced += stats.getEnhancedSignalsCount();
            totalFiltered += stats.getFilteredSignalsCount();
            totalSent += stats.getSentSignalsCount();
            
            if (stats.getLastSignalTime() != null) {
                if (lastSignal == null || stats.getLastSignalTime().isAfter(lastSignal)) {
                    lastSignal = stats.getLastSignalTime();
                }
            }
        }
        
        summary.put("totalRawSignals", totalRaw);
        summary.put("totalEnhancedSignals", totalEnhanced);
        summary.put("totalFilteredSignals", totalFiltered);
        summary.put("totalSentSignals", totalSent);
        summary.put("overallFilterRate", totalRaw == 0 ? 0 : (1.0 - (double) totalSent / totalRaw) * 100);
        summary.put("lastSignalTime", lastSignal);
        summary.put("hoursSinceLastSignal", lastSignal == null ? null : 
            java.time.Duration.between(lastSignal, LocalDateTime.now()).toHours());
        summary.put("statsResetTime", lastResetTime);
        
        // 汇总评分分布
        Map<Integer, Integer> totalScoreDistribution = new java.util.HashMap<>();
        int qualifiedSignals = 0; // 计算≥4分的信号数
        for (SignalStats stats : dailyStats.values()) {
            for (Map.Entry<Integer, Integer> entry : stats.getScoreDistribution().entrySet()) {
                totalScoreDistribution.put(entry.getKey(), 
                    totalScoreDistribution.getOrDefault(entry.getKey(), 0) + entry.getValue());
                if (entry.getKey() >= 4) {
                    qualifiedSignals += entry.getValue();
                }
            }
        }
        summary.put("scoreDistribution", totalScoreDistribution);
        summary.put("qualifiedSignals", qualifiedSignals);
        
        return summary;
    }

    private void checkAndResetDaily() {
        LocalDateTime now = LocalDateTime.now();
        if (now.toLocalDate().isAfter(lastResetTime.toLocalDate())) {
            logger.info("每日统计重置");
            dailyStats.clear();
            lastResetTime = now;
        }
    }

    public void resetStats() {
        dailyStats.clear();
        lastResetTime = LocalDateTime.now();
        logger.info("统计数据已手动重置");
    }

    public List<String> checkAlerts() {
        List<String> alerts = new ArrayList<>();
        Map<String, Object> summary = getSummary();
        Long hoursSinceLastSignal = (Long) summary.get("hoursSinceLastSignal");
        
        if (hoursSinceLastSignal != null && hoursSinceLastSignal >= 24) {
            alerts.add(String.format("告警：已经%d小时没有收到任何信号", hoursSinceLastSignal));
        }
        
        double filterRate = (double) summary.get("overallFilterRate");
        if (filterRate > 90 && (int) summary.get("totalRawSignals") > 10) {
            alerts.add(String.format("告警：信号过滤率过高(%.1f%%)，可能需要调整阈值", filterRate));
        }
        
        if ((int) summary.get("totalRawSignals") == 0) {
            alerts.add("告警：今日没有生成任何原始信号，请检查市场数据获取是否正常");
        }
        
        return alerts;
    }
}
