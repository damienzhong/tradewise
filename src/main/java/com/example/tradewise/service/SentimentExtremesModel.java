package com.example.tradewise.service;

import com.example.tradewise.service.MarketAnalysisService.Candlestick;
import com.example.tradewise.service.MarketAnalysisService.TechnicalIndicators;
import com.example.tradewise.service.SignalFusionEngine.TradingSignal;
import com.example.tradewise.service.SignalFusionEngine.SignalModel;
import com.example.tradewise.service.SignalFusionEngine.Decision;

import java.util.*;

/**
 * 市场情绪极端模型（逆向版）
 * 实现DeepSeek方案中的模型5：市场情绪极端模型
 */
public class SentimentExtremesModel {
    
    /**
     * 检查是否出现情绪极端信号
     */
    public Optional<TradingSignal> detect(String symbol, Map<String, List<Candlestick>> multiTimeframeData) {
        // 获取4小时数据用于分析（平衡短期情绪和噪声）
        List<Candlestick> fourHourData = multiTimeframeData.get("4h");
        
        if (fourHourData == null || fourHourData.size() < 20) {
            return Optional.empty();
        }
        
        // 模拟获取资金费率、多空比、社交媒体情绪等数据
        double[] simulatedFundingRate = simulateFundingRate(fourHourData);
        double[] simulatedLongShortRatio = simulateLongShortRatio(fourHourData);
        double[] simulatedSocialSentiment = simulateSocialSentiment(fourHourData);
        
        // 计算贪婪指数和恐慌指数
        int greedIndex = calculateGreedIndex(simulatedFundingRate, simulatedLongShortRatio, fourHourData);
        int fearIndex = calculateFearIndex(simulatedFundingRate, simulatedLongShortRatio, fourHourData);
        
        // 检查是否达到极端阈值
        boolean isExtremeGreed = greedIndex > 85;
        boolean isExtremeFear = fearIndex > 85;
        
        if (!isExtremeGreed && !isExtremeFear) {
            return Optional.empty(); // 没有达到极端情绪
        }
        
        // 验证条件：技术面出现背离信号、成交量异常放大、关键位置出现反转形态
        boolean hasTechnicalDivergence = hasTechnicalDivergence(fourHourData);
        boolean hasVolumeSurge = hasVolumeSurge(fourHourData);
        boolean hasReversalPattern = hasReversalPattern(fourHourData);
        
        // 必须满足至少一个确认条件
        if (!hasTechnicalDivergence && !hasVolumeSurge && !hasReversalPattern) {
            return Optional.empty(); // 缺乏确认信号
        }
        
        // 确定方向和强度
        Decision direction;
        double strength;
        String reason;
        
        if (isExtremeGreed) {
            // 市场过热，准备做空
            direction = Decision.SHORT;
            strength = 8.0;
            reason = String.format("市场情绪极度贪婪(指数:%d)，出现%s，建议做空", 
                                 greedIndex, 
                                 getConfirmationDescription(hasTechnicalDivergence, hasVolumeSurge, hasReversalPattern));
        } else {
            // 市场恐慌，准备做多
            direction = Decision.LONG;
            strength = 8.0;
            reason = String.format("市场情绪极度恐慌(指数:%d)，出现%s，建议做多", 
                                 fearIndex, 
                                 getConfirmationDescription(hasTechnicalDivergence, hasVolumeSurge, hasReversalPattern));
        }
        
        // 计算置信度
        double confidence = calculateConfidence(greedIndex, fearIndex, 
                                             hasTechnicalDivergence, hasVolumeSurge, hasReversalPattern);
        
        // 生成元数据
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("greed_index", greedIndex);
        metadata.put("fear_index", fearIndex);
        metadata.put("technical_divergence", hasTechnicalDivergence);
        metadata.put("volume_surge", hasVolumeSurge);
        metadata.put("reversal_pattern", hasReversalPattern);
        metadata.put("funding_rate_simulation", simulatedFundingRate);
        metadata.put("long_short_ratio_simulation", simulatedLongShortRatio);
        metadata.put("social_sentiment_simulation", simulatedSocialSentiment);
        metadata.put("analysis_timeframe", "4h");
        
        TradingSignal signal = new TradingSignal(
            SignalModel.EXTREME_SENTIMENT,
            direction,
            strength,
            reason,
            confidence,
            metadata
        );
        
        return Optional.of(signal);
    }
    
    /**
     * 模拟资金费率（实际应用中应使用真实API）
     */
    private double[] simulateFundingRate(List<Candlestick> fourHourData) {
        double[] fundingRates = new double[fourHourData.size()];
        
        // 基于价格趋势和波动性模拟资金费率
        for (int i = 0; i < fourHourData.size(); i++) {
            Candlestick candle = fourHourData.get(i);
            
            // 基础费率
            double baseRate = 0.001; // 0.1%
            
            // 根据价格趋势调整
            if (i > 0) {
                Candlestick prevCandle = fourHourData.get(i - 1);
                double priceChange = (candle.getClose() - prevCandle.getClose()) / prevCandle.getClose();
                double trendAdjustment = priceChange * 0.0005; // 趋势对资金费率的影响
                
                // 根据波动性调整
                List<Double> atrList = TechnicalIndicators.calculateATR(
                    fourHourData.subList(Math.max(0, i - 13), i + 1), 14);
                double atr = atrList.isEmpty() || atrList.get(atrList.size() - 1) == null ? 
                           0 : atrList.get(atrList.size() - 1);
                double volAdjustment = atr > 0 ? (atr / candle.getClose()) * 0.0002 : 0;
                
                fundingRates[i] = baseRate + trendAdjustment + volAdjustment;
            } else {
                fundingRates[i] = baseRate;
            }
            
            // 限制在合理范围内 (-0.5% 到 0.5%)
            fundingRates[i] = Math.max(-0.005, Math.min(0.005, fundingRates[i]));
        }
        
        return fundingRates;
    }
    
    /**
     * 模拟多空比（实际应用中应使用真实API）
     */
    private double[] simulateLongShortRatio(List<Candlestick> fourHourData) {
        double[] ratios = new double[fourHourData.size()];
        
        // 基于价格和成交量模拟多空比
        for (int i = 0; i < fourHourData.size(); i++) {
            Candlestick candle = fourHourData.get(i);
            
            // 基础比率 1:1
            double baseRatio = 1.0;
            
            // 根据价格位置调整（价格越高，可能空头越多）
            double pricePosition = calculatePricePosition(candle, fourHourData, i);
            double positionAdjustment = (pricePosition - 0.5) * 0.5; // 调整范围 -0.25 到 +0.25
            
            // 根据成交量调整
            double volumeAdjustment = 0;
            if (i > 0) {
                double avgVolume = calculateAverageVolume(fourHourData, Math.max(0, i - 20), i);
                volumeAdjustment = (candle.getVolume() - avgVolume) / avgVolume * 0.1;
            }
            
            ratios[i] = Math.max(0.1, baseRatio + positionAdjustment + volumeAdjustment);
        }
        
        return ratios;
    }
    
    /**
     * 模拟社交媒体情绪（实际应用中应使用真实API）
     */
    private double[] simulateSocialSentiment(List<Candlestick> fourHourData) {
        double[] sentiments = new double[fourHourData.size()];
        
        // 基于价格变化模拟情绪
        for (int i = 0; i < fourHourData.size(); i++) {
            Candlestick candle = fourHourData.get(i);
            
            // 基础情绪 0.5 (中性)
            double baseSentiment = 0.5;
            
            // 根据近期价格走势调整
            double trend = calculateRecentTrend(fourHourData, i, 10);
            double trendAdjustment = trend * 0.3; // 趋势对情绪的影响
            
            // 根据波动性调整
            List<Double> atrList = TechnicalIndicators.calculateATR(
                fourHourData.subList(Math.max(0, i - 13), i + 1), 14);
            double atr = atrList.isEmpty() || atrList.get(atrList.size() - 1) == null ? 
                       0 : atrList.get(atrList.size() - 1);
            double volAdjustment = atr > 0 ? Math.min(0.2, (atr / candle.getClose()) * 0.5) : 0;
            
            // 组合调整
            double adjustedSentiment = baseSentiment + trendAdjustment + volAdjustment;
            sentiments[i] = Math.max(0, Math.min(1, adjustedSentiment)); // 限制在0-1之间
        }
        
        return sentiments;
    }
    
    /**
     * 计算贪婪指数
     */
    private int calculateGreedIndex(double[] fundingRate, double[] longShortRatio, List<Candlestick> fourHourData) {
        if (fundingRate.length == 0) {
            return 50; // 默认中性
        }
        
        int lastIndex = fundingRate.length - 1;
        double currentFundingRate = fundingRate[lastIndex];
        double currentLongShortRatio = longShortRatio[lastIndex];
        
        // 获取近期价格涨幅
        double recentPriceChange = calculateRecentPriceChange(fourHourData, 10);
        
        // 贪婪指数计算公式：资金费率 * 40% + 多空比 * 30% + 价格涨幅 * 30%
        // 归一化到0-100范围
        double normalizedFundingRate = normalizeFundingRate(currentFundingRate);
        double normalizedLongShortRatio = normalizeLongShortRatio(currentLongShortRatio);
        double normalizedPriceChange = normalizePriceChange(recentPriceChange);
        
        double greedScore = normalizedFundingRate * 0.4 + 
                           normalizedLongShortRatio * 0.3 + 
                           normalizedPriceChange * 0.3;
        
        return (int) Math.round(greedScore * 100);
    }
    
    /**
     * 计算恐慌指数
     */
    private int calculateFearIndex(double[] fundingRate, double[] longShortRatio, List<Candlestick> fourHourData) {
        if (fundingRate.length == 0) {
            return 50; // 默认中性
        }
        
        int lastIndex = fundingRate.length - 1;
        double currentFundingRate = fundingRate[lastIndex]; // 负资金费率
        double currentLongShortRatio = longShortRatio[lastIndex]; // 空头占比
        
        // 获取近期价格跌幅
        double recentPriceChange = calculateRecentPriceChange(fourHourData, 10);
        
        // 恐慌指数计算公式：负资金费率 * 40% + 空头占比 * 30% + 价格跌幅 * 30%
        // 归一化到0-100范围
        double normalizedNegFundingRate = normalizeNegativeFundingRate(currentFundingRate);
        double normalizedShortDominance = normalizeShortDominance(currentLongShortRatio);
        double normalizedNegPriceChange = normalizeNegativePriceChange(recentPriceChange);
        
        double fearScore = normalizedNegFundingRate * 0.4 + 
                          normalizedShortDominance * 0.3 + 
                          normalizedNegPriceChange * 0.3;
        
        return (int) Math.round(fearScore * 100);
    }
    
    /**
     * 归一化资金费率 (0-1)
     */
    private double normalizeFundingRate(double rate) {
        // 假设正常范围是 -0.5% 到 +0.5%
        return Math.max(0, Math.min(1, (rate + 0.005) / 0.01));
    }
    
    /**
     * 归一化多空比 (0-1)
     */
    private double normalizeLongShortRatio(double ratio) {
        // 假设正常范围是 0.1 到 10 (即1:10 到 10:1)
        return Math.max(0, Math.min(1, Math.log10(ratio + 0.1) / 1)); // 简化归一化
    }
    
    /**
     * 归一化价格变化 (0-1)
     */
    private double normalizePriceChange(double change) {
        // 假设正常范围是 -10% 到 +10%
        return Math.max(0, Math.min(1, (change + 0.1) / 0.2));
    }
    
    /**
     * 归一化负资金费率 (0-1)
     */
    private double normalizeNegativeFundingRate(double rate) {
        // 对于恐慌指数，我们关心负的资金费率
        return Math.max(0, Math.min(1, (-rate + 0.005) / 0.01)); // 如果资金费率为负，取其绝对值
    }
    
    /**
     * 归一化空头主导 (0-1)
     */
    private double normalizeShortDominance(double ratio) {
        // 当多空比小于1时，表示空头占优
        if (ratio < 1) {
            return (1 - ratio); // 空头越占优，值越大
        } else {
            return 0; // 多头占优时不贡献恐慌
        }
    }
    
    /**
     * 归一化负价格变化 (0-1)
     */
    private double normalizeNegativePriceChange(double change) {
        // 当价格下跌时，对恐慌指数有贡献
        if (change < 0) {
            return Math.max(0, Math.min(1, (-change) * 5)); // 价格跌幅越大，恐慌指数越高
        } else {
            return 0; // 价格上涨时不贡献恐慌
        }
    }
    
    /**
     * 检查技术面背离
     */
    private boolean hasTechnicalDivergence(List<Candlestick> fourHourData) {
        if (fourHourData.size() < 20) {
            return false;
        }
        
        // 检查价格与RSI的背离
        List<Double> prices = new ArrayList<>();
        for (Candlestick candle : fourHourData) {
            prices.add(candle.getClose());
        }
        
        List<Double> rsi = TechnicalIndicators.calculateRSI(fourHourData, 14);
        
        if (rsi.size() < 20) {
            return false;
        }
        
        // 检查最近的价格高点与RSI高点是否背离
        // 简化实现：检查价格创新高但RSI未创新高
        double recentHighPrice = Collections.max(prices.subList(Math.max(0, prices.size() - 10), prices.size()));
        int priceHighIndex = -1;
        for (int i = prices.size() - 1; i >= Math.max(0, prices.size() - 10); i--) {
            if (prices.get(i).equals(recentHighPrice)) {
                priceHighIndex = i;
                break;
            }
        }
        
        if (priceHighIndex == -1) {
            return false;
        }
        
        // 检查对应时间点及之前的RSI值
        Double correspondingRsi = rsi.get(priceHighIndex);
        if (correspondingRsi == null) {
            return false;
        }
        
        // 检查是否价格创新高但RSI没有创新高
        boolean hasDivergence = false;
        for (int i = Math.max(0, rsi.size() - 20); i < rsi.size(); i++) {
            Double rsiValue = rsi.get(i);
            if (rsiValue != null && rsiValue > correspondingRsi) {
                // 如果在近期有更高的RSI值，说明当前RSI不是新高，可能存在背离
                hasDivergence = true;
                break;
            }
        }
        
        return hasDivergence;
    }
    
    /**
     * 检查成交量激增
     */
    private boolean hasVolumeSurge(List<Candlestick> fourHourData) {
        if (fourHourData.size() < 20) {
            return false;
        }
        
        // 计算平均成交量
        double volumeSum = 0;
        for (Candlestick candle : fourHourData) {
            volumeSum += candle.getVolume();
        }
        double avgVolume = volumeSum / fourHourData.size();
        
        // 检查最近几根K线的成交量是否异常放大
        int recentBars = Math.min(5, fourHourData.size());
        for (int i = fourHourData.size() - recentBars; i < fourHourData.size(); i++) {
            if (fourHourData.get(i).getVolume() > avgVolume * 2.5) { // 成交量是平均值的2.5倍以上
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 检查反转形态
     */
    private boolean hasReversalPattern(List<Candlestick> fourHourData) {
        if (fourHourData.size() < 3) {
            return false;
        }
        
        // 检查最近3根K线是否形成反转形态
        // 如：锤子线、上吊线、吞没形态等
        Candlestick current = fourHourData.get(fourHourData.size() - 1);
        Candlestick prev = fourHourData.get(fourHourData.size() - 2);
        Candlestick prev2 = fourHourData.get(fourHourData.size() - 3);
        
        // 检查锤子线/上吊线形态
        boolean hasHammer = isHammerOrHangingMan(current);
        boolean hasEngulfing = isEngulfingPattern(prev, current); // 检查前一根和当前K线的吞没形态
        boolean hasThreeLineStrike = isThreeLineStrike(prev2, prev, current); // 三连跌/三连涨后反转
        
        return hasHammer || hasEngulfing || hasThreeLineStrike;
    }
    
    /**
     * 检查锤子线或上吊线
     */
    private boolean isHammerOrHangingMan(Candlestick candle) {
        double bodySize = Math.abs(candle.getClose() - candle.getOpen());
        double upperShadow = candle.getHigh() - Math.max(candle.getClose(), candle.getOpen());
        double lowerShadow = Math.min(candle.getClose(), candle.getOpen()) - candle.getLow();
        double totalRange = candle.getHigh() - candle.getLow();
        
        // 锤子线：下影线长度至少是实体的2倍，上影线很短
        boolean isHammer = lowerShadow >= bodySize * 2 && upperShadow <= bodySize * 0.1;
        // 上吊线：形态类似锤子线，但出现在上涨趋势中
        boolean isHangingMan = lowerShadow >= bodySize * 2 && upperShadow <= bodySize * 0.1;
        
        return isHammer || isHangingMan;
    }
    
    /**
     * 检查吞没形态
     */
    private boolean isEngulfingPattern(Candlestick prev, Candlestick current) {
        double prevBodyHigh = Math.max(prev.getClose(), prev.getOpen());
        double prevBodyLow = Math.min(prev.getClose(), prev.getOpen());
        double currBodyHigh = Math.max(current.getClose(), current.getOpen());
        double currBodyLow = Math.min(current.getClose(), current.getOpen());
        
        // 看涨吞没：前阴后阳，当前实体完全覆盖前一实体
        boolean bullishEngulfing = prev.getClose() < prev.getOpen() && // 前一根是阴线
                                  current.getClose() > current.getOpen() && // 当前是阳线
                                  currBodyLow <= prevBodyLow && currBodyHigh >= prevBodyHigh; // 当前实体完全覆盖前一实体
        
        // 看跌吞没：前阳后阴，当前实体完全覆盖前一实体
        boolean bearishEngulfing = prev.getClose() > prev.getOpen() && // 前一根是阳线
                                   current.getClose() < current.getOpen() && // 当前是阴线
                                   currBodyLow <= prevBodyLow && currBodyHigh >= prevBodyHigh; // 当前实体完全覆盖前一实体
        
        return bullishEngulfing || bearishEngulfing;
    }
    
    /**
     * 检查三连跌/三连涨后反转
     */
    private boolean isThreeLineStrike(Candlestick c1, Candlestick c2, Candlestick c3) {
        // 简化的三连跌后阳线反转模式
        boolean threeDown = c1.getClose() < c1.getOpen() && // 第一天阴线
                           c2.getClose() < c2.getOpen() && // 第二天阴线
                           c3.getClose() < c3.getOpen() && // 第三天阴线
                           c2.getClose() < c1.getClose() && // 逐级走低
                           c3.getClose() < c2.getClose() &&
                           Math.abs(c3.getOpen() - c3.getClose()) > (c3.getHigh() - c3.getLow()) * 0.7; // 第三天长阳线
        
        // 简化的三连涨后阴线反转模式
        boolean threeUp = c1.getClose() > c1.getOpen() && // 第一天阳线
                         c2.getClose() > c2.getOpen() && // 第二天阳线
                         c3.getClose() > c3.getOpen() && // 第三天阳线
                         c2.getClose() > c1.getClose() && // 逐级走高
                         c3.getClose() > c2.getClose() &&
                         Math.abs(c3.getOpen() - c3.getClose()) > (c3.getHigh() - c3.getLow()) * 0.7; // 第三天长阴线
        
        return threeDown || threeUp;
    }
    
    /**
     * 获取确认描述
     */
    private String getConfirmationDescription(boolean hasTechDiv, boolean hasVolSurge, boolean hasRevPattern) {
        List<String> confirmations = new ArrayList<>();
        if (hasTechDiv) confirmations.add("技术背离");
        if (hasVolSurge) confirmations.add("成交量放大");
        if (hasRevPattern) confirmations.add("反转形态");
        
        return String.join("、", confirmations);
    }
    
    /**
     * 计算置信度
     */
    private double calculateConfidence(int greedIndex, int fearIndex, 
                                     boolean hasTechDiv, boolean hasVolSurge, boolean hasRevPattern) {
        // 基础置信度
        double baseConfidence = 0.75;
        
        // 根据情绪极端程度调整
        int extremeLevel = Math.max(greedIndex, fearIndex);
        if (extremeLevel >= 90) {
            baseConfidence = 0.9; // 极端程度非常高
        } else if (extremeLevel >= 87) {
            baseConfidence = 0.85; // 极端程度高
        } else {
            baseConfidence = 0.75; // 标准极端水平
        }
        
        // 根据确认信号数量调整
        int confirmations = 0;
        if (hasTechDiv) confirmations++;
        if (hasVolSurge) confirmations++;
        if (hasRevPattern) confirmations++;
        
        switch (confirmations) {
            case 3:
                baseConfidence *= 1.2; // 三个确认信号，置信度提高
                break;
            case 2:
                baseConfidence *= 1.1; // 两个确认信号，置信度略提高
                break;
            case 1:
                baseConfidence *= 0.9; // 仅一个确认信号，置信度略降低
                break;
            default:
                baseConfidence *= 0.7; // 无确认信号，大幅降低置信度
        }
        
        // 根据数据量调整
        if (greedIndex + fearIndex > 160) { // 表示两个指数都很高，可能是数据噪声
            baseConfidence *= 0.8;
        }
        
        // 确保置信度在合理范围内
        return Math.max(0.1, Math.min(1.0, baseConfidence));
    }
    
    /**
     * 计算价格位置（在近期高低点之间的百分位）
     */
    private double calculatePricePosition(Candlestick currentCandle, List<Candlestick> data, int currentIndex) {
        if (data.size() < 20) {
            return 0.5; // 默认中间位置
        }
        
        // 获取近期高低点
        int lookback = Math.min(20, currentIndex + 1);
        List<Candlestick> recentData = data.subList(Math.max(0, currentIndex - lookback + 1), currentIndex + 1);
        
        double highestHigh = Double.MIN_VALUE;
        double lowestLow = Double.MAX_VALUE;
        
        for (Candlestick candle : recentData) {
            if (candle.getHigh() > highestHigh) highestHigh = candle.getHigh();
            if (candle.getLow() < lowestLow) lowestLow = candle.getLow();
        }
        
        if (highestHigh == lowestLow) {
            return 0.5; // 避免除零
        }
        
        return (currentCandle.getClose() - lowestLow) / (highestHigh - lowestLow);
    }
    
    /**
     * 计算平均成交量
     */
    private double calculateAverageVolume(List<Candlestick> data, int start, int end) {
        double sum = 0;
        int count = 0;
        
        for (int i = start; i < end && i < data.size(); i++) {
            sum += data.get(i).getVolume();
            count++;
        }
        
        return count > 0 ? sum / count : 0;
    }
    
    /**
     * 计算近期趋势
     */
    private double calculateRecentTrend(List<Candlestick> data, int currentIndex, int lookback) {
        if (data.size() < 2) {
            return 0;
        }
        
        int start = Math.max(0, currentIndex - lookback + 1);
        if (currentIndex <= start) {
            return 0;
        }
        
        double firstPrice = data.get(start).getClose();
        double lastPrice = data.get(currentIndex).getClose();
        
        return (lastPrice - firstPrice) / firstPrice;
    }
    
    /**
     * 计算近期价格变化
     */
    private double calculateRecentPriceChange(List<Candlestick> data, int lookback) {
        if (data.size() < 2) {
            return 0;
        }
        
        int start = Math.max(0, data.size() - lookback);
        if (data.size() - 1 <= start) {
            return 0;
        }
        
        double firstPrice = data.get(start).getClose();
        double lastPrice = data.get(data.size() - 1).getClose();
        
        return (lastPrice - firstPrice) / firstPrice;
    }
}