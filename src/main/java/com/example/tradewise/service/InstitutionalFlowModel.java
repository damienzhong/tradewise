package com.example.tradewise.service;

import com.example.tradewise.service.DataEngine.MarketRegime;
import com.example.tradewise.service.MarketAnalysisService.Candlestick;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 机构资金流向模型
 * 用于检测机构资金的流入流出情况，识别机构行为模式
 */
public class InstitutionalFlowModel {
    
    private static final Logger logger = LoggerFactory.getLogger(InstitutionalFlowModel.class);
    
    /**
     * 检测机构资金流向
     * 
     * @param symbol 交易对
     * @param multiTimeframeData 多时间框架数据
     * @return 交易信号（如果有）
     */
    public Optional<SignalFusionEngine.TradingSignal> detect(String symbol, Map<String, List<Candlestick>> multiTimeframeData) {
        List<Candlestick> mainTF = multiTimeframeData.get("1h"); // 主要分析时间框架
        List<Candlestick> dailyTF = multiTimeframeData.get("1d"); // 日线用于大趋势判断
        if (mainTF == null || mainTF.size() < 20) {
            return Optional.empty();
        }
        
        // 检测机构建仓特征
        boolean institutionalAccumulation = detectInstitutionalAccumulation(mainTF, dailyTF);
        // 检测机构派发特征  
        boolean institutionalDistribution = detectInstitutionalDistribution(mainTF, dailyTF);
        
        // 检测价格与成交量关系（资金流向）
        boolean strongBullishFlow = detectStrongBullishFlow(mainTF);
        boolean strongBearishFlow = detectStrongBearishFlow(mainTF);
        
        // 综合判断
        if (institutionalAccumulation || strongBullishFlow) {
            double strength = calculateInstitutionalStrength(institutionalAccumulation, strongBullishFlow, true);
            double confidence = calculateInstitutionalConfidence(institutionalAccumulation, strongBullishFlow);
            String reason = buildInstitutionalReason(institutionalAccumulation, strongBullishFlow, "accumulation");
            
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("accumulation", institutionalAccumulation);
            metadata.put("bullish_flow", strongBullishFlow);
            metadata.put("volume_profile_analysis", performVolumeProfileAnalysis(mainTF));
            metadata.put("price_volume_relationship", analyzePriceVolumeRelationship(mainTF));
            
            return Optional.of(new SignalFusionEngine.TradingSignal(
                SignalFusionEngine.SignalModel.INSTITUTIONAL_FLOW,
                SignalFusionEngine.Decision.LONG,
                strength,
                reason,
                confidence,
                metadata
            ));
        } else if (institutionalDistribution || strongBearishFlow) {
            double strength = calculateInstitutionalStrength(institutionalDistribution, strongBearishFlow, false);
            double confidence = calculateInstitutionalConfidence(institutionalDistribution, strongBearishFlow);
            String reason = buildInstitutionalReason(institutionalDistribution, strongBearishFlow, "distribution");
            
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("distribution", institutionalDistribution);
            metadata.put("bearish_flow", strongBearishFlow);
            metadata.put("volume_profile_analysis", performVolumeProfileAnalysis(mainTF));
            metadata.put("price_volume_relationship", analyzePriceVolumeRelationship(mainTF));
            
            return Optional.of(new SignalFusionEngine.TradingSignal(
                SignalFusionEngine.SignalModel.INSTITUTIONAL_FLOW,
                SignalFusionEngine.Decision.SHORT,
                strength,
                reason,
                confidence,
                metadata
            ));
        }
        
        return Optional.empty();
    }
    
    /**
     * 检测机构建仓特征
     * 机构建仓通常表现为：缓慢吸筹、控制波动、逐步推高
     */
    private boolean detectInstitutionalAccumulation(List<Candlestick> candles, List<Candlestick> dailyCandles) {
        if (candles.size() < 50) return false;
        
        // 分析最近50根K线的特征
        List<Candlestick> recentCandles = candles.subList(Math.max(0, candles.size() - 50), candles.size());
        
        // 检查成交量分布是否呈现机构特征
        boolean hasSteadyAccumulation = checkSteadyAccumulationPattern(recentCandles);
        
        // 检查价格波动是否被控制
        boolean hasControlledVolatility = checkControlledVolatility(recentCandles);
        
        // 检查是否有测试上方阻力的行为
        boolean hasResistanceTesting = checkResistanceTesting(recentCandles);
        
        // 检查是否在关键支撑位有明显买盘
        boolean hasSupportBuying = checkSupportBuying(recentCandles, dailyCandles);
        
        return hasSteadyAccumulation && hasControlledVolatility && (hasResistanceTesting || hasSupportBuying);
    }
    
    /**
     * 检测机构派发特征
     * 机构派发通常表现为：拉高出货、制造假突破、吸引散户跟进
     */
    private boolean detectInstitutionalDistribution(List<Candlestick> candles, List<Candlestick> dailyCandles) {
        if (candles.size() < 50) return false;
        
        List<Candlestick> recentCandles = candles.subList(Math.max(0, candles.size() - 50), candles.size());
        
        // 检查是否在高位出现放量滞涨
        boolean hasHeavyVolumeAtTop = checkHeavyVolumeAtTop(recentCandles, dailyCandles);
        
        // 检查是否出现假突破后回落
        boolean hasFakeBreakout = checkFakeBreakout(recentCandles);
        
        // 检查是否出现巨量阴线
        boolean hasLargeBearishCandles = checkLargeBearishCandles(recentCandles);
        
        // 检查是否出现异常的成交量分布
        boolean hasUnusualVolumePattern = checkUnusualVolumePattern(recentCandles);
        
        return hasHeavyVolumeAtTop || hasFakeBreakout || hasLargeBearishCandles || hasUnusualVolumePattern;
    }
    
    /**
     * 检测强势资金流入特征
     */
    private boolean detectStrongBullishFlow(List<Candlestick> candles) {
        if (candles.size() < 20) return false;
        
        List<Candlestick> recentCandles = candles.subList(Math.max(0, candles.size() - 20), candles.size());
        
        int bullishDays = 0;
        int highVolumeBullishDays = 0;
        double avgVolume = getAverageVolume(candles, 20);
        
        for (Candlestick candle : recentCandles) {
            boolean isBullish = candle.getClose() > candle.getOpen();
            boolean isHighVolume = candle.getVolume() > avgVolume * 1.5;
            
            if (isBullish) bullishDays++;
            if (isBullish && isHighVolume) highVolumeBullishDays++;
        }
        
        // 如果大部分上涨日伴随高成交量，认为有强势资金流入
        return bullishDays > 0 && ((double) highVolumeBullishDays / bullishDays) > 0.6;
    }
    
    /**
     * 检测强势资金流出特征
     */
    private boolean detectStrongBearishFlow(List<Candlestick> candles) {
        if (candles.size() < 20) return false;
        
        List<Candlestick> recentCandles = candles.subList(Math.max(0, candles.size() - 20), candles.size());
        
        int bearishDays = 0;
        int highVolumeBearishDays = 0;
        double avgVolume = getAverageVolume(candles, 20);
        
        for (Candlestick candle : recentCandles) {
            boolean isBearish = candle.getClose() < candle.getOpen();
            boolean isHighVolume = candle.getVolume() > avgVolume * 1.5;
            
            if (isBearish) bearishDays++;
            if (isBearish && isHighVolume) highVolumeBearishDays++;
        }
        
        // 如果大部分下跌日伴随高成交量，认为有强势资金流出
        return bearishDays > 0 && ((double) highVolumeBearishDays / bearishDays) > 0.6;
    }
    
    // 辅助方法实现...
    private boolean checkSteadyAccumulationPattern(List<Candlestick> candles) {
        // 检查是否出现稳步上涨的模式，而非急涨急跌
        if (candles.size() < 20) return false;
        
        int gradualUpDays = 0;
        for (int i = 1; i < candles.size(); i++) {
            Candlestick prev = candles.get(i - 1);
            Candlestick curr = candles.get(i);
            
            double changePercent = Math.abs((curr.getClose() - prev.getClose()) / prev.getClose());
            boolean isGradualMove = changePercent < 0.03; // 涨跌幅小于3%
            boolean isBullish = curr.getClose() > prev.getClose();
            
            if (isGradualMove && isBullish) {
                gradualUpDays++;
            }
        }
        
        // 如果超过40%的日子都是温和上涨，认为是机构吸筹
        return (double) gradualUpDays / (candles.size() - 1) > 0.4;
    }
    
    private boolean checkControlledVolatility(List<Candlestick> candles) {
        // 检查价格波动是否被控制在一定范围内
        if (candles.size() < 10) return false;
        
        double totalRange = 0;
        for (Candlestick candle : candles) {
            totalRange += (candle.getHigh() - candle.getLow()) / candle.getClose();
        }
        
        double avgRangePercent = totalRange / candles.size();
        
        // 如果平均振幅较小，认为波动被控制
        return avgRangePercent < 0.04; // 平均振幅小于4%
    }
    
    private boolean checkResistanceTesting(List<Candlestick> candles) {
        // 检查是否在关键阻力位进行测试
        if (candles.size() < 10) return false;
        
        // 找出近期高点作为阻力位
        double recentHigh = candles.stream()
                .mapToDouble(Candlestick::getHigh)
                .max()
                .orElse(candles.get(0).getClose());
        
        int attempts = 0;
        for (Candlestick candle : candles) {
            // 检查是否多次接近阻力位
            if (Math.abs(candle.getHigh() - recentHigh) / recentHigh < 0.015) { // 在阻力位1.5%范围内
                attempts++;
            }
        }
        
        return attempts >= 2; // 至少尝试2次
    }
    
    private boolean checkSupportBuying(List<Candlestick> candles, List<Candlestick> dailyCandles) {
        // 检查在关键支撑位是否有明显买盘支撑
        if (dailyCandles == null || dailyCandles.size() < 20) return false;
        
        // 计算日线级别的关键支撑
        double supportLevel = dailyCandles.stream()
                .skip(Math.max(0, dailyCandles.size() - 20))
                .mapToDouble(Candlestick::getLow)
                .min()
                .orElse(candles.get(0).getClose());
        
        int supportTests = 0;
        int successfulSupports = 0;
        
        for (Candlestick candle : candles) {
            // 检查是否测试支撑位
            if (Math.abs(candle.getLow() - supportLevel) / supportLevel < 0.02) { // 在支撑位2%范围内
                supportTests++;
                // 检查是否获得有效支撑（收盘价高于开盘价）
                if (candle.getClose() > candle.getOpen()) {
                    successfulSupports++;
                }
            }
        }
        
        // 如果大部分支撑测试都获得有效支撑，认为有机构买盘
        return supportTests > 0 && (double) successfulSupports / supportTests > 0.6;
    }
    
    private boolean checkHeavyVolumeAtTop(List<Candlestick> candles, List<Candlestick> dailyCandles) {
        if (dailyCandles == null || dailyCandles.size() < 20) return false;
        
        // 找出日线级别的近期高点
        double recentDailyHigh = dailyCandles.stream()
                .skip(Math.max(0, dailyCandles.size() - 20))
                .mapToDouble(Candlestick::getHigh)
                .max()
                .orElse(candles.get(0).getClose());
        
        // 检查小时线是否在接近日线高点处出现巨量
        double avgVolume = getAverageVolume(candles, 20);
        for (Candlestick candle : candles) {
            if (candle.getHigh() > recentDailyHigh * 0.98 && // 接近日线高点
                candle.getVolume() > avgVolume * 2.0) { // 成交量是平均的2倍以上
                return true;
            }
        }
        
        return false;
    }
    
    private boolean checkFakeBreakout(List<Candlestick> candles) {
        if (candles.size() < 10) return false;
        
        // 查找假突破模式：突破后快速回落
        for (int i = 3; i < candles.size(); i++) {
            Candlestick prev1 = candles.get(i - 3);
            Candlestick prev2 = candles.get(i - 2);
            Candlestick prev3 = candles.get(i - 1);
            Candlestick current = candles.get(i);
            
            // 检查是否出现向上假突破
            double recentHigh = Arrays.asList(prev1, prev2, prev3, current)
                    .stream()
                    .mapToDouble(Candlestick::getHigh)
                    .max()
                    .orElse(current.getHigh());
                    
            // 如果当前K线突破前期高点但收盘回落，可能是假突破
            if (current.getHigh() > recentHigh * 1.01 && // 突破1%
                current.getClose() < current.getHigh() * 0.98) { // 但收盘回落2%
                return true;
            }
        }
        
        return false;
    }
    
    private boolean checkLargeBearishCandles(List<Candlestick> candles) {
        // 检查是否出现巨量阴线
        double avgVolume = getAverageVolume(candles, 20);
        
        for (Candlestick candle : candles) {
            double bodySize = Math.abs(candle.getClose() - candle.getOpen());
            double rangeSize = candle.getHigh() - candle.getLow();
            boolean isBearish = candle.getClose() < candle.getOpen();
            boolean isLargeVolume = candle.getVolume() > avgVolume * 2.0;
            boolean isLargeBody = bodySize / rangeSize > 0.7; // 实体占影线的70%以上
            
            if (isBearish && isLargeVolume && isLargeBody) {
                return true;
            }
        }
        
        return false;
    }
    
    private boolean checkUnusualVolumePattern(List<Candlestick> candles) {
        // 检查成交量是否出现异常分布模式
        if (candles.size() < 10) return false;
        
        // 计算成交量的标准差，判断是否出现异常
        double avgVolume = getAverageVolume(candles, candles.size());
        double variance = 0;
        for (Candlestick candle : candles) {
            variance += Math.pow(candle.getVolume() - avgVolume, 2);
        }
        double stdDev = Math.sqrt(variance / candles.size());
        
        // 检查是否有异常高的成交量
        for (Candlestick candle : candles) {
            if (candle.getVolume() > avgVolume + stdDev * 2.0) { // 超过2倍标准差
                return true;
            }
        }
        
        return false;
    }
    
    private double getAverageVolume(List<Candlestick> candles, int period) {
        return candles.stream()
                .skip(Math.max(0, candles.size() - period))
                .mapToDouble(Candlestick::getVolume)
                .average()
                .orElse(1.0);
    }
    
    private double calculateInstitutionalStrength(boolean institutionalSignal, boolean flowSignal, boolean isBullish) {
        double baseStrength = 6.0; // 基础强度
        double strength = baseStrength;
        
        if (institutionalSignal) {
            strength += 2.0; // 机构行为特征+2
        }
        
        if (flowSignal) {
            strength += 1.5; // 资金流向确认+1.5
        }
        
        // 如果是熊市中的看涨信号或牛市中的看跌信号，降低强度
        if (isCounterTrendSignal(institutionalSignal, isBullish)) {
            strength *= 0.7; // 降低30%
        }
        
        return Math.min(strength, 10.0); // 最大强度为10
    }
    
    private double calculateInstitutionalConfidence(boolean institutionalSignal, boolean flowSignal) {
        // 根据多个信号的一致性计算置信度
        if (institutionalSignal && flowSignal) return 0.9;
        if (institutionalSignal || flowSignal) return 0.7;
        return 0.5;
    }
    
    private String buildInstitutionalReason(boolean institutionalSignal, boolean flowSignal, String direction) {
        StringBuilder reason = new StringBuilder("检测到机构资金");
        
        if ("accumulation".equals(direction)) {
            reason.append("建仓行为：");
            if (institutionalSignal) {
                reason.append("识别到机构吸筹模式，");
            }
            if (flowSignal) {
                reason.append("伴随资金净流入，");
            }
            reason.append("表明大资金正在悄然介入，后市看涨概率较高");
        } else {
            reason.append("派发行为：");
            if (institutionalSignal) {
                reason.append("识别到机构出货模式，");
            }
            if (flowSignal) {
                reason.append("伴随资金净流出，");
            }
            reason.append("表明大资金正在撤离，后市看跌概率较高");
        }
        
        return reason.toString();
    }
    
    /**
     * 判断是否为逆势信号
     */
    private boolean isCounterTrendSignal(boolean institutionalSignal, boolean isBullish) {
        // 这里可以加入更多市场趋势判断逻辑
        return false; // 简化实现
    }
    
    /**
     * 体积分布分析 - 识别关键价格水平的成交量聚集
     */
    private Map<String, Object> performVolumeProfileAnalysis(List<Candlestick> candles) {
        // 简化实现：找出成交量最高的价格区间
        Map<Double, Double> priceVolumeMap = new HashMap<>();
        
        for (Candlestick candle : candles) {
            // 使用中间价作为代表价格
            double avgPrice = (candle.getHigh() + candle.getLow() + candle.getClose()) / 3.0;
            double volume = candle.getVolume();
            
            // 将价格四舍五入到两位小数，以创建价格区间
            double roundedPrice = Math.round(avgPrice * 100.0) / 100.0;
            priceVolumeMap.merge(roundedPrice, volume, Double::sum);
        }
        
        // 找出成交量最高的价格区域
        double highestVolumePrice = priceVolumeMap.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(0.0);
        
        double highestVolume = priceVolumeMap.get(highestVolumePrice);
        
        Map<String, Object> analysis = new HashMap<>();
        analysis.put("support_resistance_zone", highestVolumePrice);
        analysis.put("volume_at_key_level", highestVolume);
        analysis.put("total_volume", priceVolumeMap.values().stream().mapToDouble(Double::doubleValue).sum());
        
        return analysis;
    }
    
    /**
     * 分析价格与成交量关系
     */
    private Map<String, Object> analyzePriceVolumeRelationship(List<Candlestick> candles) {
        if (candles.size() < 10) {
            return Collections.emptyMap();
        }
        
        // 计算价格变化率和成交量变化率的相关性
        List<Double> priceChanges = new ArrayList<>();
        List<Double> volumeChanges = new ArrayList<>();
        
        for (int i = 1; i < candles.size(); i++) {
            Candlestick prev = candles.get(i - 1);
            Candlestick curr = candles.get(i);
            
            double priceChange = (curr.getClose() - prev.getClose()) / prev.getClose();
            double volumeChange = (curr.getVolume() - prev.getVolume()) / prev.getVolume();
            
            priceChanges.add(priceChange);
            volumeChanges.add(volumeChange);
        }
        
        // 计算简单相关系数
        double avgPriceChange = priceChanges.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double avgVolumeChange = volumeChanges.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        
        double numerator = 0;
        double priceVar = 0;
        double volumeVar = 0;
        
        for (int i = 0; i < priceChanges.size(); i++) {
            double priceDiff = priceChanges.get(i) - avgPriceChange;
            double volumeDiff = volumeChanges.get(i) - avgVolumeChange;
            
            numerator += priceDiff * volumeDiff;
            priceVar += priceDiff * priceDiff;
            volumeVar += volumeDiff * volumeDiff;
        }
        
        double denominator = Math.sqrt(priceVar * volumeVar);
        double correlation = denominator != 0 ? numerator / denominator : 0;
        
        Map<String, Object> relationship = new HashMap<>();
        relationship.put("correlation_coefficient", correlation);
        relationship.put("is_bullish_confluence", correlation > 0.3); // 价格和成交量正相关
        relationship.put("is_bearish_confluence", correlation < -0.3); // 价格和成交量负相关
        
        return relationship;
    }
}