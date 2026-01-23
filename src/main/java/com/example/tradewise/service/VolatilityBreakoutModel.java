package com.example.tradewise.service;

import com.example.tradewise.service.MarketAnalysisService.Candlestick;
import com.example.tradewise.service.MarketAnalysisService.TechnicalIndicators;
import com.example.tradewise.service.SignalFusionEngine.TradingSignal;
import com.example.tradewise.service.SignalFusionEngine.SignalModel;
import com.example.tradewise.service.SignalFusionEngine.Decision;

import java.util.*;

/**
 * 波动率结构突破模型（智能版）
 * 实现DeepSeek方案中的模型3：波动率结构突破模型
 */
public class VolatilityBreakoutModel {
    
    /**
     * 检测波动率结构突破信号
     */
    public Optional<TradingSignal> detect(String symbol, Map<String, List<Candlestick>> multiTimeframeData) {
        // 获取15分钟数据用于分析
        List<Candlestick> fifteenMinData = multiTimeframeData.get("15m");
        
        if (fifteenMinData == null || fifteenMinData.size() < 50) {
            return Optional.empty();
        }
        
        // 三层确认机制
        boolean isLowVolatilityState = checkLowVolatilityState(fifteenMinData);
        boolean isMarketStructureReady = checkMarketStructure(fifteenMinData);
        boolean isBreakoutQualityGood = checkBreakoutQuality(fifteenMinData);
        
        if (!(isLowVolatilityState && isMarketStructureReady && isBreakoutQualityGood)) {
            return Optional.empty(); // 不满足三层确认机制
        }
        
        // 智能过滤：根据市场状态决定信号方向
        Decision direction = determineDirectionBasedOnMarketState(fifteenMinData, isMarketStructureReady);
        
        // 计算信号强度
        double strength = calculateBreakoutStrength(isLowVolatilityState, isMarketStructureReady, isBreakoutQualityGood);
        
        // 生成信号原因
        String reason = generateReason(isLowVolatilityState, isMarketStructureReady, isBreakoutQualityGood, direction);
        
        // 计算置信度
        double confidence = calculateConfidence(fifteenMinData);
        
        // 生成元数据
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("low_volatility_state", isLowVolatilityState);
        metadata.put("market_structure_ready", isMarketStructureReady);
        metadata.put("breakout_quality_good", isBreakoutQualityGood);
        metadata.put("timeframe_used", "15m");
        
        TradingSignal signal = new TradingSignal(
            SignalModel.VOLATILITY_BREAKOUT,
            direction,
            strength,
            reason,
            confidence,
            metadata
        );
        
        return Optional.of(signal);
    }
    
    /**
     * 检查波动率状态（第一层确认）
     */
    private boolean checkLowVolatilityState(List<Candlestick> data) {
        if (data.size() < 100) {
            return false;
        }
        
        // 计算布林带宽度百分位
        List<Double> bbWidthPercentiles = calculateBollingerWidthPercentiles(data, 100);
        double currentBbWidthPercentile = bbWidthPercentiles.get(bbWidthPercentiles.size() - 1);
        
        // 计算ATR百分位
        List<Double> atrPercentiles = calculateAtrPercentiles(data, 100);
        double currentAtrPercentile = atrPercentiles.get(atrPercentiles.size() - 1);
        
        // 检查是否都处于最低25%分位
        return currentBbWidthPercentile <= 25.0 && currentAtrPercentile <= 25.0;
    }
    
    /**
     * 计算布林带宽度百分位
     */
    private List<Double> calculateBollingerWidthPercentiles(List<Candlestick> data, int lookbackPeriod) {
        List<Double> percentiles = new ArrayList<>();
        
        // 先计算布林带宽度
        Map<String, List<Double>> bollingerBands = TechnicalIndicators.calculateBollingerBands(data, 20, 2.0);
        List<Double> upperBand = bollingerBands.get("upper");
        List<Double> lowerBand = bollingerBands.get("lower");
        List<Double> middleBand = bollingerBands.get("middle");
        
        if (upperBand.isEmpty() || lowerBand.isEmpty() || middleBand.isEmpty()) {
            // 如果无法计算布林带，返回默认值
            for (int i = 0; i < data.size(); i++) {
                percentiles.add(50.0); // 默认中位数
            }
            return percentiles;
        }
        
        // 计算布林带宽度
        List<Double> bbWidths = new ArrayList<>();
        for (int i = 0; i < data.size(); i++) {
            Double up = upperBand.get(i);
            Double low = lowerBand.get(i);
            Double mid = middleBand.get(i);
            
            if (up != null && low != null && mid != null && mid != 0) {
                double width = (up - low) / mid * 100; // 百分比形式
                bbWidths.add(width);
            } else {
                bbWidths.add(0.0);
            }
        }
        
        // 计算每个点的百分位
        for (int i = 0; i < bbWidths.size(); i++) {
            int start = Math.max(0, i - lookbackPeriod + 1);
            List<Double> recentWidths = new ArrayList<>(bbWidths.subList(start, i + 1));
            Collections.sort(recentWidths);
            
            // 找到当前位置的值在排序列表中的百分位
            double currentValue = bbWidths.get(i);
            int position = Collections.binarySearch(recentWidths, currentValue);
            if (position < 0) {
                position = -(position + 1); // 如果未找到，binarySearch返回插入点的负值
            }
            
            double percentile = (double) position / recentWidths.size() * 100;
            percentiles.add(percentile);
        }
        
        return percentiles;
    }
    
    /**
     * 计算ATR百分位
     */
    private List<Double> calculateAtrPercentiles(List<Candlestick> data, int lookbackPeriod) {
        List<Double> percentiles = new ArrayList<>();
        
        // 计算ATR序列
        List<Double> atrList = TechnicalIndicators.calculateATR(data, 14);
        
        if (atrList.isEmpty()) {
            // 如果无法计算ATR，返回默认值
            for (int i = 0; i < data.size(); i++) {
                percentiles.add(50.0); // 默认中位数
            }
            return percentiles;
        }
        
        // 计算ATR百分位
        for (int i = 0; i < atrList.size(); i++) {
            Double currentAtr = atrList.get(i);
            if (currentAtr == null) {
                percentiles.add(50.0);
                continue;
            }
            
            // 获取最近lookbackPeriod个ATR值
            int start = Math.max(0, i - lookbackPeriod + 1);
            List<Double> recentAtrs = new ArrayList<>();
            for (int j = start; j <= i; j++) {
                if (j < atrList.size() && atrList.get(j) != null) {
                    recentAtrs.add(atrList.get(j));
                }
            }
            
            if (recentAtrs.isEmpty()) {
                percentiles.add(50.0);
                continue;
            }
            
            // 排序并计算百分位
            Collections.sort(recentAtrs);
            int position = Collections.binarySearch(recentAtrs, currentAtr);
            if (position < 0) {
                position = -(position + 1);
            }
            
            double percentile = (double) position / recentAtrs.size() * 100;
            percentiles.add(percentile);
        }
        
        return percentiles;
    }
    
    /**
     * 检查市场结构（第二层确认）
     */
    private boolean checkMarketStructure(List<Candlestick> data) {
        if (data.size() < 20) { // 至少需要8根K线，但我们使用更多来确保准确性
            return false;
        }
        
        // 识别盘整区间（高点和低点）
        List<Double> highs = new ArrayList<>();
        List<Double> lows = new ArrayList<>();
        
        for (Candlestick candle : data) {
            highs.add(candle.getHigh());
            lows.add(candle.getLow());
        }
        
        // 找到近期的最高高点和最低低点，作为盘整区间边界
        double rangeHigh = Collections.max(highs.subList(Math.max(0, highs.size() - 20), highs.size()));
        double rangeLow = Collections.min(lows.subList(Math.max(0, lows.size() - 20), lows.size()));
        
        // 检查盘整时间（至少8根K线）
        int consolidationBars = getConsolidationPeriod(data, rangeHigh, rangeLow);
        
        // 检查是否有假突破历史
        boolean hasFalseBreakouts = hasFalseBreakouts(data, rangeHigh, rangeLow);
        
        return consolidationBars >= 8 && !hasFalseBreakouts;
    }
    
    /**
     * 获取盘整周期
     */
    private int getConsolidationPeriod(List<Candlestick> data, double rangeHigh, double rangeLow) {
        int consolidationCount = 0;
        
        // 从最新的K线开始向前检查
        for (int i = data.size() - 1; i >= 0; i--) {
            Candlestick candle = data.get(i);
            
            // 检查这根K线是否在区间内
            if (candle.getHigh() <= rangeHigh && candle.getLow() >= rangeLow) {
                consolidationCount++;
            } else {
                // 如果超出区间，停止计数
                break;
            }
        }
        
        return consolidationCount;
    }
    
    /**
     * 检查是否有假突破历史
     */
    private boolean hasFalseBreakouts(List<Candlestick> data, double rangeHigh, double rangeLow) {
        // 检查最近是否有突破后迅速收回的情况
        for (int i = Math.max(0, data.size() - 10); i < data.size() - 1; i++) { // 检查最近10根K线
            Candlestick current = data.get(i);
            Candlestick next = data.get(i + 1);
            
            // 检查是否突破区间然后收回
            boolean brokeOutUp = current.getHigh() > rangeHigh && next.getClose() <= rangeHigh;
            boolean brokeOutDown = current.getLow() < rangeLow && next.getClose() >= rangeLow;
            
            if (brokeOutUp || brokeOutDown) {
                return true; // 发现假突破
            }
        }
        
        return false;
    }
    
    /**
     * 检查突破质量（第三层确认）
     */
    private boolean checkBreakoutQuality(List<Candlestick> data) {
        if (data.size() < 5) {
            return false;
        }
        
        // 获取最新的K线数据
        Candlestick latestCandle = data.get(data.size() - 1);
        Candlestick prevCandle = data.size() > 1 ? data.get(data.size() - 2) : null;
        
        // 计算ATR用于判断突破实体大小
        List<Double> atrList = TechnicalIndicators.calculateATR(data, 14);
        double currentAtr = atrList.isEmpty() || atrList.get(atrList.size() - 1) == null ? 0 : atrList.get(atrList.size() - 1);
        
        // 检查突破K线：实体 > ATR*0.8
        if (currentAtr > 0) {
            double bodySize = Math.abs(latestCandle.getClose() - latestCandle.getOpen());
            boolean hasStrongBody = bodySize > currentAtr * 0.8;
            
            if (!hasStrongBody) {
                return false; // 实体不够强
            }
        } else {
            // 如果ATR无效，使用相对价格变化
            double priceRange = latestCandle.getHigh() - latestCandle.getLow();
            double bodySize = Math.abs(latestCandle.getClose() - latestCandle.getOpen());
            boolean hasStrongBody = priceRange > 0 && (bodySize / priceRange) > 0.7; // 实体占K线范围70%以上
            
            if (!hasStrongBody) {
                return false;
            }
        }
        
        // 检查成交量： > 均量*2.0
        boolean hasHighVolume = checkHighVolume(data);
        
        // 检查收盘确认：连续2根K线在区间外（简化检查最近2根）
        boolean hasCloseConfirmation = checkCloseConfirmation(data);
        
        return hasHighVolume && hasCloseConfirmation;
    }
    
    /**
     * 检查高成交量
     */
    private boolean checkHighVolume(List<Candlestick> data) {
        if (data.size() < 20) {
            return false;
        }
        
        // 计算平均成交量
        double volumeSum = 0;
        for (int i = Math.max(0, data.size() - 20); i < data.size(); i++) {
            volumeSum += data.get(i).getVolume();
        }
        double avgVolume = volumeSum / Math.min(20, data.size());
        
        // 检查当前成交量是否满足要求
        double currentVolume = data.get(data.size() - 1).getVolume();
        return avgVolume > 0 && (currentVolume / avgVolume) >= 2.0;
    }
    
    /**
     * 检查收盘确认
     */
    private boolean checkCloseConfirmation(List<Candlestick> data) {
        if (data.size() < 2) {
            return false;
        }
        
        // 简化实现：检查最近2根K线是否收盘在同一方向
        Candlestick latest = data.get(data.size() - 1);
        Candlestick prev = data.get(data.size() - 2);
        
        // 检查收盘价方向是否一致
        return (latest.getClose() > latest.getOpen()) == (prev.getClose() > prev.getOpen());
    }
    
    /**
     * 基于市场状态确定方向
     */
    private Decision determineDirectionBasedOnMarketState(List<Candlestick> data, boolean isMarketStructureReady) {
        if (!isMarketStructureReady || data.size() < 2) {
            return Decision.NO_TRADE;
        }
        
        // 获取盘整区间
        List<Double> highs = new ArrayList<>();
        List<Double> lows = new ArrayList<>();
        
        for (Candlestick candle : data) {
            highs.add(candle.getHigh());
            lows.add(candle.getLow());
        }
        
        double rangeHigh = Collections.max(highs.subList(Math.max(0, highs.size() - 20), highs.size()));
        double rangeLow = Collections.min(lows.subList(Math.max(0, lows.size() - 20), lows.size()));
        
        // 获取当前价格
        double currentPrice = data.get(data.size() - 1).getClose();
        double prevPrice = data.size() > 1 ? data.get(data.size() - 2).getClose() : currentPrice;
        
        // 判断突破方向
        if (currentPrice > rangeHigh) {
            // 向上突破
            return Decision.LONG;
        } else if (currentPrice < rangeLow) {
            // 向下突破
            return Decision.SHORT;
        } else {
            // 仍在区间内，检查趋势
            if (currentPrice > prevPrice) {
                // 价格向上移动，可能向上突破
                return Decision.LONG;
            } else {
                // 价格向下移动，可能向下突破
                return Decision.SHORT;
            }
        }
    }
    
    /**
     * 计算突破强度
     */
    private double calculateBreakoutStrength(boolean lowVolState, boolean marketStructReady, boolean breakoutQualityGood) {
        int confirmedLayers = 0;
        if (lowVolState) confirmedLayers++;
        if (marketStructReady) confirmedLayers++;
        if (breakoutQualityGood) confirmedLayers++;
        
        // 根据确认层数计算强度
        switch (confirmedLayers) {
            case 3:
                return 9.0; // 所有三层都确认，高强度
            case 2:
                return 6.0; // 两层确认，中等强度
            case 1:
                return 3.0; // 一层确认，低强度
            default:
                return 0.0; // 无确认
        }
    }
    
    /**
     * 生成信号原因
     */
    private String generateReason(boolean lowVolState, boolean marketStructReady, boolean breakoutQualityGood, Decision direction) {
        StringBuilder sb = new StringBuilder();
        sb.append("波动率结构突破检测: ");
        
        List<String> confirmations = new ArrayList<>();
        if (lowVolState) confirmations.add("低波动率状态");
        if (marketStructReady) confirmations.add("市场结构就绪");
        if (breakoutQualityGood) confirmations.add("突破质量优良");
        
        sb.append(String.join(" + ", confirmations));
        sb.append(", 方向: ").append(direction == Decision.LONG ? "做多" : "做空");
        
        return sb.toString();
    }
    
    /**
     * 计算置信度
     */
    private double calculateConfidence(List<Candlestick> data) {
        // 基础置信度
        double baseConfidence = 0.8; // 假设突破模型有较高置信度
        
        // 根据数据量调整
        if (data.size() < 30) {
            baseConfidence *= 0.7; // 数据不足降低置信度
        } else if (data.size() >= 100) {
            baseConfidence *= 1.1; // 数据充分提高置信度
        }
        
        // 根据波动率调整
        List<Double> atrList = TechnicalIndicators.calculateATR(data, 14);
        if (!atrList.isEmpty() && atrList.get(atrList.size() - 1) != null) {
            double currentAtr = atrList.get(atrList.size() - 1);
            double currentPrice = data.get(data.size() - 1).getClose();
            double atrPercentage = (currentAtr / currentPrice) * 100;
            
            // 高波动环境降低置信度
            if (atrPercentage > 3.0) {
                baseConfidence *= 0.8;
            } else if (atrPercentage < 0.5) {
                // 过低波动可能表示市场不活跃
                baseConfidence *= 0.9;
            }
        }
        
        // 确保置信度在合理范围内
        return Math.max(0.1, Math.min(1.0, baseConfidence));
    }
}