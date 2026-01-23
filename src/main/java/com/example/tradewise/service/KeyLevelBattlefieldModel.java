package com.example.tradewise.service;

import com.example.tradewise.service.MarketAnalysisService.Candlestick;
import com.example.tradewise.service.MarketAnalysisService.TechnicalIndicators;
import com.example.tradewise.service.SignalFusionEngine.TradingSignal;
import com.example.tradewise.service.SignalFusionEngine.SignalModel;
import com.example.tradewise.service.SignalFusionEngine.Decision;

import java.util.*;

/**
 * 关键位置博弈模型（精准版）
 * 实现DeepSeek方案中的模型4：关键位置博弈模型
 */
public class KeyLevelBattlefieldModel {
    
    /**
     * 检测关键位置博弈信号
     */
    public Optional<TradingSignal> detect(String symbol, Map<String, List<Candlestick>> multiTimeframeData) {
        // 获取日线数据用于识别关键位置
        List<Candlestick> dailyData = multiTimeframeData.get("1d");
        List<Candlestick> hourlyData = multiTimeframeData.get("1h");
        
        if (dailyData == null || hourlyData == null || dailyData.size() < 30 || hourlyData.size() < 24) {
            return Optional.empty();
        }
        
        // 识别关键博弈场景
        String scenario = identifyKeyBattleScenario(dailyData, hourlyData);
        
        if (scenario == null || scenario.equals("NONE")) {
            return Optional.empty();
        }
        
        // 根据场景确定方向和强度
        Decision direction;
        double strength;
        String reason;
        
        switch (scenario) {
            case "STOP_HUNTING":
                // 场景1：止损狩猎 - 反向开仓
                direction = getReverseDirection(hourlyData);
                strength = 7.0;
                reason = "检测到止损狩猎模式：价格突破关键位后迅速收回且成交量爆量，建议反向操作";
                break;
            case "SUPPLY_DEMAND_IMBALANCE":
                // 场景2：供需失衡 - 顺突破方向开仓
                direction = getBreakoutDirection(hourlyData);
                strength = 8.0;
                reason = "检测到供需失衡模式：价格在关键位反复测试且成交量逐渐萎缩，突破后顺向开仓";
                break;
            case "TIME_EXHAUSTION":
                // 场景3：时间耗尽 - 依突破方向开仓
                direction = getBreakoutDirection(hourlyData);
                strength = 6.0;
                reason = "检测到时间耗尽模式：价格在关键位盘整超时且波动率不断降低，依突破方向开仓";
                break;
            default:
                return Optional.empty();
        }
        
        // 计算置信度
        double confidence = calculateConfidence(scenario, hourlyData);
        
        // 生成元数据
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("battle_scenario", scenario);
        metadata.put("key_levels", identifyKeyLevels(dailyData));
        metadata.put("analysis_timeframes", Arrays.asList("1d", "1h"));
        
        TradingSignal signal = new TradingSignal(
            SignalModel.KEY_LEVEL_BATTLEGROUNDS,
            direction,
            strength,
            reason,
            confidence,
            metadata
        );
        
        return Optional.of(signal);
    }
    
    /**
     * 识别关键博弈场景
     */
    private String identifyKeyBattleScenario(List<Candlestick> dailyData, List<Candlestick> hourlyData) {
        // 获取关键位
        Map<String, Double> keyLevels = identifyKeyLevels(dailyData);
        if (keyLevels.isEmpty()) {
            return "NONE";
        }
        
        double dailyHigh = keyLevels.getOrDefault("high", 0.0);
        double dailyLow = keyLevels.getOrDefault("low", 0.0);
        double prevHigh = keyLevels.getOrDefault("prev_high", 0.0);
        double prevLow = keyLevels.getOrDefault("prev_low", 0.0);
        
        // 获取当前价格和最近的小时数据
        if (hourlyData.isEmpty()) {
            return "NONE";
        }
        
        double currentPrice = hourlyData.get(hourlyData.size() - 1).getClose();
        Candlestick latestHourly = hourlyData.get(hourlyData.size() - 1);
        Candlestick prevHourly = hourlyData.size() > 1 ? hourlyData.get(hourlyData.size() - 2) : null;
        
        // 检查止损狩猎场景
        if (isStopHuntingScenario(hourlyData, dailyHigh, dailyLow, currentPrice)) {
            return "STOP_HUNTING";
        }
        
        // 检查供需失衡场景
        if (isSupplyDemandImbalanceScenario(hourlyData, dailyHigh, dailyLow, currentPrice)) {
            return "SUPPLY_DEMAND_IMBALANCE";
        }
        
        // 检查时间耗尽场景
        if (isTimeExhaustionScenario(hourlyData, dailyHigh, dailyLow, currentPrice)) {
            return "TIME_EXHAUSTION";
        }
        
        return "NONE";
    }
    
    /**
     * 检查止损狩猎场景
     */
    private boolean isStopHuntingScenario(List<Candlestick> hourlyData, double dailyHigh, double dailyLow, double currentPrice) {
        if (hourlyData.size() < 5) {
            return false;
        }
        
        // 检查是否突破关键位
        boolean brokeDailyHigh = false;
        boolean brokeDailyLow = false;
        
        // 检查最近几小时是否突破日高/日低
        for (int i = Math.max(0, hourlyData.size() - 3); i < hourlyData.size(); i++) {
            Candlestick candle = hourlyData.get(i);
            if (candle.getHigh() > dailyHigh) {
                brokeDailyHigh = true;
            }
            if (candle.getLow() < dailyLow) {
                brokeDailyLow = true;
            }
        }
        
        // 如果突破后迅速收回，且成交量爆量，则可能是止损狩猎
        if (brokeDailyHigh || brokeDailyLow) {
            // 检查当前价格是否回到关键位内
            boolean returnedInside = currentPrice >= dailyLow && currentPrice <= dailyHigh;
            
            // 检查成交量是否爆量
            boolean volumeSpikes = isVolumeSpiking(hourlyData);
            
            return returnedInside && volumeSpikes;
        }
        
        return false;
    }
    
    /**
     * 检查供需失衡场景
     */
    private boolean isSupplyDemandImbalanceScenario(List<Candlestick> hourlyData, double dailyHigh, double dailyLow, double currentPrice) {
        if (hourlyData.size() < 10) {
            return false;
        }
        
        // 检查价格是否在关键位反复测试
        int touchCount = countTouchesOfKeyLevel(hourlyData, dailyHigh, dailyLow);
        
        // 检查成交量是否逐渐萎缩
        boolean volumeDecreasing = isVolumeDecreasing(hourlyData, dailyHigh, dailyLow);
        
        return touchCount >= 3 && volumeDecreasing;
    }
    
    /**
     * 检查时间耗尽场景
     */
    private boolean isTimeExhaustionScenario(List<Candlestick> hourlyData, double dailyHigh, double dailyLow, double currentPrice) {
        if (hourlyData.size() < 24) { // 至少24小时
            return false;
        }
        
        // 检查价格是否在关键位长时间盘整
        boolean isConsolidating = isPriceConsolidatingInRange(hourlyData, dailyHigh, dailyLow);
        
        // 检查波动率是否不断降低
        boolean volatilityDecreasing = isVolatilityDecreasing(hourlyData);
        
        return isConsolidating && volatilityDecreasing;
    }
    
    /**
     * 计算触及关键位的次数
     */
    private int countTouchesOfKeyLevel(List<Candlestick> hourlyData, double highLevel, double lowLevel) {
        int touches = 0;
        double tolerance = Math.abs(highLevel - lowLevel) * 0.005; // 0.5%容差
        
        for (Candlestick candle : hourlyData) {
            // 检查高点是否触及关键位
            if (Math.abs(candle.getHigh() - highLevel) <= tolerance || 
                Math.abs(candle.getLow() - highLevel) <= tolerance) {
                touches++;
            }
            // 检查低点是否触及关键位
            if (Math.abs(candle.getHigh() - lowLevel) <= tolerance || 
                Math.abs(candle.getLow() - lowLevel) <= tolerance) {
                touches++;
            }
        }
        
        return touches;
    }
    
    /**
     * 检查成交量是否递减
     */
    private boolean isVolumeDecreasing(List<Candlestick> hourlyData, double highLevel, double lowLevel) {
        // 获取在关键位附近的成交量
        List<Double> volumesInRange = new ArrayList<>();
        double tolerance = Math.abs(highLevel - lowLevel) * 0.01; // 1%容差
        
        for (Candlestick candle : hourlyData) {
            if (Math.abs(candle.getHigh() - highLevel) <= tolerance || 
                Math.abs(candle.getLow() - lowLevel) <= tolerance ||
                (candle.getHigh() >= lowLevel && candle.getLow() <= highLevel)) {
                volumesInRange.add(candle.getVolume());
            }
        }
        
        if (volumesInRange.size() < 5) {
            return false;
        }
        
        // 检查成交量是否呈递减趋势
        double earlyAvg = calculateAverage(volumesInRange.subList(0, volumesInRange.size() / 2));
        double lateAvg = calculateAverage(volumesInRange.subList(volumesInRange.size() / 2, volumesInRange.size()));
        
        return lateAvg < earlyAvg * 0.8; // 后半段平均成交量比前半段低20%以上
    }
    
    /**
     * 检查价格是否在区间内盘整
     */
    private boolean isPriceConsolidatingInRange(List<Candlestick> hourlyData, double highLevel, double lowLevel) {
        int totalBars = 0;
        int inRangeBars = 0;
        double tolerance = Math.abs(highLevel - lowLevel) * 0.02; // 2%容差
        
        for (Candlestick candle : hourlyData) {
            if (candle.getHigh() <= highLevel + tolerance && candle.getLow() >= lowLevel - tolerance) {
                inRangeBars++;
            }
            totalBars++;
        }
        
        // 70%的时间在区间内盘整
        return totalBars > 0 && (double) inRangeBars / totalBars >= 0.7;
    }
    
    /**
     * 检查波动率是否递减
     */
    private boolean isVolatilityDecreasing(List<Candlestick> hourlyData) {
        if (hourlyData.size() < 20) {
            return false;
        }
        
        // 计算前半段和后半段的平均ATR
        int midPoint = hourlyData.size() / 2;
        List<Candlestick> firstHalf = hourlyData.subList(0, midPoint);
        List<Candlestick> secondHalf = hourlyData.subList(midPoint, hourlyData.size());
        
        List<Double> atrFirst = TechnicalIndicators.calculateATR(firstHalf, 14);
        List<Double> atrSecond = TechnicalIndicators.calculateATR(secondHalf, 14);
        
        double avgAtrFirst = atrFirst.stream()
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(Double.MAX_VALUE);
        
        double avgAtrSecond = atrSecond.stream()
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0);
        
        return avgAtrSecond < avgAtrFirst * 0.8; // 后半段波动率比前半段低20%以上
    }
    
    /**
     * 检查成交量是否激增
     */
    private boolean isVolumeSpiking(List<Candlestick> hourlyData) {
        if (hourlyData.size() < 20) {
            return false;
        }
        
        // 计算平均成交量
        double volumeSum = 0;
        for (Candlestick candle : hourlyData) {
            volumeSum += candle.getVolume();
        }
        double avgVolume = volumeSum / hourlyData.size();
        
        // 检查最近几根K线的成交量是否显著高于平均水平
        int recentBars = Math.min(3, hourlyData.size());
        for (int i = hourlyData.size() - recentBars; i < hourlyData.size(); i++) {
            if (hourlyData.get(i).getVolume() > avgVolume * 3.0) { // 成交量是平均值的3倍以上
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 计算平均值
     */
    private double calculateAverage(List<Double> values) {
        if (values.isEmpty()) {
            return 0.0;
        }
        
        double sum = 0;
        for (Double value : values) {
            if (value != null) {
                sum += value;
            }
        }
        
        return sum / values.size();
    }
    
    /**
     * 识别关键位
     */
    private Map<String, Double> identifyKeyLevels(List<Candlestick> dailyData) {
        Map<String, Double> levels = new HashMap<>();
        
        if (dailyData.size() < 10) {
            return levels;
        }
        
        // 获取最近几天的高低点作为关键位
        int lookback = Math.min(10, dailyData.size());
        List<Candlestick> recentData = dailyData.subList(dailyData.size() - lookback, dailyData.size());
        
        double highestHigh = Double.MIN_VALUE;
        double lowestLow = Double.MAX_VALUE;
        
        for (Candlestick candle : recentData) {
            if (candle.getHigh() > highestHigh) highestHigh = candle.getHigh();
            if (candle.getLow() < lowestLow) lowestLow = candle.getLow();
        }
        
        levels.put("high", highestHigh);
        levels.put("low", lowestLow);
        
        // 如果有足够数据，也添加前一日的高点和低点
        if (dailyData.size() >= 2) {
            Candlestick prevDay = dailyData.get(dailyData.size() - 2);
            levels.put("prev_high", prevDay.getHigh());
            levels.put("prev_low", prevDay.getLow());
        }
        
        return levels;
    }
    
    /**
     * 获取反向方向（用于止损狩猎）
     */
    private Decision getReverseDirection(List<Candlestick> hourlyData) {
        if (hourlyData.size() < 2) {
            return Decision.NO_TRADE;
        }
        
        // 获取最新的小时K线
        Candlestick latest = hourlyData.get(hourlyData.size() - 1);
        Candlestick prev = hourlyData.get(hourlyData.size() - 2);
        
        // 如果最近是上涨，预期会回调做空；如果是下跌，预期会反弹做多
        if (latest.getClose() > prev.getClose()) {
            return Decision.SHORT; // 预期回调
        } else {
            return Decision.LONG; // 预期反弹
        }
    }
    
    /**
     * 获取突破方向
     */
    private Decision getBreakoutDirection(List<Candlestick> hourlyData) {
        if (hourlyData.size() < 2) {
            return Decision.NO_TRADE;
        }
        
        // 获取最新的小时K线
        Candlestick latest = hourlyData.get(hourlyData.size() - 1);
        Candlestick prev = hourlyData.get(hourlyData.size() - 2);
        
        // 根据价格方向确定突破方向
        return latest.getClose() > prev.getClose() ? Decision.LONG : Decision.SHORT;
    }
    
    /**
     * 计算置信度
     */
    private double calculateConfidence(String scenario, List<Candlestick> hourlyData) {
        double baseConfidence = 0.0;
        
        switch (scenario) {
            case "STOP_HUNTING":
                baseConfidence = 0.75; // 止损狩猎模式通常比较可靠
                break;
            case "SUPPLY_DEMAND_IMBALANCE":
                baseConfidence = 0.80; // 供需失衡模式可靠性较高
                break;
            case "TIME_EXHAUSTION":
                baseConfidence = 0.70; // 时间耗尽模式需要进一步确认
                break;
            default:
                baseConfidence = 0.50;
        }
        
        // 根据数据量调整
        if (hourlyData.size() < 20) {
            baseConfidence *= 0.7; // 数据不足降低置信度
        } else if (hourlyData.size() >= 50) {
            baseConfidence *= 1.1; // 数据充分提高置信度
        }
        
        // 根据波动率调整
        List<Double> atrList = TechnicalIndicators.calculateATR(hourlyData, 14);
        if (!atrList.isEmpty() && atrList.get(atrList.size() - 1) != null) {
            double currentAtr = atrList.get(atrList.size() - 1);
            double currentPrice = hourlyData.get(hourlyData.size() - 1).getClose();
            double atrPercentage = (currentAtr / currentPrice) * 100;
            
            // 极高或极低波动都可能影响可靠性
            if (atrPercentage > 4.0) {
                baseConfidence *= 0.8; // 过高波动降低置信度
            } else if (atrPercentage < 0.5) {
                baseConfidence *= 0.9; // 过低波动稍微降低置信度
            }
        }
        
        // 确保置信度在合理范围内
        return Math.max(0.1, Math.min(1.0, baseConfidence));
    }
}