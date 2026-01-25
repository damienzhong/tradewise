package com.example.tradewise.scheduler;

import com.example.tradewise.entity.Signal;
import com.example.tradewise.mapper.SignalMapper;
import com.example.tradewise.service.MarketAnalysisService;
import com.example.tradewise.service.MarketDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 信号追踪调度器
 * 定时检查活跃信号的价格变化，自动更新信号状态和盈亏
 */
@Component
public class SignalTrackingScheduler {

    private static final Logger logger = LoggerFactory.getLogger(SignalTrackingScheduler.class);

    @Autowired
    private SignalMapper signalMapper;

    @Autowired
    private MarketDataService marketDataService;

    /**
     * 每分钟检查一次活跃信号
     */
    @Scheduled(fixedRate = 60000) // 每60秒执行一次
    public void trackActiveSignals() {
        try {
            // 获取所有活跃状态的信号
            List<Signal> activeSignals = signalMapper.findByConditions(
                null, null, "ACTIVE", null, null, 0, 1000);

            if (activeSignals.isEmpty()) {
                logger.debug("当前没有活跃信号需要追踪");
                return;
            }

            logger.info("开始追踪 {} 个活跃信号", activeSignals.size());
            int updatedCount = 0;

            for (Signal signal : activeSignals) {
                try {
                    // 获取当前价格
                    BigDecimal currentPrice = getCurrentPrice(signal.getSymbol());
                    if (currentPrice == null) {
                        continue;
                    }

                    // 检查止损止盈
                    boolean shouldClose = checkStopLossOrTakeProfit(signal, currentPrice);
                    
                    // 检查信号是否过期（24小时后自动过期）
                    boolean isExpired = checkExpiration(signal);

                    if (shouldClose || isExpired) {
                        updateSignalOutcome(signal, currentPrice, isExpired);
                        updatedCount++;
                    }

                } catch (Exception e) {
                    logger.error("追踪信号失败: {} - {}", signal.getSymbol(), signal.getId(), e);
                }
            }

            if (updatedCount > 0) {
                logger.info("更新了 {} 个信号的状态", updatedCount);
            }

        } catch (Exception e) {
            logger.error("信号追踪任务执行失败", e);
        }
    }

    /**
     * 获取当前价格
     */
    private BigDecimal getCurrentPrice(String symbol) {
        try {
            List<MarketAnalysisService.Candlestick> klines =
                marketDataService.getKlines(symbol, "1m", 1);
            
            if (!klines.isEmpty()) {
                return BigDecimal.valueOf(klines.get(0).getClose());
            }
        } catch (Exception e) {
            logger.warn("获取 {} 当前价格失败", symbol, e);
        }
        return null;
    }

    /**
     * 检查是否触发止损或止盈
     */
    private boolean checkStopLossOrTakeProfit(Signal signal, BigDecimal currentPrice) {
        if (signal.getStopLoss() == null && signal.getTakeProfit() == null) {
            return false;
        }

        boolean isBuy = "BUY".equals(signal.getSignalType());

        // 检查止损
        if (signal.getStopLoss() != null) {
            if (isBuy && currentPrice.compareTo(signal.getStopLoss()) <= 0) {
                logger.info("信号 {} 触发止损: 当前价格 {} <= 止损价 {}", 
                    signal.getId(), currentPrice, signal.getStopLoss());
                return true;
            } else if (!isBuy && currentPrice.compareTo(signal.getStopLoss()) >= 0) {
                logger.info("信号 {} 触发止损: 当前价格 {} >= 止损价 {}", 
                    signal.getId(), currentPrice, signal.getStopLoss());
                return true;
            }
        }

        // 检查止盈
        if (signal.getTakeProfit() != null) {
            if (isBuy && currentPrice.compareTo(signal.getTakeProfit()) >= 0) {
                logger.info("信号 {} 触发止盈: 当前价格 {} >= 止盈价 {}", 
                    signal.getId(), currentPrice, signal.getTakeProfit());
                return true;
            } else if (!isBuy && currentPrice.compareTo(signal.getTakeProfit()) <= 0) {
                logger.info("信号 {} 触发止盈: 当前价格 {} <= 止盈价 {}", 
                    signal.getId(), currentPrice, signal.getTakeProfit());
                return true;
            }
        }

        return false;
    }

    /**
     * 检查信号是否过期
     */
    private boolean checkExpiration(Signal signal) {
        LocalDateTime expirationTime = signal.getSignalTime().plusHours(24);
        boolean isExpired = LocalDateTime.now().isAfter(expirationTime);
        
        if (isExpired) {
            logger.info("信号 {} 已过期: 生成时间 {}", signal.getId(), signal.getSignalTime());
        }
        
        return isExpired;
    }

    /**
     * 更新信号结果
     */
    private void updateSignalOutcome(Signal signal, BigDecimal finalPrice, boolean isExpired) {
        signal.setStatus(isExpired ? "EXPIRED" : "CLOSED");
        signal.setOutcomeTime(LocalDateTime.now());
        signal.setFinalPrice(finalPrice);
        
        // 计算盈亏百分比
        BigDecimal pnlPercentage = calculatePnl(signal.getPrice(), finalPrice, signal.getSignalType());
        signal.setPnlPercentage(pnlPercentage);
        
        // 设置备注
        String notes = isExpired ? "信号已过期自动关闭" : 
            (pnlPercentage.compareTo(BigDecimal.ZERO) > 0 ? "触发止盈" : "触发止损");
        signal.setNotes(notes);
        
        signalMapper.updateOutcome(signal);
        
        logger.info("信号 {} 已更新: 状态={}, 盈亏={}%", 
            signal.getId(), signal.getStatus(), pnlPercentage);
    }

    /**
     * 计算盈亏百分比
     */
    private BigDecimal calculatePnl(BigDecimal entryPrice, BigDecimal exitPrice, String signalType) {
        if (entryPrice == null || exitPrice == null || entryPrice.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal priceDiff = exitPrice.subtract(entryPrice);
        BigDecimal pnl = priceDiff.divide(entryPrice, 4, BigDecimal.ROUND_HALF_UP)
                                   .multiply(new BigDecimal("100"));
        
        // 如果是卖出信号，盈亏方向相反
        if ("SELL".equals(signalType)) {
            pnl = pnl.negate();
        }
        
        return pnl;
    }
}
