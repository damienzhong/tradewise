package com.example.tradewise.service;

import com.example.tradewise.config.TradeWiseProperties;
import com.example.tradewise.service.DataEngine.MarketRegime;
import com.example.tradewise.service.MarketAnalysisService.Candlestick;
import com.example.tradewise.service.MarketAnalysisService.TechnicalIndicators;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 智能数据引擎
 * 实现DeepSeek六维智能合约交易系统中的数据层功能
 */
public class SmartDataEngine {
    
    private static final Logger logger = LoggerFactory.getLogger(SmartDataEngine.class);
    
    @org.springframework.beans.factory.annotation.Autowired
    private MarketDataService marketDataService;
    
    @org.springframework.beans.factory.annotation.Autowired
    private TradeWiseProperties tradeWiseProperties;
    
    // 缓存多时间框架数据
    private final Map<String, Map<String, List<Candlestick>>> cachedMultiTimeframeData = new ConcurrentHashMap<>();
    
    /**
     * 市场状态检测器
     */
    public static class MarketStateDetector {
        
        /**
         * 市场状态枚举
         */
        public enum MarketState {
            STRONG_TREND_UP,      // 强趋势上涨
            WEAK_TREND_UP,        // 弱趋势上涨
            STRONG_TREND_DOWN,    // 强趋势下跌
            WEAK_TREND_DOWN,      // 弱趋势下跌
            WIDE_RANGE,           // 宽幅震荡
            NARROW_RANGE,         // 窄幅震荡
            HIGH_VOLATILE_CHAOTIC,// 高波动无序
            LOW_VOLATILE_SIDEWAYS // 低波动横盘
        }
        
        /**
         * 检测市场状态
         */
        public MarketState detectMarketState(String symbol, Map<String, List<Candlestick>> multiTimeframeData) {
            if (multiTimeframeData == null || multiTimeframeData.isEmpty()) {
                return MarketState.LOW_VOLATILE_SIDEWAYS; // 默认状态
            }
            
            // 获取1小时数据用于分析
            List<Candlestick> hourlyData = multiTimeframeData.get("1h");
            if (hourlyData == null || hourlyData.size() < 50) {
                return MarketState.LOW_VOLATILE_SIDEWAYS; // 数据不足时默认为低波动横盘
            }
            
            // 计算趋势指标
            boolean isStrongTrend = isStrongTrend(hourlyData);
            boolean isWeakTrend = isWeakTrend(hourlyData);
            boolean isRangeBound = isRangeBound(hourlyData);
            boolean isHighVolatility = isHighVolatility(hourlyData);
            boolean isChaotic = isChaotic(hourlyData);
            
            // 趋势判断
            if (isStrongTrend) {
                return isBullishTrend(hourlyData) ? MarketState.STRONG_TREND_UP : MarketState.STRONG_TREND_DOWN;
            } else if (isWeakTrend) {
                return isBullishTrend(hourlyData) ? MarketState.WEAK_TREND_UP : MarketState.WEAK_TREND_DOWN;
            }
            
            // 震荡判断
            if (isRangeBound) {
                boolean isWideRange = isWideRange(hourlyData);
                if (isWideRange) {
                    return MarketState.WIDE_RANGE;
                } else {
                    return MarketState.NARROW_RANGE;
                }
            }
            
            // 波动性判断
            if (isHighVolatility) {
                if (isChaotic) {
                    return MarketState.HIGH_VOLATILE_CHAOTIC;
                } else {
                    // 高波动但有序，可能是趋势初期
                    return isBullishTrend(hourlyData) ? MarketState.WEAK_TREND_UP : MarketState.WEAK_TREND_DOWN;
                }
            } else {
                return MarketState.LOW_VOLATILE_SIDEWAYS;
            }
        }
        
        /**
         * 检查是否为强趋势
         */
        private boolean isStrongTrend(List<Candlestick> data) {
            if (data.size() < 50) return false;
            
            // 使用价格偏离均线的程度判断
            List<Double> closes = new ArrayList<>();
            for (Candlestick candle : data) {
                closes.add(candle.getClose());
            }
            
            List<Double> ema50 = TechnicalIndicators.calculateEMA(data, 50);
            List<Double> ema200 = TechnicalIndicators.calculateEMA(data, 200);
            
            if (ema50.isEmpty() || ema200.isEmpty()) return false;
            
            Double latestEma50 = ema50.get(ema50.size() - 1);
            Double latestEma200 = ema200.get(ema200.size() - 1);
            double currentPrice = data.get(data.size() - 1).getClose();
            
            if (latestEma50 == null || latestEma200 == null) return false;
            
            // 检查价格是否明显偏离均线
            double deviation50 = Math.abs(currentPrice - latestEma50) / latestEma50;
            double deviation200 = Math.abs(currentPrice - latestEma200) / latestEma200;
            
            return deviation50 > 0.03 || deviation200 > 0.03; // 偏离3%以上为强趋势
        }
        
        /**
         * 检查是否为弱趋势
         */
        private boolean isWeakTrend(List<Candlestick> data) {
            if (data.size() < 50) return false;
            
            List<Double> ema50 = TechnicalIndicators.calculateEMA(data, 50);
            List<Double> ema200 = TechnicalIndicators.calculateEMA(data, 200);
            
            if (ema50.isEmpty() || ema200.isEmpty()) return false;
            
            Double latestEma50 = ema50.get(ema50.size() - 1);
            Double latestEma200 = ema200.get(ema200.size() - 1);
            double currentPrice = data.get(data.size() - 1).getClose();
            
            if (latestEma50 == null || latestEma200 == null) return false;
            
            double deviation50 = Math.abs(currentPrice - latestEma50) / latestEma50;
            double deviation200 = Math.abs(currentPrice - latestEma200) / latestEma200;
            
            return (deviation50 > 0.015 && deviation50 <= 0.03) || 
                   (deviation200 > 0.015 && deviation200 <= 0.03); // 1.5%-3%为弱趋势
        }
        
        /**
         * 检查是否为震荡市场
         */
        private boolean isRangeBound(List<Candlestick> data) {
            if (data.size() < 20) return false;
            
            // 计算价格在一段时间内的振幅
            double highestHigh = Double.MIN_VALUE;
            double lowestLow = Double.MAX_VALUE;
            
            for (int i = Math.max(0, data.size() - 20); i < data.size(); i++) {
                Candlestick candle = data.get(i);
                if (candle.getHigh() > highestHigh) highestHigh = candle.getHigh();
                if (candle.getLow() < lowestLow) lowestLow = candle.getLow();
            }
            
            // 检查收盘价是否在区间中部
            double currentPrice = data.get(data.size() - 1).getClose();
            double rangeMid = (highestHigh + lowestLow) / 2;
            double rangeSize = highestHigh - lowestLow;
            
            // 如果价格在区间中部，且波动相对较小，则认为是震荡
            return Math.abs(currentPrice - rangeMid) < rangeSize * 0.25 && rangeSize / rangeMid < 0.05; // 振幅小于5%
        }
        
        /**
         * 检查是否为宽幅震荡
         */
        private boolean isWideRange(List<Candlestick> data) {
            if (data.size() < 20) return false;
            
            double highestHigh = Double.MIN_VALUE;
            double lowestLow = Double.MAX_VALUE;
            
            for (int i = Math.max(0, data.size() - 20); i < data.size(); i++) {
                Candlestick candle = data.get(i);
                if (candle.getHigh() > highestHigh) highestHigh = candle.getHigh();
                if (candle.getLow() < lowestLow) lowestLow = candle.getLow();
            }
            
            double rangeSize = highestHigh - lowestLow;
            double avgPrice = (highestHigh + lowestLow) / 2;
            
            // 振幅大于3%认为是宽幅震荡
            return rangeSize / avgPrice > 0.03;
        }
        
        /**
         * 检查是否为高波动
         */
        private boolean isHighVolatility(List<Candlestick> data) {
            if (data.size() < 14) return false;
            
            List<Double> atrList = TechnicalIndicators.calculateATR(data, 14);
            if (atrList.isEmpty()) return false;
            
            Double latestAtr = atrList.get(atrList.size() - 1);
            if (latestAtr == null) return false;
            
            double currentPrice = data.get(data.size() - 1).getClose();
            double atrPercentage = (latestAtr / currentPrice) * 100;
            
            // ATR百分比大于2.5%认为是高波动
            return atrPercentage > 2.5;
        }
        
        /**
         * 检查市场是否混乱
         */
        private boolean isChaotic(List<Candlestick> data) {
            if (data.size() < 10) return false;
            
            // 检查价格变化的方向一致性
            int upCount = 0, downCount = 0;
            
            for (int i = 1; i < data.size(); i++) {
                if (data.get(i).getClose() > data.get(i-1).getClose()) {
                    upCount++;
                } else if (data.get(i).getClose() < data.get(i-1).getClose()) {
                    downCount++;
                }
            }
            
            // 如果上涨和下跌次数都很多，且比例接近，认为市场混乱
            int totalCount = upCount + downCount;
            if (totalCount < 5) return false;
            
            double upRatio = (double) upCount / totalCount;
            double downRatio = (double) downCount / totalCount;
            
            // 方向变化过于频繁且比例接近，认为是混乱
            return Math.abs(upRatio - downRatio) < 0.3 && totalCount > data.size() * 0.7;
        }
        
        /**
         * 检查是否为看涨趋势
         */
        private boolean isBullishTrend(List<Candlestick> data) {
            if (data.size() < 20) return false;
            
            double recentPrice = data.get(data.size() - 1).getClose();
            double pastPrice = data.get(Math.max(0, data.size() - 20)).getClose();
            
            return recentPrice > pastPrice;
        }
    }
    
    /**
     * 获取多时间框架数据
     */
    public Map<String, List<Candlestick>> getMultiTimeframeData(String symbol) {
        // 检查缓存
        Map<String, List<Candlestick>> cached = cachedMultiTimeframeData.get(symbol);
        if (cached != null) {
            // 检查缓存是否过期（假设5分钟过期）
            boolean isExpired = false;
            for (List<Candlestick> data : cached.values()) {
                if (!data.isEmpty()) {
                    long now = System.currentTimeMillis();
                    long lastCandleTime = data.get(data.size() - 1).getCloseTime();
                    if (now - lastCandleTime > 5 * 60 * 1000) { // 5分钟
                        isExpired = true;
                        break;
                    }
                }
            }
            
            if (!isExpired) {
                logger.debug("使用缓存的多时间框架数据: {}", symbol);
                return cached;
            }
        }
        
        // 获取新的多时间框架数据
        Map<String, List<Candlestick>> result = new HashMap<>();
        
        // 定义时间框架
        String[] timeframes = {"1m", "5m", "15m", "1h", "4h", "1d"};
        
        for (String timeframe : timeframes) {
            try {
                // 根据时间框架获取不同数量的K线数据
                int limit = getLimitForTimeframe(timeframe);
                
                List<Candlestick> candlesticks = marketDataService.getKlines(symbol, timeframe, limit);
                
                if (!candlesticks.isEmpty()) {
                    result.put(timeframe, candlesticks);
                    logger.debug("获取 {} 数据: {} 根K线", timeframe, candlesticks.size());
                } else {
                    logger.warn("未能获取 {} 的 {} 数据", symbol, timeframe);
                }
            } catch (Exception e) {
                logger.error("获取 {} 的 {} 数据时发生错误", symbol, timeframe, e);
            }
        }
        
        // 缓存结果
        if (!result.isEmpty()) {
            cachedMultiTimeframeData.put(symbol, result);
        }
        
        return result;
    }
    
    /**
     * 根据时间框架确定获取的数据量
     */
    private int getLimitForTimeframe(String timeframe) {
        switch (timeframe) {
            case "1m":
                return 300; // 5小时数据
            case "5m":
                return 288; // 1天数据
            case "15m":
                return 192; // 2天数据
            case "1h":
                return 168; // 1周数据
            case "4h":
                return 126; // 3周数据
            case "1d":
                return 90;  // 3个月数据
            default:
                return 100;
        }
    }
    
    /**
     * 计算衍生数据：波动率、相关性、市场情绪
     */
    public Map<String, Object> calculateDerivedData(String symbol, Map<String, List<Candlestick>> multiTimeframeData) {
        Map<String, Object> derivedData = new HashMap<>();
        
        // 计算各时间框架的波动率
        for (Map.Entry<String, List<Candlestick>> entry : multiTimeframeData.entrySet()) {
            String timeframe = entry.getKey();
            List<Candlestick> data = entry.getValue();
            
            if (!data.isEmpty()) {
                double volatility = calculateVolatility(data);
                derivedData.put(timeframe + "_volatility", volatility);
                
                // 计算平均真实波幅
                List<Double> atrList = TechnicalIndicators.calculateATR(data, 14);
                if (!atrList.isEmpty() && atrList.get(atrList.size() - 1) != null) {
                    derivedData.put(timeframe + "_atr", atrList.get(atrList.size() - 1));
                }
            }
        }
        
        // 计算相关性（简化实现：使用1小时数据与其他时间框架的相关性）
        List<Candlestick> hourlyData = multiTimeframeData.get("1h");
        if (hourlyData != null && !hourlyData.isEmpty()) {
            List<Double> hourlyReturns = calculateReturns(hourlyData);
            
            for (Map.Entry<String, List<Candlestick>> entry : multiTimeframeData.entrySet()) {
                String timeframe = entry.getKey();
                List<Candlestick> data = entry.getValue();
                
                if (!"1h".equals(timeframe) && data != null && !data.isEmpty()) {
                    List<Double> returns = calculateReturns(data);
                    double correlation = calculateCorrelation(hourlyReturns, returns);
                    derivedData.put("correlation_1h_vs_" + timeframe, correlation);
                }
            }
        }
        
        // 计算市场情绪指标（基于价格动量和RSI）
        List<Candlestick> dailyData = multiTimeframeData.get("1d");
        if (dailyData != null && dailyData.size() >= 14) {
            List<Double> rsi = TechnicalIndicators.calculateRSI(dailyData, 14);
            if (!rsi.isEmpty() && rsi.get(rsi.size() - 1) != null) {
                double currentRsi = rsi.get(rsi.size() - 1);
                
                // 简化的市场情绪指标
                double sentiment = currentRsi > 70 ? 1.0 : (currentRsi < 30 ? 0.0 : 0.5);
                derivedData.put("sentiment_index", sentiment);
            }
        }
        
        return derivedData;
    }
    
    /**
     * 计算波动率
     */
    private double calculateVolatility(List<Candlestick> data) {
        if (data.size() < 2) return 0.0;
        
        // 计算对数收益率的标准差
        List<Double> logReturns = new ArrayList<>();
        for (int i = 1; i < data.size(); i++) {
            double prevClose = data.get(i - 1).getClose();
            double currClose = data.get(i).getClose();
            if (prevClose > 0) {
                double logReturn = Math.log(currClose / prevClose);
                logReturns.add(logReturn);
            }
        }
        
        if (logReturns.isEmpty()) return 0.0;
        
        // 计算标准差
        double mean = logReturns.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double variance = logReturns.stream()
                .mapToDouble(r -> Math.pow(r - mean, 2))
                .average()
                .orElse(0.0);
        
        return Math.sqrt(variance) * Math.sqrt(252); // 年化波动率
    }
    
    /**
     * 计算收益率序列
     */
    private List<Double> calculateReturns(List<Candlestick> data) {
        List<Double> returns = new ArrayList<>();
        for (int i = 1; i < data.size(); i++) {
            double prevClose = data.get(i - 1).getClose();
            double currClose = data.get(i).getClose();
            if (prevClose > 0) {
                returns.add((currClose - prevClose) / prevClose);
            }
        }
        return returns;
    }
    
    /**
     * 计算相关系数
     */
    private double calculateCorrelation(List<Double> series1, List<Double> series2) {
        if (series1.isEmpty() || series2.isEmpty()) return 0.0;
        
        int size = Math.min(series1.size(), series2.size());
        if (size < 2) return 0.0;
        
        // 取较短序列的长度
        List<Double> s1 = series1.subList(Math.max(0, series1.size() - size), series1.size());
        List<Double> s2 = series2.subList(Math.max(0, series2.size() - size), series2.size());
        
        // 计算均值
        double mean1 = s1.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double mean2 = s2.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        
        // 计算协方差和标准差
        double covariance = 0.0;
        double variance1 = 0.0;
        double variance2 = 0.0;
        
        for (int i = 0; i < size; i++) {
            double diff1 = s1.get(i) - mean1;
            double diff2 = s2.get(i) - mean2;
            covariance += diff1 * diff2;
            variance1 += diff1 * diff1;
            variance2 += diff2 * diff2;
        }
        
        if (variance1 == 0 || variance2 == 0) return 0.0;
        
        return covariance / Math.sqrt(variance1 * variance2);
    }
    
    /**
     * 将内部市场状态映射到DataEngine的MarketRegime
     */
    public MarketRegime mapToMarketRegime(MarketStateDetector.MarketState state) {
        switch (state) {
            case STRONG_TREND_UP:
            case STRONG_TREND_DOWN:
                return MarketRegime.STRONG_TREND;
            case WEAK_TREND_UP:
            case WEAK_TREND_DOWN:
                return MarketRegime.WEAK_TREND;
            case WIDE_RANGE:
            case NARROW_RANGE:
            case LOW_VOLATILE_SIDEWAYS:
                return MarketRegime.RANGE;
            case HIGH_VOLATILE_CHAOTIC:
                return MarketRegime.VOLATILITY_EXPAND;
            default:
                return MarketRegime.RANGE;
        }
    }
}