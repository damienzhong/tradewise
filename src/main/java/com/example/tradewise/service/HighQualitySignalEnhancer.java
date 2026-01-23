package com.example.tradewise.service;

import com.example.tradewise.service.MarketAnalysisService.Candlestick;
import com.example.tradewise.service.MarketAnalysisService.TechnicalIndicators;
import com.example.tradewise.service.MarketAnalysisService.TradingSignal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 高质量信号增强器
 * 通过多维度验证机制，提升信号质量和准确性
 *
 * 核心策略：
 * 1. 多时间框架确认（MTF Confirmation）
 * 2. 成交量价格背离检测（Volume-Price Divergence）
 * 3. 关键支撑阻力位确认（Key Level Confirmation）
 * 4. 趋势一致性验证（Trend Alignment）
 * 5. 风险回报比优化（Risk-Reward Optimization）
 */
@Service
public class HighQualitySignalEnhancer {

    private static final Logger logger = LoggerFactory.getLogger(HighQualitySignalEnhancer.class);

    /**
     * 增强信号质量
     *
     * @param signal 原始信号
     * @param candlesticks K线数据
     * @param multiTimeframeData 多时间框架数据
     * @return 增强后的信号，如果信号不符合高质量标准则返回null
     */
    public TradingSignal enhanceSignal(TradingSignal signal, List<Candlestick> candlesticks,
                                       Map<String, List<Candlestick>> multiTimeframeData) {

        if (signal == null || candlesticks == null || candlesticks.isEmpty()) {
            return null;
        }

        logger.debug("开始增强信号: {} - {}", signal.getSymbol(), signal.getIndicator());

        // 1. 多时间框架确认
        boolean mtfConfirmed = checkMultiTimeframeConfirmation(signal, multiTimeframeData);
        if (!mtfConfirmed) {
            logger.debug("信号未通过多时间框架确认: {}", signal.getSymbol());
            return null; // 不符合高质量标准
        }

        // 2. 成交量确认
        boolean volumeConfirmed = checkVolumeConfirmation(signal, candlesticks);
        if (!volumeConfirmed) {
            logger.debug("信号未通过成交量确认: {}", signal.getSymbol());
            // 成交量不足会降低评分，但不直接拒绝
            signal.setScore(Math.max(0, signal.getScore() - 2));
        }

        // 3. 关键位置确认
        boolean keyLevelConfirmed = checkKeyLevelConfirmation(signal, candlesticks);
        if (keyLevelConfirmed) {
            // 在关键位置的信号加分
            signal.setScore(Math.min(10, signal.getScore() + 1));
            logger.debug("信号在关键位置，加分: {}", signal.getSymbol());
        }

        // 4. 趋势一致性验证
        boolean trendAligned = checkTrendAlignment(signal, candlesticks);
        if (!trendAligned) {
            logger.debug("信号与主趋势不一致: {}", signal.getSymbol());
            signal.setScore(Math.max(0, signal.getScore() - 1));
        }

        // 5. 优化风险回报比
        optimizeRiskReward(signal, candlesticks);

        // 6. 添加市场环境描述
        addMarketContextDescription(signal, candlesticks);

        // 7. 计算信号置信度
        calculateEnhancedConfidence(signal, mtfConfirmed, volumeConfirmed, keyLevelConfirmed, trendAligned);

        logger.info("信号增强完成: {} - 评分: {}, 置信度: {}",
                signal.getSymbol(), signal.getScore(), signal.getConfidence());

        return signal;
    }

    /**
     * 多时间框架确认
     * 检查更高时间框架是否支持当前信号
     */
    private boolean checkMultiTimeframeConfirmation(TradingSignal signal,
                                                    Map<String, List<Candlestick>> multiTimeframeData) {
        if (multiTimeframeData == null || multiTimeframeData.isEmpty()) {
            return true; // 如果没有多时间框架数据，默认通过
        }

        // 获取4小时和日线数据
        List<Candlestick> fourHourData = multiTimeframeData.get("4h");
        List<Candlestick> dailyData = multiTimeframeData.get("1d");

        int confirmations = 0;
        int totalChecks = 0;

        // 检查4小时趋势
        if (fourHourData != null && fourHourData.size() >= 50) {
            totalChecks++;
            if (isTrendAligned(signal, fourHourData, 50)) {
                confirmations++;
            }
        }

        // 检查日线趋势
        if (dailyData != null && dailyData.size() >= 20) {
            totalChecks++;
            if (isTrendAligned(signal, dailyData, 20)) {
                confirmations++;
            }
        }

        // 至少需要一个更高时间框架确认
        return totalChecks == 0 || confirmations > 0;
    }

    /**
     * 检查趋势是否一致
     */
    private boolean isTrendAligned(TradingSignal signal, List<Candlestick> candlesticks, int period) {
        if (candlesticks.size() < period) {
            return false;
        }

        // 计算移动平均线判断趋势
        List<Double> sma = TechnicalIndicators.calculateSMA(candlesticks, period);
        if (sma.isEmpty() || sma.get(sma.size() - 1) == null) {
            return false;
        }

        double currentPrice = candlesticks.get(candlesticks.size() - 1).getClose();
        double ma = sma.get(sma.size() - 1);

        // 买入信号需要价格在均线之上，卖出信号需要价格在均线之下
        if (signal.getSignalType() == TradingSignal.SignalType.BUY) {
            return currentPrice > ma;
        } else if (signal.getSignalType() == TradingSignal.SignalType.SELL) {
            return currentPrice < ma;
        }

        return true;
    }

    /**
     * 成交量确认
     * 检查信号是否有成交量支持
     */
    private boolean checkVolumeConfirmation(TradingSignal signal, List<Candlestick> candlesticks) {
        if (candlesticks.size() < 20) {
            return false;
        }

        // 计算平均成交量
        double avgVolume = candlesticks.stream()
                .skip(Math.max(0, candlesticks.size() - 20))
                .mapToDouble(Candlestick::getVolume)
                .average()
                .orElse(0.0);

        // 当前成交量
        double currentVolume = candlesticks.get(candlesticks.size() - 1).getVolume();

        // 成交量需要超过平均值的120%
        return currentVolume > avgVolume * 1.2;
    }

    /**
     * 关键位置确认
     * 检查信号是否在关键支撑/阻力位附近
     */
    private boolean checkKeyLevelConfirmation(TradingSignal signal, List<Candlestick> candlesticks) {
        if (candlesticks.size() < 50) {
            return false;
        }

        double currentPrice = signal.getPrice();

        // 找出最近50根K线的关键高低点
        List<Double> highs = candlesticks.stream()
                .skip(Math.max(0, candlesticks.size() - 50))
                .mapToDouble(Candlestick::getHigh)
                .boxed()
                .collect(Collectors.toList());

        List<Double> lows = candlesticks.stream()
                .skip(Math.max(0, candlesticks.size() - 50))
                .mapToDouble(Candlestick::getLow)
                .boxed()
                .collect(Collectors.toList());

        double recentHigh = highs.stream().max(Double::compareTo).orElse(currentPrice);
        double recentLow = lows.stream().min(Double::compareTo).orElse(currentPrice);

        // 计算ATR作为价格波动参考
        List<Double> atr = TechnicalIndicators.calculateATR(candlesticks, 14);
        double currentAtr = atr.isEmpty() || atr.get(atr.size() - 1) == null ?
                currentPrice * 0.02 : atr.get(atr.size() - 1);

        // 检查是否在关键位置附近（ATR的0.5倍范围内）
        boolean nearHigh = Math.abs(currentPrice - recentHigh) < currentAtr * 0.5;
        boolean nearLow = Math.abs(currentPrice - recentLow) < currentAtr * 0.5;

        // 买入信号应该在低点附近，卖出信号应该在高点附近
        if (signal.getSignalType() == TradingSignal.SignalType.BUY) {
            return nearLow;
        } else if (signal.getSignalType() == TradingSignal.SignalType.SELL) {
            return nearHigh;
        }

        return false;
    }

    /**
     * 趋势一致性验证
     */
    private boolean checkTrendAlignment(TradingSignal signal, List<Candlestick> candlesticks) {
        if (candlesticks.size() < 50) {
            return true; // 数据不足时默认通过
        }

        // 使用50周期SMA判断主趋势
        List<Double> sma50 = TechnicalIndicators.calculateSMA(candlesticks, 50);
        if (sma50.isEmpty() || sma50.get(sma50.size() - 1) == null) {
            return true;
        }

        double currentPrice = candlesticks.get(candlesticks.size() - 1).getClose();
        double ma50 = sma50.get(sma50.size() - 1);

        // 买入信号应该在上升趋势中，卖出信号应该在下降趋势中
        if (signal.getSignalType() == TradingSignal.SignalType.BUY) {
            return currentPrice > ma50;
        } else if (signal.getSignalType() == TradingSignal.SignalType.SELL) {
            return currentPrice < ma50;
        }

        return true;
    }

    /**
     * 优化风险回报比
     * 根据市场结构调整止损止盈位置
     */
    private void optimizeRiskReward(TradingSignal signal, List<Candlestick> candlesticks) {
        if (candlesticks.size() < 20) {
            return;
        }

        double currentPrice = signal.getPrice();

        // 计算ATR
        List<Double> atr = TechnicalIndicators.calculateATR(candlesticks, 14);
        double currentAtr = atr.isEmpty() || atr.get(atr.size() - 1) == null ?
                currentPrice * 0.02 : atr.get(atr.size() - 1);

        // 找出最近的支撑阻力位
        List<Double> recentHighs = candlesticks.stream()
                .skip(Math.max(0, candlesticks.size() - 20))
                .mapToDouble(Candlestick::getHigh)
                .boxed()
                .collect(Collectors.toList());

        List<Double> recentLows = candlesticks.stream()
                .skip(Math.max(0, candlesticks.size() - 20))
                .mapToDouble(Candlestick::getLow)
                .boxed()
                .collect(Collectors.toList());

        double recentHigh = recentHighs.stream().max(Double::compareTo).orElse(currentPrice);
        double recentLow = recentLows.stream().min(Double::compareTo).orElse(currentPrice);

        if (signal.getSignalType() == TradingSignal.SignalType.BUY) {
            // 买入信号：止损设在最近低点下方，止盈设在最近高点上方
            double stopLoss = Math.min(recentLow - currentAtr * 0.5, currentPrice - currentAtr * 1.5);
            double takeProfit = Math.max(recentHigh + currentAtr * 0.5, currentPrice + currentAtr * 3.0);

            signal.setStopLoss(stopLoss);
            signal.setTakeProfit(takeProfit);

        } else if (signal.getSignalType() == TradingSignal.SignalType.SELL) {
            // 卖出信号：止损设在最近高点上方，止盈设在最近低点下方
            double stopLoss = Math.max(recentHigh + currentAtr * 0.5, currentPrice + currentAtr * 1.5);
            double takeProfit = Math.min(recentLow - currentAtr * 0.5, currentPrice - currentAtr * 3.0);

            signal.setStopLoss(stopLoss);
            signal.setTakeProfit(takeProfit);
        }

        // 计算风险回报比
        double risk = Math.abs(currentPrice - signal.getStopLoss());
        double reward = Math.abs(signal.getTakeProfit() - currentPrice);
        double riskRewardRatio = risk > 0 ? reward / risk : 0;

        // 将风险回报比添加到信号说明中
        String updatedSuggestion = signal.getSuggestion() +
                String.format("\n风险回报比: 1:%.2f", riskRewardRatio);
        signal.setSuggestion(updatedSuggestion);
    }

    /**
     * 添加市场环境描述
     */
    private void addMarketContextDescription(TradingSignal signal, List<Candlestick> candlesticks) {
        if (candlesticks.size() < 50) {
            return;
        }

        // 计算市场波动性
        List<Double> atr = TechnicalIndicators.calculateATR(candlesticks, 14);
        double currentAtr = atr.isEmpty() || atr.get(atr.size() - 1) == null ?
                0 : atr.get(atr.size() - 1);
        double currentPrice = candlesticks.get(candlesticks.size() - 1).getClose();
        double volatilityPercent = currentPrice > 0 ? (currentAtr / currentPrice) * 100 : 0;

        // 计算趋势强度
        List<Double> sma20 = TechnicalIndicators.calculateSMA(candlesticks, 20);
        List<Double> sma50 = TechnicalIndicators.calculateSMA(candlesticks, 50);

        String trendDescription = "震荡";
        if (sma20.size() > 0 && sma50.size() > 0 &&
                sma20.get(sma20.size() - 1) != null && sma50.get(sma50.size() - 1) != null) {
            double ma20 = sma20.get(sma20.size() - 1);
            double ma50 = sma50.get(sma50.size() - 1);

            if (ma20 > ma50 * 1.02) {
                trendDescription = "强势上涨";
            } else if (ma20 > ma50) {
                trendDescription = "温和上涨";
            } else if (ma20 < ma50 * 0.98) {
                trendDescription = "强势下跌";
            } else if (ma20 < ma50) {
                trendDescription = "温和下跌";
            }
        }

        // 更新原因说明
        String enhancedReason = signal.getReason() +
                String.format("\n市场环境: %s, 波动率: %.2f%%", trendDescription, volatilityPercent);
        signal.setReason(enhancedReason);
    }

    /**
     * 计算增强后的置信度
     */
    private void calculateEnhancedConfidence(TradingSignal signal, boolean mtfConfirmed,
                                             boolean volumeConfirmed, boolean keyLevelConfirmed,
                                             boolean trendAligned) {
        int confirmations = 0;
        if (mtfConfirmed) confirmations++;
        if (volumeConfirmed) confirmations++;
        if (keyLevelConfirmed) confirmations++;
        if (trendAligned) confirmations++;

        // 根据确认数量设置置信度
        if (confirmations >= 4) {
            signal.setConfidence("极高");
        } else if (confirmations >= 3) {
            signal.setConfidence("高");
        } else if (confirmations >= 2) {
            signal.setConfidence("中");
        } else {
            signal.setConfidence("低");
        }
    }

    /**
     * 批量增强信号
     */
    public List<TradingSignal> enhanceSignals(List<TradingSignal> signals,
                                              Map<String, List<Candlestick>> candlesticksMap,
                                              Map<String, Map<String, List<Candlestick>>> multiTimeframeDataMap) {

        List<TradingSignal> enhancedSignals = new ArrayList<>();

        for (TradingSignal signal : signals) {
            List<Candlestick> candlesticks = candlesticksMap.get(signal.getSymbol());
            Map<String, List<Candlestick>> mtfData = multiTimeframeDataMap.get(signal.getSymbol());

            TradingSignal enhanced = enhanceSignal(signal, candlesticks, mtfData);
            if (enhanced != null && enhanced.getScore() >= 6) { // 只保留评分>=6的信号
                enhancedSignals.add(enhanced);
            }
        }

        logger.info("信号增强完成: 输入{}个，输出{}个高质量信号", signals.size(), enhancedSignals.size());

        return enhancedSignals;
    }
}
