package com.example.tradewise.service;

import com.example.tradewise.service.MarketAnalysisService.Candlestick;
import com.example.tradewise.service.MarketAnalysisService.TechnicalIndicators;
import com.example.tradewise.service.SignalFusionEngine.TradingSignal;
import com.example.tradewise.service.SignalFusionEngine.SignalModel;
import com.example.tradewise.service.SignalFusionEngine.Decision;

import java.util.*;

/**
 * 趋势动量共振模型（增强版）
 * 实现DeepSeek方案中的模型1：趋势动量共振模型
 */
public class TrendMomentumResonanceModel {
    
    /**
     * 检测趋势动量共振信号
     */
    public Optional<TradingSignal> detect(String symbol, Map<String, List<Candlestick>> multiTimeframeData) {
        // 获取不同时间框架的数据
        List<Candlestick> hourlyData = multiTimeframeData.get("1h");
        List<Candlestick> fifteenMinData = multiTimeframeData.get("15m");
        List<Candlestick> fiveMinData = multiTimeframeData.get("5m");
        
        if (hourlyData == null || fifteenMinData == null || fiveMinData == null) {
            return Optional.empty();
        }
        
        // Step 1: 1小时趋势确认
        boolean hourlyTrendConfirmed = confirmHourlyTrend(hourlyData);
        
        // Step 2: 15分钟动量确认
        boolean fifteenMinMomentumConfirmed = confirmFifteenMinMomentum(fifteenMinData);
        
        // Step 3: 5分钟入场时机
        boolean fiveMinEntryOpportunity = confirmFiveMinEntry(fiveMinData);
        
        // 计算共振强度
        int resonanceCount = 0;
        List<String> confirmationFactors = new ArrayList<>();
        
        if (hourlyTrendConfirmed) {
            resonanceCount++;
            confirmationFactors.add("1小时趋势确认");
        }
        
        if (fifteenMinMomentumConfirmed) {
            resonanceCount++;
            confirmationFactors.add("15分钟动量确认");
        }
        
        if (fiveMinEntryOpportunity) {
            resonanceCount++;
            confirmationFactors.add("5分钟入场时机");
        }
        
        if (resonanceCount == 0) {
            return Optional.empty(); // 没有共振信号
        }
        
        // 确定方向
        Decision direction = determineDirection(hourlyData, fifteenMinData, fiveMinData);
        
        // 计算信号强度
        double strength = calculateResonanceStrength(resonanceCount);
        
        // 生成信号原因
        String reason = String.format("趋势动量共振检测: %d个时间框架确认, 因素=[%s]", 
                                    resonanceCount, String.join(", ", confirmationFactors));
        
        // 计算置信度
        double confidence = calculateConfidence(resonanceCount, hourlyData, fifteenMinData, fiveMinData);
        
        // 生成元数据
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("resonance_count", resonanceCount);
        metadata.put("confirmed_factors", confirmationFactors);
        metadata.put("timeframes_used", Arrays.asList("1h", "15m", "5m"));
        
        TradingSignal signal = new TradingSignal(
            SignalModel.TREND_MOMENTUM_RESONANCE,
            direction,
            strength,
            reason,
            confidence,
            metadata
        );
        
        return Optional.of(signal);
    }
    
    /**
     * 确认1小时趋势
     */
    private boolean confirmHourlyTrend(List<Candlestick> hourlyData) {
        if (hourlyData.size() < 50) {
            return false;
        }
        
        // 检查EMA多头排列
        List<Double> ema20 = TechnicalIndicators.calculateEMA(hourlyData, 20);
        List<Double> ema50 = TechnicalIndicators.calculateEMA(hourlyData, 50);
        List<Double> ema200 = TechnicalIndicators.calculateEMA(hourlyData, 200);
        
        if (ema20.isEmpty() || ema50.isEmpty() || ema200.isEmpty()) {
            return false;
        }
        
        int lastIndex = ema20.size() - 1;
        Double ema20Val = ema20.get(lastIndex);
        Double ema50Val = ema50.get(lastIndex);
        Double ema200Val = ema200.get(lastIndex);
        double currentPrice = hourlyData.get(hourlyData.size() - 1).getClose();
        
        if (ema20Val == null || ema50Val == null || ema200Val == null) {
            return false;
        }
        
        // 检查多头排列（看涨）或空头排列（看跌）
        boolean isBullishPattern = ema20Val > ema50Val && ema50Val > ema200Val && currentPrice > ema200Val;
        boolean isBearishPattern = ema20Val < ema50Val && ema50Val < ema200Val && currentPrice < ema200Val;
        
        if (!isBullishPattern && !isBearishPattern) {
            return false;
        }
        
        // 检查ADX强度（使用简化计算）
        boolean adxStrong = isAdxStrong(hourlyData);
        
        // 检查MACD位置
        boolean macdPosition = isMacdInCorrectPosition(hourlyData, isBullishPattern);
        
        return adxStrong && macdPosition;
    }
    
    /**
     * 检查ADX强度（简化实现）
     */
    private boolean isAdxStrong(List<Candlestick> hourlyData) {
        // 这里简化实现，实际应用中应计算ADX指标
        // 暂时使用价格偏离均线的程度作为替代
        if (hourlyData.size() < 25) {
            return false;
        }
        
        List<Double> ema20 = TechnicalIndicators.calculateEMA(hourlyData, 20);
        List<Double> ema200 = TechnicalIndicators.calculateEMA(hourlyData, 200);
        
        if (ema20.isEmpty() || ema200.isEmpty()) {
            return false;
        }
        
        Double latestEma20 = ema20.get(ema20.size() - 1);
        Double latestEma200 = ema200.get(ema200.size() - 1);
        double currentPrice = hourlyData.get(hourlyData.size() - 1).getClose();
        
        if (latestEma20 == null || latestEma200 == null) {
            return false;
        }
        
        // 检查价格偏离均线程度是否足够（> 2.5%）
        double deviation = Math.abs(currentPrice - latestEma200) / latestEma200;
        return deviation > 0.025;
    }
    
    /**
     * 检查MACD位置
     */
    private boolean isMacdInCorrectPosition(List<Candlestick> hourlyData, boolean isBullish) {
        Map<String, List<Double>> macdData = TechnicalIndicators.calculateMACD(hourlyData, 12, 26, 9);
        List<Double> dif = macdData.get("dif");
        List<Double> dea = macdData.get("dea");
        
        if (dif.isEmpty() || dea.isEmpty()) {
            return false;
        }
        
        Double latestDif = dif.get(dif.size() - 1);
        Double latestDea = dea.get(dea.size() - 1);
        
        if (latestDif == null || latestDea == null) {
            return false;
        }
        
        // 对于看涨趋势，MACD应在零轴上方；对于看跌趋势，应在零轴下方
        if (isBullish) {
            return latestDif > 0 && latestDea > 0;
        } else {
            return latestDif < 0 && latestDea < 0;
        }
    }
    
    /**
     * 确认15分钟动量
     */
    private boolean confirmFifteenMinMomentum(List<Candlestick> fifteenMinData) {
        if (fifteenMinData.size() < 20) {
            return false;
        }
        
        // 检查RSI是否从超卖区回弹
        List<Double> rsi = TechnicalIndicators.calculateRSI(fifteenMinData, 14);
        if (rsi.size() < 5) { // 需要有足够的数据点来判断是否从超卖/超买区回弹
            return false;
        }
        
        // 检查RSI是否在合理的区间（从超卖区回弹到45+ 或 从超买区回弹到55-）
        Double currentRsi = rsi.get(rsi.size() - 1);
        Double prevRsi = rsi.size() > 1 ? rsi.get(rsi.size() - 2) : null;
        
        boolean rsiCondition = false;
        if (currentRsi != null) {
            // 检查RSI是否在合理区间（看涨：30-70，看跌：30-70但趋势向下）
            rsiCondition = currentRsi >= 30 && currentRsi <= 70;
        }
        
        // 检查K线形态（简化实现：检查是否有较长实体的K线）
        boolean candlePattern = hasSignificantCandleBody(fifteenMinData);
        
        // 检查成交量是否放大
        boolean volumeAmplified = isVolumeAmplified(fifteenMinData);
        
        return rsiCondition && candlePattern && volumeAmplified;
    }
    
    /**
     * 检查是否有显著实体的K线
     */
    private boolean hasSignificantCandleBody(List<Candlestick> data) {
        if (data.size() < 1) {
            return false;
        }
        
        Candlestick latestCandle = data.get(data.size() - 1);
        double bodySize = Math.abs(latestCandle.getClose() - latestCandle.getOpen());
        double candleRange = latestCandle.getHigh() - latestCandle.getLow();
        
        // 实体占整个K线范围的比例超过60%
        return candleRange > 0 && (bodySize / candleRange) > 0.6;
    }
    
    /**
     * 检查成交量是否放大
     */
    private boolean isVolumeAmplified(List<Candlestick> data) {
        if (data.size() < 20) {
            return false;
        }
        
        // 计算最近20根K线的平均成交量
        double volumeSum = 0;
        for (int i = Math.max(0, data.size() - 20); i < data.size(); i++) {
            volumeSum += data.get(i).getVolume();
        }
        double avgVolume = volumeSum / Math.min(20, data.size());
        
        // 检查当前成交量是否放大
        double currentVolume = data.get(data.size() - 1).getVolume();
        return avgVolume > 0 && (currentVolume / avgVolume) >= 1.2; // 成交量放大至均量1.2倍
    }
    
    /**
     * 确认5分钟入场时机
     */
    private boolean confirmFiveMinEntry(List<Candlestick> fiveMinData) {
        if (fiveMinData.size() < 10) {
            return false;
        }
        
        // 检查是否回踩关键支撑/阻力不破
        boolean supportResistanceHeld = isSupportResistanceHeld(fiveMinData);
        
        // 检查是否有微型反转形态（简化实现：检查是否出现十字星或纺锤线）
        boolean reversalPattern = hasReversalPattern(fiveMinData);
        
        // 检查是否在合理的价格区间
        boolean priceInRange = isPriceInProperRange(fiveMinData);
        
        return supportResistanceHeld || reversalPattern || priceInRange;
    }
    
    /**
     * 检查是否回踩支撑阻力不破
     */
    private boolean isSupportResistanceHeld(List<Candlestick> data) {
        if (data.size() < 5) {
            return false;
        }
        
        // 简化实现：检查最近几根K线是否在窄幅区间内震荡
        double highestHigh = Double.MIN_VALUE;
        double lowestLow = Double.MAX_VALUE;
        
        for (int i = Math.max(0, data.size() - 5); i < data.size(); i++) {
            Candlestick candle = data.get(i);
            if (candle.getHigh() > highestHigh) highestHigh = candle.getHigh();
            if (candle.getLow() < lowestLow) lowestLow = candle.getLow();
        }
        
        // 检查区间宽度相对于ATR的比例
        List<Double> atrList = TechnicalIndicators.calculateATR(data, 14);
        if (atrList.isEmpty() || atrList.get(atrList.size() - 1) == null) {
            return false;
        }
        
        double currentAtr = atrList.get(atrList.size() - 1);
        double rangeWidth = highestHigh - lowestLow;
        
        // 如果窄幅震荡的区间小于ATR的一半，则认为支撑阻力有效
        return currentAtr > 0 && (rangeWidth / currentAtr) < 0.5;
    }
    
    /**
     * 检查是否有反转形态
     */
    private boolean hasReversalPattern(List<Candlestick> data) {
        if (data.size() < 2) {
            return false;
        }
        
        // 检查是否出现十字星（实体很小）
        Candlestick latestCandle = data.get(data.size() - 1);
        double bodySize = Math.abs(latestCandle.getClose() - latestCandle.getOpen());
        double candleRange = latestCandle.getHigh() - latestCandle.getLow();
        
        // 实体占整个K线范围的比例小于10%
        return candleRange > 0 && (bodySize / candleRange) < 0.1;
    }
    
    /**
     * 检查价格是否在合理区间
     */
    private boolean isPriceInProperRange(List<Candlestick> data) {
        if (data.size() < 20) {
            return false;
        }
        
        // 计算布林带
        Map<String, List<Double>> bollingerBands = TechnicalIndicators.calculateBollingerBands(data, 20, 2.0);
        List<Double> upperBand = bollingerBands.get("upper");
        List<Double> lowerBand = bollingerBands.get("lower");
        List<Double> middleBand = bollingerBands.get("middle");
        
        if (upperBand.isEmpty() || lowerBand.isEmpty() || middleBand.isEmpty()) {
            return false;
        }
        
        Double currentUpper = upperBand.get(upperBand.size() - 1);
        Double currentLower = lowerBand.get(lowerBand.size() - 1);
        Double currentMiddle = middleBand.get(middleBand.size() - 1);
        double currentPrice = data.get(data.size() - 1).getClose();
        
        if (currentUpper == null || currentLower == null || currentMiddle == null) {
            return false;
        }
        
        // 检查价格是否在布林带中间区域（不是极端位置）
        return currentPrice > currentLower * 1.05 && currentPrice < currentUpper * 0.95;
    }
    
    /**
     * 确定信号方向
     */
    private Decision determineDirection(List<Candlestick> hourlyData, 
                                     List<Candlestick> fifteenMinData, 
                                     List<Candlestick> fiveMinData) {
        // 基于1小时趋势方向确定主要方向
        List<Double> ema200 = TechnicalIndicators.calculateEMA(hourlyData, 200);
        double currentPrice = hourlyData.get(hourlyData.size() - 1).getClose();
        
        if (ema200.isEmpty() || ema200.get(ema200.size() - 1) == null) {
            // 如果无法确定趋势，基于价格短期动量
            return determineDirectionFromMomentum(fifteenMinData);
        }
        
        double ema200Val = ema200.get(ema200.size() - 1);
        
        if (currentPrice > ema200Val) {
            return Decision.LONG; // 价格在长期均线上方，看涨
        } else {
            return Decision.SHORT; // 价格在长期均线下方，看跌
        }
    }
    
    /**
     * 基于动量确定方向
     */
    private Decision determineDirectionFromMomentum(List<Candlestick> data) {
        if (data.size() < 2) {
            return Decision.NO_TRADE;
        }
        
        double currentPrice = data.get(data.size() - 1).getClose();
        double prevPrice = data.get(data.size() - 2).getClose();
        
        return currentPrice > prevPrice ? Decision.LONG : Decision.SHORT;
    }
    
    /**
     * 计算共振强度
     */
    private double calculateResonanceStrength(int resonanceCount) {
        switch (resonanceCount) {
            case 3:
                return 10.0; // 三个时间框架全部确认
            case 2:
                return 7.0;  // 两个时间框架确认
            case 1:
                return 3.0;  // 一个时间框架确认
            default:
                return 0.0;
        }
    }
    
    /**
     * 计算置信度
     */
    private double calculateConfidence(int resonanceCount, List<Candlestick> hourlyData, 
                                    List<Candlestick> fifteenMinData, List<Candlestick> fiveMinData) {
        // 基础置信度基于共振数量
        double baseConfidence = resonanceCount / 3.0; // 最多3个时间框架
        
        // 根据各时间框架的强度调整
        double hourlyStrength = evaluateTimeframeStrength(hourlyData, "1h");
        double fifteenMinStrength = evaluateTimeframeStrength(fifteenMinData, "15m");
        double fiveMinStrength = evaluateTimeframeStrength(fiveMinData, "5m");
        
        // 平均强度
        double avgStrength = (hourlyStrength + fifteenMinStrength + fiveMinStrength) / 3.0;
        
        // 综合置信度
        return Math.min(1.0, baseConfidence * 0.6 + avgStrength * 0.4);
    }
    
    /**
     * 评估单个时间框架的强度
     */
    private double evaluateTimeframeStrength(List<Candlestick> data, String timeframe) {
        if (data.isEmpty()) {
            return 0.0;
        }
        
        // 计算ATR来评估波动性
        List<Double> atrList = TechnicalIndicators.calculateATR(data, 14);
        if (atrList.isEmpty() || atrList.get(atrList.size() - 1) == null) {
            return 0.3; // 默认中等强度
        }
        
        double currentAtr = atrList.get(atrList.size() - 1);
        double currentPrice = data.get(data.size() - 1).getClose();
        double atrPercentage = (currentAtr / currentPrice) * 100;
        
        // 根据ATR百分比调整强度（适度的波动性更有意义）
        if (atrPercentage > 1.0 && atrPercentage < 4.0) {
            return 0.8; // 适中波动性，强度较高
        } else if (atrPercentage >= 4.0) {
            return 0.6; // 高波动性，可能存在噪音
        } else {
            return 0.4; // 低波动性，信号可能不够活跃
        }
    }
}