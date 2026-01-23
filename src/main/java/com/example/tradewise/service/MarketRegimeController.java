package com.example.tradewise.service;

import com.example.tradewise.service.MarketAnalysisService.Candlestick;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 市场状态总控器
 * 用于识别当前市场所处的状态，以决定哪些信号模型可以参与评分
 */
@Component
public class MarketRegimeController {
    
    private static final Logger logger = LoggerFactory.getLogger(MarketRegimeController.class);
    
    /**
     * 市场状态枚举
     */
    public enum MarketRegime {
        STRONG_TREND,        // 强趋势
        WEAK_TREND,          // 弱趋势
        RANGE,               // 震荡区间
        SQUEEZE,             // 波动率压缩
        VOLATILITY_EXPANSION // 波动率扩张
    }
    
    /**
     * 检测当前市场状态
     * 
     * @param symbol 交易对
     * @param candles K线数据
     * @return 当前市场状态
     */
    public MarketRegime detect(String symbol, List<Candlestick> candles) {
        if (candles.size() < 50) {
            logger.warn("K线数据不足，返回默认状态：RANGE");
            return MarketRegime.RANGE; // 数据不足时返回震荡状态
        }
        
        // 计算各种市场特征指标
        boolean isStrongTrend = isStrongTrend(candles);
        boolean isWeakTrend = isWeakTrend(candles);
        boolean isRange = isRange(candles);
        boolean isSqueeze = isSqueeze(candles);
        boolean isVolatilityExpansion = isVolatilityExpansion(candles);
        
        // 根据特征判断市场状态
        if (isSqueeze) {
            logger.debug("{} 处于波动率压缩状态", symbol);
            return MarketRegime.SQUEEZE;
        } else if (isVolatilityExpansion) {
            logger.debug("{} 处于波动率扩张状态", symbol);
            return MarketRegime.VOLATILITY_EXPANSION;
        } else if (isStrongTrend) {
            logger.debug("{} 处于强趋势状态", symbol);
            return MarketRegime.STRONG_TREND;
        } else if (isWeakTrend) {
            logger.debug("{} 处于弱趋势状态", symbol);
            return MarketRegime.WEAK_TREND;
        } else if (isRange) {
            logger.debug("{} 处于震荡区间状态", symbol);
            return MarketRegime.RANGE;
        } else {
            logger.debug("{} 处于震荡区间状态（默认）", symbol);
            return MarketRegime.RANGE;
        }
    }
    
    /**
     * 检测是否为强趋势
     */
    private boolean isStrongTrend(List<Candlestick> candles) {
        // 检查价格偏离均线的程度
        List<Double> sma50 = MarketAnalysisService.TechnicalIndicators.calculateSMA(candles, 50);
        List<Double> sma20 = MarketAnalysisService.TechnicalIndicators.calculateSMA(candles, 20);
        
        if (sma50.isEmpty() || sma20.isEmpty()) {
            return false;
        }
        
        Double latestSma50 = sma50.get(sma50.size() - 1);
        Double latestSma20 = sma20.get(sma20.size() - 1);
        double latestPrice = candles.get(candles.size() - 1).getClose();
        
        if (latestSma50 == null || latestSma20 == null) {
            return false;
        }
        
        // 检查价格与均线的偏离程度
        double deviationFromSma50 = Math.abs(latestPrice - latestSma50) / latestSma50;
        double deviationFromSma20 = Math.abs(latestPrice - latestSma20) / latestSma20;
        
        // 如果价格偏离长期均线超过3%，且短期均线也明显偏离，认为是强趋势
        return deviationFromSma50 > 0.03 && deviationFromSma20 > 0.02;
    }
    
    /**
     * 检测是否为弱趋势
     */
    private boolean isWeakTrend(List<Candlestick> candles) {
        // 检查趋势强度但不满足强趋势条件
        List<Double> sma50 = MarketAnalysisService.TechnicalIndicators.calculateSMA(candles, 50);
        List<Double> sma20 = MarketAnalysisService.TechnicalIndicators.calculateSMA(candles, 20);
        
        if (sma50.isEmpty() || sma20.isEmpty()) {
            return false;
        }
        
        Double latestSma50 = sma50.get(sma50.size() - 1);
        Double latestSma20 = sma20.get(sma20.size() - 1);
        double latestPrice = candles.get(candles.size() - 1).getClose();
        
        if (latestSma50 == null || latestSma20 == null) {
            return false;
        }
        
        // 检查价格与均线的偏离程度（介于震荡和强趋势之间）
        double deviationFromSma50 = Math.abs(latestPrice - latestSma50) / latestSma50;
        double deviationFromSma20 = Math.abs(latestPrice - latestSma20) / latestSma20;
        
        return deviationFromSma50 > 0.015 && deviationFromSma20 > 0.01;
    }
    
    /**
     * 检测是否为震荡区间
     */
    private boolean isRange(List<Candlestick> candles) {
        // 检查价格是否在一定范围内震荡
        int lookback = Math.min(50, candles.size());
        List<Candlestick> recentCandles = candles.subList(candles.size() - lookback, candles.size());
        
        // 计算近期的最高价和最低价
        double highest = recentCandles.stream().mapToDouble(Candlestick::getHigh).max().orElse(0);
        double lowest = recentCandles.stream().mapToDouble(Candlestick::getLow).min().orElse(0);
        
        // 计算价格波动范围
        double range = (highest - lowest) / lowest;
        
        // 如果波动范围较小，认为是震荡
        return range < 0.04; // 4%以内的波动认为是震荡
    }
    
    /**
     * 检测是否为波动率压缩
     */
    private boolean isSqueeze(List<Candlestick> candles) {
        // 检查ATR是否处于近期低位，表示波动率压缩
        List<Double> atrList = MarketAnalysisService.TechnicalIndicators.calculateATR(candles, 14);
        
        if (atrList.size() < 50) {
            return false;
        }
        
        // 获取最近50根K线的ATR值
        List<Double> recentAtr = atrList.subList(Math.max(0, atrList.size() - 50), atrList.size());
        
        Double currentAtr = recentAtr.get(recentAtr.size() - 1);
        if (currentAtr == null) {
            return false;
        }
        
        // 计算近期ATR的平均值和最小值
        double avgAtr = recentAtr.stream()
                .filter(a -> a != null)
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0);
        double minAtr = recentAtr.stream()
                .filter(a -> a != null)
                .mapToDouble(Double::doubleValue)
                .min()
                .orElse(0);
        
        // 如果当前ATR接近近期最小值，认为是波动率压缩
        return currentAtr <= minAtr * 1.2 && minAtr > 0 && avgAtr > 0;
    }
    
    /**
     * 检测是否为波动率扩张
     */
    private boolean isVolatilityExpansion(List<Candlestick> candles) {
        // 检查ATR是否快速上升，表示波动率扩张
        List<Double> atrList = MarketAnalysisService.TechnicalIndicators.calculateATR(candles, 14);
        
        if (atrList.size() < 10) {
            return false;
        }
        
        // 比较最近几根K线的ATR与历史平均水平
        int recentCount = Math.min(5, atrList.size());
        List<Double> recentAtr = atrList.subList(atrList.size() - recentCount, atrList.size());
        List<Double> olderAtr = atrList.subList(Math.max(0, atrList.size() - 20), atrList.size() - recentCount);
        
        if (recentAtr.isEmpty() || olderAtr.isEmpty()) {
            return false;
        }
        
        double recentAvg = recentAtr.stream()
                .filter(a -> a != null)
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0);
        double olderAvg = olderAtr.stream()
                .filter(a -> a != null)
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0);
        
        // 如果近期ATR比历史平均大幅上升，认为是波动率扩张
        return recentAvg > olderAvg * 1.5 && olderAvg > 0;
    }
    
    /**
     * 根据市场状态判断是否允许某种信号模型参与评分
     * 
     * @param regime 当前市场状态
     * @param model 信号模型类型
     * @return 是否允许该模型参与评分
     */
    public boolean isModelAllowed(MarketRegime regime, SignalModel model) {
        switch (regime) {
            case STRONG_TREND:
                // 强趋势状态下，允许趋势和结构模型
                return model == SignalModel.TREND_MOMENTUM_RESONANCE || 
                       model == SignalModel.KEY_LEVEL_BATTLEGROUNDS ||
                       model == SignalModel.INSTITUTIONAL_FLOW;
            case WEAK_TREND:
                // 弱趋势状态下，允许趋势和动量模型
                return model == SignalModel.TREND_MOMENTUM_RESONANCE ||
                       model == SignalModel.VOLATILITY_BREAKOUT;
            case RANGE:
                // 震荡状态下，允许反转和关键位置模型
                return model == SignalModel.KEY_LEVEL_BATTLEGROUNDS ||
                       model == SignalModel.CORRELATION_ARBITRAGE;
            case SQUEEZE:
                // 波动率压缩状态下，允许波动率突破模型
                return model == SignalModel.VOLATILITY_BREAKOUT ||
                       model == SignalModel.SENTIMENT_EXTREMES;
            case VOLATILITY_EXPANSION:
                // 波动率扩张状态下，允许顺势模型
                return model == SignalModel.TREND_MOMENTUM_RESONANCE ||
                       model == SignalModel.INSTITUTIONAL_FLOW;
            default:
                // 默认情况下允许所有模型
                return true;
        }
    }
    
    /**
     * 信号模型枚举
     */
    public enum SignalModel {
        TREND_MOMENTUM_RESONANCE,    // 趋势动量共振模型
        INSTITUTIONAL_FLOW,          // 机构资金流向模型
        VOLATILITY_BREAKOUT,         // 波动率结构突破模型
        KEY_LEVEL_BATTLEGROUNDS,     // 关键位置博弈模型
        SENTIMENT_EXTREMES,          // 市场情绪极端模型
        CORRELATION_ARBITRAGE        // 相关性套利模型
    }
}