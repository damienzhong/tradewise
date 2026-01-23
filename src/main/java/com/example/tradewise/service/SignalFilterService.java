package com.example.tradewise.service;

import com.example.tradewise.service.MarketAnalysisService.TradingSignal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 信号过滤服务 - 解决信号过载问题
 * 功能：
 * 1. 只发送高质量信号（LEVEL_1和LEVEL_2）
 * 2. 同一交易对的信号去重和冷却
 * 3. 每日信号数量限制
 * 4. 低优先级信号缓存，用于每日摘要
 */
@Service
public class SignalFilterService {

    private static final Logger logger = LoggerFactory.getLogger(SignalFilterService.class);

    // 信号冷却时间（分钟）
    private static final int LEVEL_1_COOLDOWN_MINUTES = 120; // 2小时
    private static final int LEVEL_2_COOLDOWN_MINUTES = 60;  // 1小时
    private static final int LEVEL_3_COOLDOWN_MINUTES = 240; // 4小时

    // 每日最大信号数量
    private static final int MAX_SIGNALS_PER_DAY = 20;

    // 记录最后发送时间
    private final Map<String, LocalDateTime> lastSignalTime = new ConcurrentHashMap<>();

    // 记录今日已发送信号数量
    private int todaySignalCount = 0;
    private LocalDateTime lastResetDate = LocalDateTime.now();

    // 缓存低优先级信号用于每日摘要
    private final List<TradingSignal> lowPrioritySignals = Collections.synchronizedList(new ArrayList<>());

    /**
     * 过滤信号 - 只返回应该立即发送的高质量信号
     */
    public List<TradingSignal> filterSignalsForImmediateSend(List<TradingSignal> signals) {
        resetDailyCountIfNeeded();

        List<TradingSignal> filteredSignals = new ArrayList<>();

        for (TradingSignal signal : signals) {
            // 检查是否达到每日限制
            if (todaySignalCount >= MAX_SIGNALS_PER_DAY) {
                logger.info("已达到每日信号发送上限({}个)，信号将被缓存到每日摘要", MAX_SIGNALS_PER_DAY);
                lowPrioritySignals.add(signal);
                continue;
            }

            // 只处理LEVEL_1和LEVEL_2信号
            String signalLevel = signal.getSignalLevel();
            if (!"LEVEL_1".equals(signalLevel) && !"LEVEL_2".equals(signalLevel)) {
                // LEVEL_3信号缓存到每日摘要
                lowPrioritySignals.add(signal);
                logger.debug("LEVEL_3信号已缓存到每日摘要: {} - {}", signal.getSymbol(), signal.getIndicator());
                continue;
            }

            // 检查冷却时间
            if (!isSignalAllowed(signal)) {
                logger.debug("信号在冷却期内，跳过: {} - {}", signal.getSymbol(), signal.getIndicator());
                continue;
            }

            // 通过所有过滤条件
            filteredSignals.add(signal);
            updateSignalTime(signal);
            todaySignalCount++;

            logger.info("信号通过过滤: {} - {} (级别: {}, 评分: {})",
                    signal.getSymbol(), signal.getIndicator(), signalLevel, signal.getScore());
        }

        logger.info("信号过滤完成: 输入{}个，输出{}个，缓存{}个",
                signals.size(), filteredSignals.size(), lowPrioritySignals.size());

        return filteredSignals;
    }

    /**
     * 检查信号是否允许发送（冷却机制）
     */
    private boolean isSignalAllowed(TradingSignal signal) {
        String key = getSignalKey(signal);
        LocalDateTime lastTime = lastSignalTime.get(key);

        if (lastTime == null) {
            return true; // 首次信号
        }

        // 根据信号级别确定冷却时间
        int cooldownMinutes;
        String signalLevel = signal.getSignalLevel();

        if ("LEVEL_1".equals(signalLevel)) {
            cooldownMinutes = LEVEL_1_COOLDOWN_MINUTES;
        } else if ("LEVEL_2".equals(signalLevel)) {
            cooldownMinutes = LEVEL_2_COOLDOWN_MINUTES;
        } else {
            cooldownMinutes = LEVEL_3_COOLDOWN_MINUTES;
        }

        Duration duration = Duration.between(lastTime, LocalDateTime.now());
        return duration.toMinutes() >= cooldownMinutes;
    }

    /**
     * 更新信号发送时间
     */
    private void updateSignalTime(TradingSignal signal) {
        String key = getSignalKey(signal);
        lastSignalTime.put(key, LocalDateTime.now());
    }

    /**
     * 生成信号唯一键
     */
    private String getSignalKey(TradingSignal signal) {
        return signal.getSymbol() + "_" + signal.getSignalType();
    }

    /**
     * 重置每日计数
     */
    private void resetDailyCountIfNeeded() {
        LocalDateTime now = LocalDateTime.now();
        if (now.toLocalDate().isAfter(lastResetDate.toLocalDate())) {
            todaySignalCount = 0;
            lastResetDate = now;
            logger.info("每日信号计数已重置");
        }
    }

    /**
     * 获取低优先级信号用于每日摘要
     */
    public List<TradingSignal> getLowPrioritySignals() {
        return new ArrayList<>(lowPrioritySignals);
    }

    /**
     * 清空低优先级信号缓存
     */
    public void clearLowPrioritySignals() {
        lowPrioritySignals.clear();
        logger.info("低优先级信号缓存已清空");
    }

    /**
     * 获取今日已发送信号数量
     */
    public int getTodaySignalCount() {
        resetDailyCountIfNeeded();
        return todaySignalCount;
    }

    /**
     * 获取信号统计信息
     */
    public Map<String, Object> getStatistics() {
        resetDailyCountIfNeeded();

        Map<String, Object> stats = new HashMap<>();
        stats.put("todaySignalCount", todaySignalCount);
        stats.put("maxSignalsPerDay", MAX_SIGNALS_PER_DAY);
        stats.put("remainingQuota", MAX_SIGNALS_PER_DAY - todaySignalCount);
        stats.put("lowPrioritySignalsCount", lowPrioritySignals.size());
        stats.put("lastResetDate", lastResetDate);

        return stats;
    }
}
