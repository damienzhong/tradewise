package com.example.tradewise.service;

import com.example.tradewise.service.MarketAnalysisService.Candlestick;
import com.example.tradewise.service.MarketAnalysisService.TradingSignal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 信号验证器
 * 对生成的信号进行有效性验证，确保信号质量
 */
@Component
public class SignalValidator {
    
    private static final Logger logger = LoggerFactory.getLogger(SignalValidator.class);
    
    /**
     * 验证信号的有效性
     * 
     * @param signal 待验证的交易信号
     * @param candlesticks K线数据
     * @return 信号是否有效
     */
    public boolean validateSignal(TradingSignal signal, List<Candlestick> candlesticks) {
        if (candlesticks == null || candlesticks.size() < 5) {
            logger.warn("K线数据不足，无法验证信号: {}", signal.getSymbol());
            return false;
        }
        
        // 1. 价格行为确认：检查是否有K线形态支持
        boolean priceActionConfirmed = checkPriceActionConfirmation(signal, candlesticks);
        
        // 2. 成交量验证：检查是否伴随成交量放大
        boolean volumeConfirmed = checkVolumeConfirmation(signal, candlesticks);
        
        // 3. 时间确认：检查信号是否在合理的时间点
        boolean timeValid = checkTimeValidity(signal);
        
        // 4. 结构验证：检查是否符合当前市场结构
        boolean structureValid = checkStructureValidity(signal, candlesticks);
        
        // 5. 信号强度验证：检查信号评分是否达到阈值
        boolean strengthValid = checkSignalStrength(signal);
        
        boolean isValid = priceActionConfirmed && volumeConfirmed && timeValid && structureValid && strengthValid;
        
        if (!isValid) {
            logger.debug("信号 {} 未通过验证 - 价格行为: {}, 成交量: {}, 时间: {}, 结构: {}, 强度: {}", 
                signal.getSymbol(), priceActionConfirmed, volumeConfirmed, timeValid, structureValid, strengthValid);
        }
        
        return isValid;
    }
    
    /**
     * 检查价格行为确认
     */
    private boolean checkPriceActionConfirmation(TradingSignal signal, List<Candlestick> candlesticks) {
        if (candlesticks.size() < 3) {
            return true; // 数据不足时默认通过
        }
        
        // 获取最近的K线
        Candlestick latestCandle = candlesticks.get(candlesticks.size() - 1);
        Candlestick prevCandle = candlesticks.get(candlesticks.size() - 2);
        
        // 检查K线形态是否支持信号方向
        if (signal.getSignalType() == TradingSignal.SignalType.BUY) {
            // 买入信号：检查是否出现看涨K线形态或价格行为
            boolean bullishCandle = latestCandle.getClose() > latestCandle.getOpen(); // 阳线
            boolean priceAbovePrevious = latestCandle.getClose() > prevCandle.getClose(); // 价格高于前一根K线
            
            return bullishCandle || priceAbovePrevious;
        } else if (signal.getSignalType() == TradingSignal.SignalType.SELL) {
            // 卖出信号：检查是否出现看跌K线形态或价格行为
            boolean bearishCandle = latestCandle.getClose() < latestCandle.getOpen(); // 阴线
            boolean priceBelowPrevious = latestCandle.getClose() < prevCandle.getClose(); // 价格低于前一根K线
            
            return bearishCandle || priceBelowPrevious;
        }
        
        return true; // HOLD信号默认通过
    }
    
    /**
     * 检查成交量确认
     */
    private boolean checkVolumeConfirmation(TradingSignal signal, List<Candlestick> candlesticks) {
        if (candlesticks.size() < 20) {
            return true; // 数据不足时默认通过
        }
        
        // 计算近期平均成交量
        double recentAvgVolume = candlesticks.subList(Math.max(0, candlesticks.size() - 20), candlesticks.size())
                .stream()
                .mapToDouble(Candlestick::getVolume)
                .average()
                .orElse(1.0);
        
        // 获取当前成交量
        double currentVolume = candlesticks.get(candlesticks.size() - 1).getVolume();
        
        // 检查当前成交量是否显著放大（至少是平均成交量的1.2倍）
        return currentVolume >= recentAvgVolume * 1.2;
    }
    
    /**
     * 检查时间有效性
     */
    private boolean checkTimeValidity(TradingSignal signal) {
        // 检查信号是否在合理的市场时段（避免休市时间的异常信号）
        // 这里可以集成经济日历过滤器来检查是否在重大新闻事件期间
        return true; // 在MarketAnalysisService中已经集成了经济日历过滤，这里简化处理
    }
    
    /**
     * 检查结构有效性
     */
    private boolean checkStructureValidity(TradingSignal signal, List<Candlestick> candlesticks) {
        if (candlesticks.size() < 50) {
            return true; // 数据不足时默认通过
        }
        
        // 计算长期趋势（使用50周期均线判断）
        List<Double> longTermMA = MarketAnalysisService.TechnicalIndicators.calculateSMA(candlesticks, 50);
        Double latestLongTermMA = longTermMA.get(longTermMA.size() - 1);
        if (latestLongTermMA == null) {
            return true; // 计算失败时默认通过
        }
        
        double currentPrice = candlesticks.get(candlesticks.size() - 1).getClose();
        boolean isLongTermBullish = currentPrice > latestLongTermMA;
        
        // 检查信号方向是否与长期趋势一致（趋势跟随策略）
        boolean isSignalBullish = signal.getSignalType() == TradingSignal.SignalType.BUY;
        
        // 在趋势市场中，顺趋势信号更有效
        if (isLongTermBullish && isSignalBullish) {
            return true; // 顺长期趋势的买入信号
        } else if (!isLongTermBullish && !isSignalBullish) {
            return true; // 顺长期趋势的卖出信号
        }
        
        // 逆趋势信号需要更高的确认度
        return signal.getScore() >= 7; // 评分7分以上才允许逆趋势信号
    }
    
    /**
     * 检查信号强度
     */
    private boolean checkSignalStrength(TradingSignal signal) {
        // 检查信号评分是否达到最低要求
        return signal.getScore() >= 4; // 最低4分才能通过验证
    }
    
    /**
     * 计算信号置信度
     */
    public double calculateSignalConfidence(TradingSignal signal, List<Candlestick> candlesticks) {
        if (!validateSignal(signal, candlesticks)) {
            return 0.0; // 未通过验证的信号置信度为0
        }
        
        // 基础置信度基于评分
        double baseConfidence = signal.getScore() / 10.0;
        
        // 根据验证结果调整置信度
        boolean priceActionConfirmed = checkPriceActionConfirmation(signal, candlesticks);
        boolean volumeConfirmed = checkVolumeConfirmation(signal, candlesticks);
        boolean structureValid = checkStructureValidity(signal, candlesticks);
        
        double adjustment = 1.0;
        if (priceActionConfirmed) adjustment *= 1.1;
        if (volumeConfirmed) adjustment *= 1.1;
        if (structureValid) adjustment *= 1.1;
        
        // 避免置信度过高
        return Math.min(1.0, baseConfidence * adjustment);
    }
}