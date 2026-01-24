package com.example.tradewise.service;

import com.example.tradewise.config.TradeWiseProperties;
import com.example.tradewise.entity.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;

import java.time.LocalDateTime;

import javax.mail.internet.MimeMessage;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.Objects;

/**
 * 市场行情分析服务
 * 用于分析市场数据并生成交易信号
 */
@Service
public class MarketAnalysisService {

    private static final Logger logger = LoggerFactory.getLogger(MarketAnalysisService.class);

    @Autowired
    private EmailService emailService;

    @Autowired
    private TradeWiseProperties tradeWiseProperties;

    @Autowired
    private MarketDataService marketDataService;

    @Autowired
    private SystemHealthService systemHealthService;  // 注入系统健康服务

    @Autowired
    private EconomicCalendarFilter economicCalendarFilter;  // 注入经济日历过滤器

    @Autowired
    private SignalPerformanceTracker signalPerformanceTracker;  // 注入信号绩效跟踪器

    @Autowired
    private MarketRegimeController marketRegimeController;  // 注入市场状态总控器

    @Autowired
    private SignalStateManager signalStateManager;  // 注入信号状态管理器

    @Autowired
    private SignalValidator signalValidator;  // 注入信号验证器

    @Autowired
    private AdaptiveParameterSystem adaptiveParameterSystem;  // 注入自适应参数系统

    @Autowired
    private PerformanceMonitor performanceMonitor;  // 注入性能监控服务

    @Autowired
    private SignalEnhancer signalEnhancer;  // 注入信号增强器

    @Autowired
    private EmailFilterStrategy emailFilterStrategy;  // 注入邮件过滤策略

    /**
     * K线数据类
     */
    public static class Candlestick {
        private String symbol;
        private long openTime;
        private double open;
        private double high;
        private double low;
        private double close;
        private double volume;
        private long closeTime;
        private double quoteAssetVolume;
        private int numberOfTrades;
        private double takerBuyBaseAssetVolume;
        private double takerBuyQuoteAssetVolume;

        // 构造函数
        public Candlestick(String symbol, long openTime, double open, double high, double low, double close, double volume) {
            this.symbol = symbol;
            this.openTime = openTime;
            this.open = open;
            this.high = high;
            this.low = low;
            this.close = close;
            this.volume = volume;
        }

        // Getters and setters
        public String getSymbol() {
            return symbol;
        }

        public void setSymbol(String symbol) {
            this.symbol = symbol;
        }

        public long getOpenTime() {
            return openTime;
        }

        public void setOpenTime(long openTime) {
            this.openTime = openTime;
        }

        public double getOpen() {
            return open;
        }

        public void setOpen(double open) {
            this.open = open;
        }

        public double getHigh() {
            return high;
        }

        public void setHigh(double high) {
            this.high = high;
        }

        public double getLow() {
            return low;
        }

        public void setLow(double low) {
            this.low = low;
        }

        public double getClose() {
            return close;
        }

        public void setClose(double close) {
            this.close = close;
        }

        public double getVolume() {
            return volume;
        }

        public void setVolume(double volume) {
            this.volume = volume;
        }

        public long getCloseTime() {
            return closeTime;
        }

        public void setCloseTime(long closeTime) {
            this.closeTime = closeTime;
        }

        public double getQuoteAssetVolume() {
            return quoteAssetVolume;
        }

        public void setQuoteAssetVolume(double quoteAssetVolume) {
            this.quoteAssetVolume = quoteAssetVolume;
        }

        public int getNumberOfTrades() {
            return numberOfTrades;
        }

        public void setNumberOfTrades(int numberOfTrades) {
            this.numberOfTrades = numberOfTrades;
        }

        public double getTakerBuyBaseAssetVolume() {
            return takerBuyBaseAssetVolume;
        }

        public void setTakerBuyBaseAssetVolume(double takerBuyBaseAssetVolume) {
            this.takerBuyBaseAssetVolume = takerBuyBaseAssetVolume;
        }

        public double getTakerBuyQuoteAssetVolume() {
            return takerBuyQuoteAssetVolume;
        }

        public void setTakerBuyQuoteAssetVolume(double takerBuyQuoteAssetVolume) {
            this.takerBuyQuoteAssetVolume = takerBuyQuoteAssetVolume;
        }
    }

    /**
     * 市场行情描述类
     */
    public static class MarketDescription {
        private String symbol;
        private double currentPrice;
        private String trendStatus; // 趋势状态
        private String volatilityLevel; // 波动水平
        private String volumeStatus; // 成交量状态
        private String rsiStatus; // RSI状态
        private String bollingerStatus; // 布林带状态
        private String overallMarketView; // 整体市场观点
        private LocalDateTime timestamp;
        private String marketStateHash; // 市场状态哈希值，用于判断是否发生变化

        public MarketDescription(String symbol, double currentPrice) {
            this.symbol = symbol;
            this.currentPrice = currentPrice;
            this.timestamp = LocalDateTime.now();
        }

        // Getters and setters
        public String getSymbol() {
            return symbol;
        }

        public void setSymbol(String symbol) {
            this.symbol = symbol;
        }

        public double getCurrentPrice() {
            return currentPrice;
        }

        public void setCurrentPrice(double currentPrice) {
            this.currentPrice = currentPrice;
        }

        public String getTrendStatus() {
            return trendStatus;
        }

        public void setTrendStatus(String trendStatus) {
            this.trendStatus = trendStatus;
        }

        public String getVolatilityLevel() {
            return volatilityLevel;
        }

        public void setVolatilityLevel(String volatilityLevel) {
            this.volatilityLevel = volatilityLevel;
        }

        public String getVolumeStatus() {
            return volumeStatus;
        }

        public void setVolumeStatus(String volumeStatus) {
            this.volumeStatus = volumeStatus;
        }

        public String getRsiStatus() {
            return rsiStatus;
        }

        public void setRsiStatus(String rsiStatus) {
            this.rsiStatus = rsiStatus;
        }

        public String getBollingerStatus() {
            return bollingerStatus;
        }

        public void setBollingerStatus(String bollingerStatus) {
            this.bollingerStatus = bollingerStatus;
        }

        public String getOverallMarketView() {
            return overallMarketView;
        }

        public void setOverallMarketView(String overallMarketView) {
            this.overallMarketView = overallMarketView;
        }

        public LocalDateTime getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
        }

        public String getMarketStateHash() {
            return marketStateHash;
        }

        public void setMarketStateHash(String marketStateHash) {
            this.marketStateHash = marketStateHash;
        }
    }

    /**
     * 合并市场分析结果类
     */
    public static class CombinedMarketAnalysis {
        private List<TradingSignal> allSignals;
        private List<MarketDescription> allMarketDescriptions;
        private LocalDateTime analysisTime;

        public CombinedMarketAnalysis() {
            this.allSignals = new ArrayList<>();
            this.allMarketDescriptions = new ArrayList<>();
            this.analysisTime = LocalDateTime.now();
        }

        public void addSignal(TradingSignal signal) {
            this.allSignals.add(signal);
        }

        public void addMarketDescription(MarketDescription description) {
            this.allMarketDescriptions.add(description);
        }

        // Getters and setters
        public List<TradingSignal> getAllSignals() {
            return allSignals;
        }

        public void setAllSignals(List<TradingSignal> allSignals) {
            this.allSignals = allSignals;
        }

        public List<MarketDescription> getAllMarketDescriptions() {
            return allMarketDescriptions;
        }

        public void setAllMarketDescriptions(List<MarketDescription> allMarketDescriptions) {
            this.allMarketDescriptions = allMarketDescriptions;
        }

        public LocalDateTime getAnalysisTime() {
            return analysisTime;
        }

        public void setAnalysisTime(LocalDateTime analysisTime) {
            this.analysisTime = analysisTime;
        }
    }

    /**
     * 交易信号类
     */
    public static class TradingSignal {
        public enum SignalType {
            BUY, SELL, HOLD
        }

        private String symbol;
        private SignalType signalType;
        private String indicator;
        private double price;
        private String reason;
        private String suggestion; // 开单建议
        private String confidence; // 信号置信度
        private LocalDateTime timestamp;
        private int score; // 信号评分 (0-10)
        private Map<String, Integer> scoreDetails; // 评分细节
        private double stopLoss; // 止损价格
        private double takeProfit; // 止盈价格
        private String signalLevel; // 信号等级 (LEVEL_1, LEVEL_2, LEVEL_3)
        
        // 信号解释信息，用于回放和调试
        private Map<String, Object> signalExplanation;
        
        // 合约交易新增字段
        private int leverage = 10; // 杠杆倍数，默认10倍
        private double positionSize; // 仓位大小（张数或合约数）
        private double marginRequired; // 所需保证金
        private double liquidationPrice; // 预估爆仓价格
        private double fundingRate; // 资金费率
        private String contractType = "PERPETUAL"; // 合约类型：永续合约、交割合约等
        private double roiPercentage; // 预期收益率百分比
        private double maxPositionRisk; // 最大头寸风险

        public TradingSignal(String symbol, SignalType signalType, String indicator, double price, String reason, String suggestion, String confidence) {
            this.symbol = symbol;
            this.signalType = signalType;
            this.indicator = indicator;
            this.price = price;
            this.reason = reason;
            this.suggestion = suggestion;
            this.confidence = confidence;
            this.score = 0;
            this.scoreDetails = new HashMap<>();
            this.stopLoss = 0.0;
            this.takeProfit = 0.0;
            this.signalLevel = "LEVEL_3"; // 默认为观察级别
            this.signalExplanation = new HashMap<>(); // 初始化信号解释
            this.timestamp = LocalDateTime.now();
            // 初始化合约交易相关参数
            this.leverage = 10; // 默认10倍杠杆
            this.positionSize = 0.0;
            this.marginRequired = 0.0;
            this.liquidationPrice = 0.0;
            this.fundingRate = 0.0;
            this.roiPercentage = 0.0;
            this.maxPositionRisk = 0.0;
        }

        // Getters
        public String getSymbol() {
            return symbol;
        }

        public SignalType getSignalType() {
            return signalType;
        }

        public void setIndicator(String indicator) {
            this.indicator = indicator;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }

        public void setSuggestion(String suggestion) {
            this.suggestion = suggestion;
        }

        public void setConfidence(String confidence) {
            this.confidence = confidence;
        }

        public String getIndicator() {
            return indicator;
        }

        public double getPrice() {
            return price;
        }

        public String getReason() {
            return reason;
        }

        public String getSuggestion() {
            return suggestion;
        }

        public String getConfidence() {
            return confidence;
        }

        public LocalDateTime getTimestamp() {
            return timestamp;
        }

        public int getScore() {
            return score;
        }

        public void setScore(int score) {
            this.score = score;
            // 根据评分设置信号等级
            if (score >= 8) {
                this.signalLevel = "LEVEL_1";
            } else if (score >= 6) {
                this.signalLevel = "LEVEL_2";
            } else {
                this.signalLevel = "LEVEL_3";
            }
        }

        public Map<String, Integer> getScoreDetails() {
            return scoreDetails;
        }

        public void setScoreDetails(Map<String, Integer> scoreDetails) {
            this.scoreDetails = scoreDetails;
        }

        public double getStopLoss() {
            return stopLoss;
        }

        public void setStopLoss(double stopLoss) {
            this.stopLoss = stopLoss;
        }

        public double getTakeProfit() {
            return takeProfit;
        }

        public void setTakeProfit(double takeProfit) {
            this.takeProfit = takeProfit;
        }

        public String getSignalLevel() {
            return signalLevel;
        }

        public void setSignalLevel(String signalLevel) {
            this.signalLevel = signalLevel;
        }
        
        // 信号解释信息的getter和setter
        public Map<String, Object> getSignalExplanation() {
            return signalExplanation;
        }
        
        public void setSignalExplanation(Map<String, Object> signalExplanation) {
            this.signalExplanation = signalExplanation;
        }
        
        // 合约交易相关字段的Getter和Setter
        public int getLeverage() {
            return leverage;
        }

        public void setLeverage(int leverage) {
            this.leverage = leverage;
        }

        public double getPositionSize() {
            return positionSize;
        }

        public void setPositionSize(double positionSize) {
            this.positionSize = positionSize;
        }

        public double getMarginRequired() {
            return marginRequired;
        }

        public void setMarginRequired(double marginRequired) {
            this.marginRequired = marginRequired;
        }

        public double getLiquidationPrice() {
            return liquidationPrice;
        }

        public void setLiquidationPrice(double liquidationPrice) {
            this.liquidationPrice = liquidationPrice;
        }

        public double getFundingRate() {
            return fundingRate;
        }

        public void setFundingRate(double fundingRate) {
            this.fundingRate = fundingRate;
        }

        public String getContractType() {
            return contractType;
        }

        public void setContractType(String contractType) {
            this.contractType = contractType;
        }

        public double getRoiPercentage() {
            return roiPercentage;
        }

        public void setRoiPercentage(double roiPercentage) {
            this.roiPercentage = roiPercentage;
        }

        public double getMaxPositionRisk() {
            return maxPositionRisk;
        }

        public void setMaxPositionRisk(double maxPositionRisk) {
            this.maxPositionRisk = maxPositionRisk;
        }
    }

    /**
     * 技术指标计算工具类
     */
    public static class TechnicalIndicators {

        /**
         * 计算简单移动平均线(SMA)
         */
        public static List<Double> calculateSMA(List<Candlestick> candles, int period) {
            List<Double> smaValues = new ArrayList<>();
            if (candles.size() < period) {
                for (int i = 0; i < candles.size(); i++) {
                    smaValues.add(null);
                }
                return smaValues;
            }

            for (int i = 0; i < candles.size(); i++) {
                if (i < period - 1) {
                    smaValues.add(null);
                    continue;
                }

                double sum = 0;
                for (int j = i - period + 1; j <= i; j++) {
                    sum += candles.get(j).getClose();
                }
                smaValues.add(sum / period);
            }

            return smaValues;
        }

        /**
         * 计算指数移动平均线(EMA)
         */
        public static List<Double> calculateEMA(List<Candlestick> candles, int period) {
            List<Double> emaValues = new ArrayList<>();
            if (candles.isEmpty()) return emaValues;

            double multiplier = 2.0 / (period + 1);
            double ema = candles.get(0).getClose(); // 初始值为第一个收盘价

            for (int i = 0; i < candles.size(); i++) {
                if (i == 0) {
                    ema = candles.get(i).getClose();
                } else {
                    ema = (candles.get(i).getClose() - ema) * multiplier + ema;
                }
                if (i < period - 1) {
                    emaValues.add(null);
                } else {
                    emaValues.add(ema);
                }
            }

            return emaValues;
        }

        /**
         * 计算RSI相对强弱指数
         */
        public static List<Double> calculateRSI(List<Candlestick> candles, int period) {
            List<Double> rsiValues = new ArrayList<>();
            if (candles.size() <= 1) return rsiValues;

            // 初始化前几个值为null
            for (int i = 0; i < period; i++) {
                rsiValues.add(null);
            }

            // 计算每日价格变化
            List<Double> changes = new ArrayList<>();
            for (int i = 1; i < candles.size(); i++) {
                changes.add(candles.get(i).getClose() - candles.get(i - 1).getClose());
            }

            // 计算初始RSI
            double gainSum = 0;
            double lossSum = 0;
            for (int i = 0; i < period; i++) {
                double change = changes.get(i);
                if (change >= 0) {
                    gainSum += change;
                } else {
                    lossSum += Math.abs(change);
                }
            }

            double avgGain = gainSum / period;
            double avgLoss = lossSum / period;

            if (avgLoss == 0) {
                rsiValues.add(100.0);
            } else {
                double rs = avgGain / avgLoss;
                double rsi = 100 - (100 / (1 + rs));
                rsiValues.add(rsi);
            }

            // 计算后续RSI值
            for (int i = period; i < changes.size(); i++) {
                double currentChange = changes.get(i);
                double currentGain = currentChange >= 0 ? currentChange : 0;
                double currentLoss = currentChange < 0 ? Math.abs(currentChange) : 0;

                avgGain = (avgGain * (period - 1) + currentGain) / period;
                avgLoss = (avgLoss * (period - 1) + currentLoss) / period;

                if (avgLoss == 0) {
                    rsiValues.add(100.0);
                } else {
                    double rs = avgGain / avgLoss;
                    double rsi = 100 - (100 / (1 + rs));
                    rsiValues.add(rsi);
                }
            }

            return rsiValues;
        }

        /**
         * 计算MACD指标
         */
        public static Map<String, List<Double>> calculateMACD(List<Candlestick> candles, int fastPeriod, int slowPeriod, int signalPeriod) {
            Map<String, List<Double>> macdResult = new HashMap<>();

            // 计算快线EMA
            List<Double> emaFast = calculateEMA(candles, fastPeriod);
            // 计算慢线EMA
            List<Double> emaSlow = calculateEMA(candles, slowPeriod);

            List<Double> dif = new ArrayList<>(); // DIF线 (快线 - 慢线)
            List<Double> dea = new ArrayList<>(); // DEA线 (DIF的EMA)
            List<Double> histogram = new ArrayList<>(); // MACD柱状图 (DIF - DEA) * 2

            for (int i = 0; i < candles.size(); i++) {
                if (emaFast.get(i) == null || emaSlow.get(i) == null) {
                    dif.add(null);
                    dea.add(null);
                    histogram.add(null);
                } else {
                    double currentDif = emaFast.get(i) - emaSlow.get(i);
                    dif.add(currentDif);

                    // 计算DEA线（DIF的EMA）
                    if (i == 0) {
                        dea.add(currentDif);
                    } else {
                        // 使用2/(signalPeriod+1)作为平滑系数
                        double alpha = 2.0 / (signalPeriod + 1);
                        Double prevDea = dea.get(i - 1);
                        if (prevDea != null) {
                            double currentDea = alpha * currentDif + (1 - alpha) * prevDea;
                            dea.add(currentDea);
                        } else {
                            dea.add(currentDif);
                        }
                    }

                    // 计算MACD柱状图
                    Double currentDea = dea.get(i);
                    if (currentDea != null) {
                        histogram.add((currentDif - currentDea) * 2);
                    } else {
                        histogram.add(null);
                    }
                }
            }

            macdResult.put("dif", dif);
            macdResult.put("dea", dea);
            macdResult.put("histogram", histogram);

            return macdResult;
        }

        /**
         * 计算布林带
         */
        public static Map<String, List<Double>> calculateBollingerBands(List<Candlestick> candles, int period, double stdMultiplier) {
            Map<String, List<Double>> bands = new HashMap<>();
            List<Double> upperBand = new ArrayList<>();
            List<Double> middleBand = new ArrayList<>();
            List<Double> lowerBand = new ArrayList<>();

            List<Double> smaValues = calculateSMA(candles, period);

            for (int i = 0; i < candles.size(); i++) {
                if (smaValues.get(i) == null) {
                    upperBand.add(null);
                    middleBand.add(null);
                    lowerBand.add(null);
                    continue;
                }

                // 计算标准差
                double sumSquaredDiff = 0;
                int startIndex = Math.max(0, i - period + 1);
                int actualPeriod = i - startIndex + 1;

                for (int j = startIndex; j <= i; j++) {
                    double diff = candles.get(j).getClose() - smaValues.get(i);
                    sumSquaredDiff += diff * diff;
                }

                double stdDev = Math.sqrt(sumSquaredDiff / actualPeriod);

                upperBand.add(smaValues.get(i) + stdDev * stdMultiplier);
                middleBand.add(smaValues.get(i));
                lowerBand.add(smaValues.get(i) - stdDev * stdMultiplier);
            }

            bands.put("upper", upperBand);
            bands.put("middle", middleBand);
            bands.put("lower", lowerBand);

            return bands;
        }

        /**
         * 计算ATR（平均真实波幅）用于风险管理
         */
        public static List<Double> calculateATR(List<Candlestick> candles, int period) {
            List<Double> atrList = new ArrayList<>();

            for (int i = 0; i < candles.size(); i++) {
                if (i == 0) {
                    // 第一天的TR是当日最高价减去最低价
                    double tr = candles.get(i).getHigh() - candles.get(i).getLow();
                    atrList.add(tr);
                } else {
                    // TR = max(high-low, |high-prev_close|, |low-prev_close|)
                    double highLow = candles.get(i).getHigh() - candles.get(i).getLow();
                    double highPrevClose = Math.abs(candles.get(i).getHigh() - candles.get(i - 1).getClose());
                    double lowPrevClose = Math.abs(candles.get(i).getLow() - candles.get(i - 1).getClose());
                    double tr = Math.max(Math.max(highLow, highPrevClose), lowPrevClose);

                    if (i < period) {
                        // 前period天使用简单平均
                        double sum = 0;
                        int count = Math.min(i + 1, period);
                        for (int j = Math.max(0, i - period + 1); j <= i; j++) {
                            double dayHighLow = candles.get(j).getHigh() - candles.get(j).getLow();
                            double dayHighPrevClose = j > 0 ? Math.abs(candles.get(j).getHigh() - candles.get(j - 1).getClose()) : 0;
                            double dayLowPrevClose = j > 0 ? Math.abs(candles.get(j).getLow() - candles.get(j - 1).getClose()) : 0;
                            double dayTr = Math.max(Math.max(dayHighLow, dayHighPrevClose), dayLowPrevClose);
                            sum += dayTr;
                        }
                        atrList.add(sum / count);
                    } else {
                        // 之后使用平滑移动平均
                        double prevAtr = atrList.get(i - 1);
                        double currentAtr = (prevAtr * (period - 1) + tr) / period;
                        atrList.add(currentAtr);
                    }
                }
            }

            return atrList;
        }

        /**
         * 计算市场趋势强度（基于价格与长期均线的关系）
         */
        public static List<Integer> calculateTrendStrength(List<Candlestick> candles, int longTermMaPeriod) {
            List<Integer> trendStrength = new ArrayList<>();
            List<Double> longTermMa = calculateSMA(candles, longTermMaPeriod);

            for (int i = 0; i < candles.size(); i++) {
                if (longTermMa.get(i) == null) {
                    trendStrength.add(0); // 无趋势
                } else {
                    double currentPrice = candles.get(i).getClose();
                    double ma = longTermMa.get(i);

                    // 判断价格相对于均线的位置
                    if (currentPrice > ma * 1.01) { // 价格高于均线1%
                        trendStrength.add(1); // 上升趋势
                    } else if (currentPrice < ma * 0.99) { // 价格低于均线1%
                        trendStrength.add(-1); // 下降趋势
                    } else {
                        trendStrength.add(0); // 横盘
                    }
                }
            }

            return trendStrength;
        }

        /**
         * 计算成交量移动平均线
         */
        public static List<Double> calculateVolumeMA(List<Candlestick> candles, int period) {
            List<Double> volumeMA = new ArrayList<>();
            if (candles.size() < period) {
                for (int i = 0; i < candles.size(); i++) {
                    volumeMA.add(null);
                }
                return volumeMA;
            }

            for (int i = 0; i < candles.size(); i++) {
                if (i < period - 1) {
                    volumeMA.add(null);
                    continue;
                }

                double sum = 0;
                for (int j = i - period + 1; j <= i; j++) {
                    sum += candles.get(j).getVolume();
                }
                volumeMA.add(sum / period);
            }

            return volumeMA;
        }
    }

    /**
     * 生成交易信号
     */
    public List<TradingSignal> generateSignals(String symbol, List<Candlestick> candlesticks) {
        List<TradingSignal> signals = new ArrayList<>();

        // 从配置中获取技术指标参数
        int emaShortPeriod = tradeWiseProperties.getMarketAnalysis().getEmaShortPeriod();
        int emaLongPeriod = tradeWiseProperties.getMarketAnalysis().getEmaLongPeriod();
        int rsiPeriod = tradeWiseProperties.getMarketAnalysis().getRsiPeriod();
        int bbPeriod = tradeWiseProperties.getMarketAnalysis().getBbPeriod();
        double bbStdMultiplier = tradeWiseProperties.getMarketAnalysis().getBbStdMultiplier();

        if (candlesticks == null || candlesticks.size() < Math.max(Math.max(emaLongPeriod, rsiPeriod), bbPeriod)) {
            logger.warn("蜡烛图数据不足，无法生成信号，symbol: {}, 数据点数: {}", symbol, candlesticks != null ? candlesticks.size() : 0);
            return signals;
        }

        // 计算各种技术指标
        List<Double> emaShort = TechnicalIndicators.calculateEMA(candlesticks, emaShortPeriod); // 短期EMA
        List<Double> emaLong = TechnicalIndicators.calculateEMA(candlesticks, emaLongPeriod);  // 长期EMA
        List<Double> rsi = TechnicalIndicators.calculateRSI(candlesticks, rsiPeriod);      // RSI
        Map<String, List<Double>> bollingerBands = TechnicalIndicators.calculateBollingerBands(candlesticks, bbPeriod, bbStdMultiplier);
        Map<String, List<Double>> macdData = TechnicalIndicators.calculateMACD(candlesticks, 12, 26, 9); // MACD
        List<Double> atr = TechnicalIndicators.calculateATR(candlesticks, 14); // ATR用于风险管理
        List<Integer> trendStrength = TechnicalIndicators.calculateTrendStrength(candlesticks, 50); // 市场趋势强度
        List<Double> volumeMA = TechnicalIndicators.calculateVolumeMA(candlesticks, 20); // 成交量移动平均

        // 当前价格
        Candlestick currentCandle = candlesticks.get(candlesticks.size() - 1);
        double currentPrice = currentCandle.getClose();
        double currentAtr = atr.get(atr.size() - 1) != null ? atr.get(atr.size() - 1) : 0;
        double currentVolume = currentCandle.getVolume();
        double currentVolumeMA = volumeMA.get(volumeMA.size() - 1) != null ? volumeMA.get(volumeMA.size() - 1) : 0;
        int currentTrend = trendStrength.get(trendStrength.size() - 1);

        int currentIndex = candlesticks.size() - 1;

        // 多指标确认计数器
        int buyConfirmations = 0;
        int sellConfirmations = 0;

        // 检查EMA金叉/死叉信号
        if (currentIndex >= emaLongPeriod) { // 确保有足够的数据点
            Double currentEmaShort = emaShort.get(currentIndex);
            Double currentEmaLong = emaLong.get(currentIndex);
            Double prevEmaShort = emaShort.get(currentIndex - 1);
            Double prevEmaLong = emaLong.get(currentIndex - 1);

            if (currentEmaShort != null && currentEmaLong != null &&
                    prevEmaShort != null && prevEmaLong != null) {

                // 金叉：短期均线上穿长期均线
                if (prevEmaShort <= prevEmaLong && currentEmaShort > currentEmaLong) {
                    // 只有在趋势向上或横盘时才确认
                    if (currentTrend >= 0) {
                        buyConfirmations++;
                    }
                }
                // 死叉：短期均线下穿长期均线
                else if (prevEmaShort >= prevEmaLong && currentEmaShort < currentEmaLong) {
                    // 只有在趋势向下或横盘时才确认
                    if (currentTrend <= 0) {
                        sellConfirmations++;
                    }
                }
            }
        }

        // 检查RSI超买超卖信号
        if (currentIndex >= rsiPeriod) { // RSI需要足够的数据点
            Double currentRsi = rsi.get(currentIndex);
            if (currentRsi != null) {
                if (currentRsi < 30) {
                    // 只在上升趋势中确认超卖买入信号，避免下跌趋势中的陷阱
                    if (currentTrend >= 0) {
                        buyConfirmations++;
                    }
                } else if (currentRsi > 70) {
                    // 只在下降趋势中确认超买卖出信号
                    if (currentTrend <= 0) {
                        sellConfirmations++;
                    }
                }
            }
        }

        // 检查MACD信号
        if (currentIndex >= 26) { // MACD需要足够的数据点
            List<Double> dif = macdData.get("dif");
            List<Double> dea = macdData.get("dea");
            List<Double> histogram = macdData.get("histogram");

            Double currentDif = dif.get(currentIndex);
            Double currentDea = dea.get(currentIndex);
            Double prevDif = currentIndex > 0 ? dif.get(currentIndex - 1) : null;
            Double prevDea = currentIndex > 0 ? dea.get(currentIndex - 1) : null;

            if (currentDif != null && currentDea != null && prevDif != null && prevDea != null) {
                // MACD金叉
                if (prevDif <= prevDea && currentDif > currentDea) {
                    // 确认柱状图在增加且趋势方向一致
                    Double prevHistogram = currentIndex > 0 ? histogram.get(currentIndex - 1) : null;
                    if (prevHistogram != null && histogram.get(currentIndex) > prevHistogram) {
                        buyConfirmations++;
                    }
                }
                // MACD死叉
                else if (prevDif >= prevDea && currentDif < currentDea) {
                    // 确认柱状图在增加且趋势方向一致
                    Double prevHistogram = currentIndex > 0 ? histogram.get(currentIndex - 1) : null;
                    if (prevHistogram != null && histogram.get(currentIndex) < prevHistogram) {
                        sellConfirmations++;
                    }
                }
            }
        }

        // 检查布林带信号
        if (currentIndex >= bbPeriod) { // 布林带需要足够的数据点
            List<Double> upperBand = bollingerBands.get("upper");
            List<Double> lowerBand = bollingerBands.get("lower");
            List<Double> middleBand = bollingerBands.get("middle");

            Double currentUpper = upperBand.get(currentIndex);
            Double currentLower = lowerBand.get(currentIndex);
            Double currentMiddle = middleBand.get(currentIndex);

            if (currentUpper != null && currentLower != null && currentMiddle != null) {
                // 价格触及下轨（可能的买入信号）- 需要成交量放大确认
                if (currentPrice <= currentLower * 1.005 && currentVolume > currentVolumeMA * 1.2) { // 成交量比均值高20%
                    buyConfirmations++;
                }
                // 价格触及上轨（可能的卖出信号）- 需要成交量放大确认
                else if (currentPrice >= currentUpper * 0.995 && currentVolume > currentVolumeMA * 1.2) { // 成交量比均值高20%
                    sellConfirmations++;
                }
            }
        }

        // 根据确认信号数量生成交易信号
        String confidence = "";
        String suggestion = "";

        // 使用自适应参数系统的确认阈值，动态调整信号严格程度
        int confirmationThreshold = (Integer) adaptiveParameterSystem.getParameter("signal_confirmation_threshold"); // 至少N个指标确认

        if (buyConfirmations >= confirmationThreshold) {
            if (buyConfirmations >= 3) {
                confidence = "高";
            } else if (buyConfirmations == 2) {
                confidence = "中";
            }

            // 基于ATR计算止损和止盈位，使用自适应参数
            double stopLossMultiplier = (Double) adaptiveParameterSystem.getParameter("atr_stop_loss_multiplier");
            double takeProfitMultiplier = (Double) adaptiveParameterSystem.getParameter("atr_take_profit_multiplier");
            double stopLoss = currentPrice - currentAtr * stopLossMultiplier; // 止损位为当前价格减去ATR乘数
            double takeProfit = currentPrice + currentAtr * takeProfitMultiplier; // 止盈位为当前价格加上ATR乘数

            String reason = String.format("多指标确认买入信号(确认数:%d)，当前价格: %.4f，市场趋势: %s",
                    buyConfirmations, currentPrice, currentTrend > 0 ? "上升" : (currentTrend < 0 ? "下降" : "横盘"));
            String sug = String.format("建议开多单，开仓价位: %.4f，止盈价位: %.4f，止损价位: %.4f，仓位管理: 建议轻仓(资金的2%%)操作，当前市场趋势为%s",
                    currentPrice, takeProfit, stopLoss, currentTrend > 0 ? "上升趋势，信号可靠性较高" : (currentTrend < 0 ? "下降趋势，谨慎操作" : "横盘整理，注意风险"));

            signals.add(new TradingSignal(symbol, TradingSignal.SignalType.BUY, "多指标确认买入",
                    currentPrice, reason, sug, confidence));
        }

        if (sellConfirmations >= confirmationThreshold) {
            if (sellConfirmations >= 3) {
                confidence = "高";
            } else if (sellConfirmations == 2) {
                confidence = "中";
            }

            // 基于ATR计算止损和止盈位，使用自适应参数
            double stopLossMultiplier = (Double) adaptiveParameterSystem.getParameter("atr_stop_loss_multiplier");
            double takeProfitMultiplier = (Double) adaptiveParameterSystem.getParameter("atr_take_profit_multiplier");
            double stopLoss = currentPrice + currentAtr * stopLossMultiplier; // 空单止损位为当前价格加上ATR乘数
            double takeProfit = currentPrice - currentAtr * takeProfitMultiplier; // 空单止盈位为当前价格减去ATR乘数

            String reason = String.format("多指标确认卖出信号(确认数:%d)，当前价格: %.4f，市场趋势: %s",
                    sellConfirmations, currentPrice, currentTrend > 0 ? "上升" : (currentTrend < 0 ? "下降" : "横盘"));
            String sug = String.format("建议开空单，开仓价位: %.4f，止盈价位: %.4f，止损价位: %.4f，仓位管理: 建议轻仓(资金的2%%)操作，当前市场趋势为%s",
                    currentPrice, takeProfit, stopLoss, currentTrend < 0 ? "下降趋势，信号可靠性较高" : (currentTrend > 0 ? "上升趋势，谨慎操作" : "横盘整理，注意风险"));

            signals.add(new TradingSignal(symbol, TradingSignal.SignalType.SELL, "多指标确认卖出",
                    currentPrice, reason, sug, confidence));
        }

        return signals;
    }

    /**
     * 基于多时间框架和结构分析生成高级交易信号
     */
    public List<TradingSignal> generateAdvancedSignals(String symbol, List<Candlestick> candlesticks) {
        List<TradingSignal> signals = new ArrayList<>();

        if (candlesticks == null || candlesticks.size() < 50) {
            logger.warn("蜡烛图数据不足，无法生成高级信号，symbol: {}, 数据点数: {}", symbol, candlesticks != null ? candlesticks.size() : 0);
            return signals;
        }

        // 检测当前市场状态
        MarketRegimeController.MarketRegime currentRegime = marketRegimeController.detect(symbol, candlesticks);
        logger.debug("检测到市场状态: {} for {}", currentRegime, symbol);

        // 计算各种技术指标
        List<Double> atr = TechnicalIndicators.calculateATR(candlesticks, 14);
        List<Double> emaShort = TechnicalIndicators.calculateEMA(candlesticks, 12);
        List<Double> emaLong = TechnicalIndicators.calculateEMA(candlesticks, 26);
        List<Double> rsi = TechnicalIndicators.calculateRSI(candlesticks, 14);
        List<Double> volumeMA = TechnicalIndicators.calculateVolumeMA(candlesticks, 20);

        // 获取当前数据
        Candlestick currentCandle = candlesticks.get(candlesticks.size() - 1);
        double currentPrice = currentCandle.getClose();
        double currentVolume = currentCandle.getVolume();
        double currentAtr = atr.get(atr.size() - 1) != null ? atr.get(atr.size() - 1) : 0;
        double currentVolumeMA = volumeMA.get(volumeMA.size() - 1) != null ? volumeMA.get(volumeMA.size() - 1) : 0;

        int currentIndex = candlesticks.size() - 1;

        // 检测结构信号
        detectStructureSignals(signals, symbol, candlesticks, currentPrice, currentAtr, currentIndex);

        // 检测波动率信号
        detectVolatilitySignals(signals, symbol, candlesticks, currentPrice, currentAtr, currentIndex);

        // 检测布林带挤压信号
        detectBollingerSqueezeSignals(signals, symbol, candlesticks, currentPrice, currentIndex);

        // 检测成交量异常信号
        detectVolumeAnomalySignals(signals, symbol, candlesticks, currentPrice, currentVolume, currentVolumeMA, currentIndex);

        // 检测假突破信号
        detectFakeBreakoutSignals(signals, symbol, candlesticks, currentPrice, currentVolume, currentIndex);

        // 检测复合高优先级信号
        detectCompositeHighPrioritySignals(signals, symbol, candlesticks, currentPrice, currentAtr, currentIndex);

        // 检测多时间框架共振信号
        detectMultiTimeframeResonanceSignals(signals, symbol, candlesticks, currentPrice, currentAtr, currentIndex);

        // 使用新的六维智能合约交易系统模型
        generateSixDimensionSignals(signals, symbol, candlesticks);

        // 为所有信号计算评分和风险管理参数
        for (TradingSignal signal : signals) {
            // 检查信号是否可以被处理（状态管理）
            if (!signalStateManager.isSignalProcessable(signal)) {
                logger.debug("信号 {} 处于无效状态，跳过处理", signalStateManager.getSignalId(signal));
                continue;
            }
            
            // 根据市场状态过滤信号模型
            MarketRegimeController.SignalModel signalModel = getSignalModel(signal);
            if (marketRegimeController.isModelAllowed(currentRegime, signalModel)) {
                SignalScoreCalculator.calculateSignalScore(signal, candlesticks, currentPrice, currentAtr);
                
                // 验证信号有效性
                if (signalValidator.validateSignal(signal, candlesticks)) {
                    calculateRiskManagementLevels(signal, candlesticks, currentAtr);
                    
                    // 将信号状态更新为已触发
                    signalStateManager.setSignalStatus(signalStateManager.getSignalId(signal), 
                        SignalStateManager.SignalState.TRIGGERED, "信号已触发并处理");
                } else {
                    logger.debug("信号 {} 未通过有效性验证，跳过处理", signalStateManager.getSignalId(signal));
                    // 将未通过验证的信号标记为失效
                    signalStateManager.invalidateSignal(signal);
                }
            } else {
                logger.debug("市场状态 {} 不允许模型 {}，忽略信号", currentRegime, signalModel);
                // 将不允许的信号标记为失效
                signalStateManager.invalidateSignal(signal);
            }
        }

        return signals;
    }

    /**
     * 生成六维智能合约交易系统信号
     */
    private void generateSixDimensionSignals(List<TradingSignal> signals, String symbol, List<Candlestick> candlesticks) {
        // 获取多时间框架数据
        Map<String, List<Candlestick>> multiTimeframeData = new HashMap<>();
        multiTimeframeData.put("1m", getMarketData(symbol, "1m", 100));   // 最新100根1分钟K线
        multiTimeframeData.put("5m", getMarketData(symbol, "5m", 100));   // 最新100根5分钟K线
        multiTimeframeData.put("15m", getMarketData(symbol, "15m", 100)); // 最新100根15分钟K线
        multiTimeframeData.put("1h", candlesticks);                       // 传入当前的1小时数据
        multiTimeframeData.put("4h", getMarketData(symbol, "4h", 100));   // 最新100根4小时K线
        multiTimeframeData.put("1d", getMarketData(symbol, "1d", 30));    // 最新30根日K线

        // 创建六维模型实例
        TrendMomentumResonanceModel trendMomentumModel = new TrendMomentumResonanceModel();
        InstitutionalFlowModel institutionalFlowModel = new InstitutionalFlowModel();
        VolatilityBreakoutModel volatilityBreakoutModel = new VolatilityBreakoutModel();
        KeyLevelBattlefieldModel keyLevelBattlefieldModel = new KeyLevelBattlefieldModel();
        SentimentExtremesModel sentimentExtremesModel = new SentimentExtremesModel();
        CorrelationArbitrageModel correlationArbitrageModel = new CorrelationArbitrageModel();

        // 检测各模型信号并转换为MarketAnalysisService.TradingSignal
        trendMomentumModel.detect(symbol, multiTimeframeData)
            .map(signalFusionSignal -> convertSignalFusionEngineTradingSignal(signalFusionSignal, symbol))
            .ifPresent(signals::add);
        
        institutionalFlowModel.detect(symbol, multiTimeframeData)
            .map(signalFusionSignal -> convertSignalFusionEngineTradingSignal(signalFusionSignal, symbol))
            .ifPresent(signals::add);
        
        volatilityBreakoutModel.detect(symbol, multiTimeframeData)
            .map(signalFusionSignal -> convertSignalFusionEngineTradingSignal(signalFusionSignal, symbol))
            .ifPresent(signals::add);
        
        keyLevelBattlefieldModel.detect(symbol, multiTimeframeData)
            .map(signalFusionSignal -> convertSignalFusionEngineTradingSignal(signalFusionSignal, symbol))
            .ifPresent(signals::add);
        
        sentimentExtremesModel.detect(symbol, multiTimeframeData)
            .map(signalFusionSignal -> convertSignalFusionEngineTradingSignal(signalFusionSignal, symbol))
            .ifPresent(signals::add);

        // 相关性套利模型需要额外的交易对数据
        // 这里简化处理，使用模拟数据
        Map<String, List<Candlestick>> allSymbolsData = new HashMap<>();
        allSymbolsData.put(symbol, candlesticks);
        correlationArbitrageModel.detect(symbol, multiTimeframeData, allSymbolsData)
            .map(signalFusionSignal -> convertSignalFusionEngineTradingSignal(signalFusionSignal, symbol))
            .ifPresent(signals::add);
    }
    
    /**
     * 将SignalFusionEngine.TradingSignal转换为MarketAnalysisService.TradingSignal
     */
    private TradingSignal convertSignalFusionEngineTradingSignal(SignalFusionEngine.TradingSignal signalFusionSignal, String symbol) {
        // 获取当前价格，这里简化处理，实际应用中应该从市场数据获取
        double currentPrice = 0.0; 
        
        // 转换信号类型
        TradingSignal.SignalType signalType;
        switch (signalFusionSignal.getDirection()) {
            case LONG:
                signalType = TradingSignal.SignalType.BUY;
                break;
            case SHORT:
                signalType = TradingSignal.SignalType.SELL;
                break;
            default:
                signalType = TradingSignal.SignalType.HOLD;
                break;
        }
        
        // 创建新的MarketAnalysisService.TradingSignal
        TradingSignal convertedSignal = new TradingSignal(
            symbol, // 使用正确的symbol
            signalType,
            signalFusionSignal.getModel().toString(),
            currentPrice,
            signalFusionSignal.getReason(),
            "根据六维智能合约交易系统模型生成的信号",
            signalFusionSignal.getConfidence() > 0.8 ? "高" : (signalFusionSignal.getConfidence() > 0.5 ? "中" : "低")
        );
        
        // 设置信号评分
        convertedSignal.setScore((int)(signalFusionSignal.getStrength()));
        
        return convertedSignal;
    }

    /**
     * 检测结构信号 - 趋势结构破坏
     */
    private void detectStructureSignals(List<TradingSignal> signals, String symbol, List<Candlestick> candlesticks,
                                        double currentPrice, double currentAtr, int currentIndex) {
        // 检查最近3个swing low/high是否形成上升/下降趋势
        List<Double> swingLows = new ArrayList<>();
        List<Double> swingHighs = new ArrayList<>();
        List<Integer> swingIndices = new ArrayList<>();

        // 简化的摆动高低点识别（基于连续3根K线的模式）
        for (int i = 2; i < candlesticks.size() - 2; i++) {
            Candlestick current = candlesticks.get(i);
            Candlestick prev1 = candlesticks.get(i - 1);
            Candlestick prev2 = candlesticks.get(i - 2);
            Candlestick next1 = candlesticks.get(i + 1);
            Candlestick next2 = candlesticks.get(i + 2);

            // 寻找局部低点
            if (current.getLow() <= prev1.getLow() && current.getLow() <= prev2.getLow() &&
                    current.getLow() <= next1.getLow() && current.getLow() <= next2.getLow()) {
                swingLows.add(current.getLow());
                swingIndices.add(i);
            }

            // 寻找局部高点
            if (current.getHigh() >= prev1.getHigh() && current.getHigh() >= prev2.getHigh() &&
                    current.getHigh() >= next1.getHigh() && current.getHigh() >= next2.getHigh()) {
                swingHighs.add(current.getHigh());
                swingIndices.add(i);
            }
        }

        // 检查上升结构破坏
        if (swingLows.size() >= 3) {
            // 检查最近3个swing low是否逐步抬高（上升趋势）
            if (swingLows.get(swingLows.size() - 3) < swingLows.get(swingLows.size() - 2) &&
                    swingLows.get(swingLows.size() - 2) < swingLows.get(swingLows.size() - 1)) {
                // 存在上升趋势，检查是否被破坏
                double lastSwingLow = swingLows.get(swingLows.size() - 1);
                if (currentPrice < (lastSwingLow - currentAtr * 0.2)) { // 有效跌破最近swing low
                    String reason = String.format("趋势结构被破坏：价格跌破最近有效支撑位(%.4f)，ATR=%.4f", lastSwingLow, currentAtr);
                    String suggestion = String.format("市场从上升趋势转为非趋势状态，可能转向下跌，建议关注反向机会");
                    signals.add(new TradingSignal(symbol, TradingSignal.SignalType.SELL, "结构信号-上升趋势破坏",
                            currentPrice, reason, suggestion, "高"));
                }
            }
        }

        // 检查下降结构破坏
        if (swingHighs.size() >= 3) {
            // 检查最近3个swing high是否逐步降低（下降趋势）
            if (swingHighs.get(swingHighs.size() - 3) > swingHighs.get(swingHighs.size() - 2) &&
                    swingHighs.get(swingHighs.size() - 2) > swingHighs.get(swingHighs.size() - 1)) {
                // 存在下降趋势，检查是否被破坏
                double lastSwingHigh = swingHighs.get(swingHighs.size() - 1);
                if (currentPrice > (lastSwingHigh + currentAtr * 0.2)) { // 有效突破最近swing high
                    String reason = String.format("趋势结构被破坏：价格突破最近有效阻力位(%.4f)，ATR=%.4f", lastSwingHigh, currentAtr);
                    String suggestion = String.format("市场从下降趋势转为非趋势状态，可能转向上涨，建议关注反向机会");
                    signals.add(new TradingSignal(symbol, TradingSignal.SignalType.BUY, "结构信号-下降趋势破坏",
                            currentPrice, reason, suggestion, "高"));
                }
            }
        }
    }

    /**
     * 检测波动率信号
     */
    private void detectVolatilitySignals(List<TradingSignal> signals, String symbol, List<Candlestick> candlesticks,
                                         double currentPrice, double currentAtr, int currentIndex) {
        // 计算ATR序列，检查是否连续下降
        List<Double> atrList = TechnicalIndicators.calculateATR(candlesticks, 14);

        // 检查ATR是否连续20根下降（波动率压缩）
        int atrDeclineCount = 0;
        for (int i = atrList.size() - 20; i < atrList.size() - 1; i++) {
            if (i >= 1 && atrList.get(i) != null && atrList.get(i - 1) != null &&
                    atrList.get(i) < atrList.get(i - 1)) {
                atrDeclineCount++;
            }
        }

        if (atrDeclineCount >= 15) { // 近20根中有15根ATR下降
            // 检查当前ATR是否处于近期低位
            List<Double> recentAtr = atrList.subList(Math.max(0, atrList.size() - 48), atrList.size());
            double minAtr = recentAtr.stream().filter(Objects::nonNull).min(Double::compareTo).orElse(currentAtr);
            double maxAtr = recentAtr.stream().filter(Objects::nonNull).max(Double::compareTo).orElse(currentAtr);

            if (currentAtr <= minAtr * 1.2 && minAtr > 0) { // 当前ATR接近近期最低值
                String reason = String.format("波动率极度压缩：ATR(%.4f)处于近期最低水平，市场即将选择方向 [评分: 4/10分]", currentAtr);
                
                // 分析当前价格相对于近期高低价位的位置，给出方向建议
                int lookbackPeriod = Math.min(48, candlesticks.size());
                List<Candlestick> recentCandles = candlesticks.subList(Math.max(0, candlesticks.size() - lookbackPeriod), candlesticks.size());
                
                double highestHigh = recentCandles.stream().mapToDouble(Candlestick::getHigh).max().orElse(currentPrice);
                double lowestLow = recentCandles.stream().mapToDouble(Candlestick::getLow).min().orElse(currentPrice);
                
                String directionSuggestion;
                if (currentPrice > (highestHigh + lowestLow) / 2) {
                    // 价格在近期区间的上半部分，可能向下突破
                    directionSuggestion = String.format("当前价格(%.4f)位于近期区间(%.4f-%.4f)上半部分，预计向下突破可能性较大，建议准备做空", 
                            currentPrice, lowestLow, highestHigh);
                } else {
                    // 价格在近期区间的下半部分，可能向上突破
                    directionSuggestion = String.format("当前价格(%.4f)位于近期区间(%.4f-%.4f)下半部分，预计向上突破可能性较大，建议准备做多", 
                            currentPrice, lowestLow, highestHigh);
                }
                
                TradingSignal signal = new TradingSignal(symbol, TradingSignal.SignalType.HOLD, "波动率信号-波动率压缩",
                        currentPrice, reason, directionSuggestion, "中");
                
                // 设置止损和止盈位（对于波动率压缩信号，这是潜在的突破位置）
                if (currentPrice > (highestHigh + lowestLow) / 2) {
                    // 价格偏高，预期向下突破
                    signal.setStopLoss(currentPrice + currentAtr * 0.5); // 止损在上方
                    signal.setTakeProfit(currentPrice - currentAtr * 2.0); // 止盈在下方
                } else {
                    // 价格偏低，预期向上突破
                    signal.setStopLoss(currentPrice - currentAtr * 0.5); // 止损在下方
                    signal.setTakeProfit(currentPrice + currentAtr * 2.0); // 止盈在上方
                }
                
                signals.add(signal);
            }
        }

        // 检测波动率突然放大
        Candlestick currentCandle = candlesticks.get(currentIndex);
        double bodySize = Math.abs(currentCandle.getClose() - currentCandle.getOpen());

        if (bodySize > currentAtr * 1.5) { // K线实体长度超过ATR的1.5倍
            String reason = String.format("波动率突然放大：K线实体(%.4f)远超ATR(%.4f)", bodySize, currentAtr);
            String suggestion = String.format("市场进入主动行情阶段，后续不宜再按震荡逻辑操作");
            signals.add(new TradingSignal(symbol, TradingSignal.SignalType.HOLD, "波动率信号-波动率放大",
                    currentPrice, reason, suggestion, "中"));
        }
    }

    /**
     * 检测布林带挤压信号 (The Squeeze)
     */
    private void detectBollingerSqueezeSignals(List<TradingSignal> signals, String symbol, List<Candlestick> candlesticks,
                                               double currentPrice, int currentIndex) {
        // 计算布林带
        Map<String, List<Double>> bollingerBands = TechnicalIndicators.calculateBollingerBands(candlesticks, 20, 2.0);
        List<Double> atrList = TechnicalIndicators.calculateATR(candlesticks, 14);

        List<Double> upperBand = bollingerBands.get("upper");
        List<Double> lowerBand = bollingerBands.get("lower");
        List<Double> middleBand = bollingerBands.get("middle");

        if (upperBand.get(currentIndex) != null && lowerBand.get(currentIndex) != null &&
                middleBand.get(currentIndex) != null) {

            // 计算布林带宽度
            double bandWidth = (upperBand.get(currentIndex) - lowerBand.get(currentIndex)) / middleBand.get(currentIndex);

            // 获取最近48个周期的布林带宽度最小值
            List<Double> recentBandWidths = new ArrayList<>();
            int startIdx = Math.max(0, currentIndex - 47);
            for (int i = startIdx; i <= currentIndex; i++) {
                Double up = upperBand.get(i);
                Double low = lowerBand.get(i);
                Double mid = middleBand.get(i);
                if (up != null && low != null && mid != null && mid != 0) {
                    recentBandWidths.add((up - low) / mid);
                }
            }

            if (!recentBandWidths.isEmpty()) {
                double minBandWidth = Collections.min(recentBandWidths);
                double currentAtr = atrList.get(currentIndex) != null ? atrList.get(currentIndex) : 0;

                // 检查是否满足布林带挤压条件
                if (bandWidth <= minBandWidth * 1.2 && currentAtr > 0) { // 当前布林带宽度接近近期最小值
                    // 进一步检查K线实体大小
                    Candlestick currentCandle = candlesticks.get(currentIndex);
                    double bodySize = Math.abs(currentCandle.getClose() - currentCandle.getOpen());

                    if (bodySize < currentAtr * 0.5) { // K线实体小于0.5倍ATR
                        String reason = String.format("波动率极度压缩：布林带宽度(%.6f)处于近期最低水平，市场即将选择方向 [评分: 4/10分]", bandWidth);
                        
                        // 分析当前价格相对于近期高低价位的位置，给出方向建议
                        int lookbackPeriod = Math.min(48, candlesticks.size());
                        List<Candlestick> recentCandles = candlesticks.subList(Math.max(0, candlesticks.size() - lookbackPeriod), candlesticks.size());
                        
                        double highestHigh = recentCandles.stream().mapToDouble(Candlestick::getHigh).max().orElse(currentPrice);
                        double lowestLow = recentCandles.stream().mapToDouble(Candlestick::getLow).min().orElse(currentPrice);
                        
                        String directionSuggestion;
                        if (currentPrice > (highestHigh + lowestLow) / 2) {
                            // 价格在近期区间的上半部分，可能向下突破
                            directionSuggestion = String.format("当前价格(%.4f)位于近期区间(%.4f-%.4f)上半部分，预计向下突破可能性较大，建议准备做空", 
                                    currentPrice, lowestLow, highestHigh);
                        } else {
                            // 价格在近期区间的下半部分，可能向上突破
                            directionSuggestion = String.format("当前价格(%.4f)位于近期区间(%.4f-%.4f)下半部分，预计向上突破可能性较大，建议准备做多", 
                                    currentPrice, lowestLow, highestHigh);
                        }
                        
                        TradingSignal signal = new TradingSignal(symbol, TradingSignal.SignalType.HOLD, "波动率信号-布林带挤压",
                                currentPrice, reason, directionSuggestion, "中");
                        
                        // 设置止损和止盈位（对于波动率压缩信号，这是潜在的突破位置）
                        if (currentPrice > (highestHigh + lowestLow) / 2) {
                            // 价格偏高，预期向下突破
                            signal.setStopLoss(currentPrice + currentAtr * 0.5); // 止损在上方
                            signal.setTakeProfit(currentPrice - currentAtr * 2.0); // 止盈在下方
                        } else {
                            // 价格偏低，预期向上突破
                            signal.setStopLoss(currentPrice - currentAtr * 0.5); // 止损在下方
                            signal.setTakeProfit(currentPrice + currentAtr * 2.0); // 止盈在上方
                        }
                        
                        signals.add(signal);
                    }
                }
            }
        }
    }

    /**
     * 检测假突破扫损信号 (Liquidity Grab)
     */
    private void detectFakeBreakoutSignals(List<TradingSignal> signals, String symbol, List<Candlestick> candlesticks,
                                           double currentPrice, double currentVolume, int currentIndex) {
        // 首先确定关键位（使用过去24小时的高/低点）
        int lookbackPeriod = Math.min(24, candlesticks.size());
        List<Candlestick> recentCandles = candlesticks.subList(Math.max(0, candlesticks.size() - lookbackPeriod), candlesticks.size());

        double highestHigh = recentCandles.stream().mapToDouble(Candlestick::getHigh).max().orElse(currentPrice);
        double lowestLow = recentCandles.stream().mapToDouble(Candlestick::getLow).min().orElse(currentPrice);

        // 检查是否发生了突破
        if (currentIndex >= 1) {
            // 检查是否突破了24小时高点
            if (candlesticks.get(currentIndex).getHigh() > highestHigh) {
                // 检查是否在5根K线内收盘价回到关键位以下
                for (int i = 1; i <= Math.min(5, currentIndex); i++) {
                    if (candlesticks.get(currentIndex - i).getClose() < highestHigh) {
                        // 检查突破时的成交量
                        double breakoutVolume = candlesticks.get(currentIndex).getVolume();
                        List<Double> volumeHistory = recentCandles.stream().mapToDouble(Candlestick::getVolume).boxed().collect(Collectors.toList());
                        double avgVolume = volumeHistory.stream().mapToDouble(Double::doubleValue).average().orElse(1.0);

                        if (breakoutVolume > avgVolume * 2) { // 突破时成交量超过均量2倍
                            String reason = String.format("假突破警报：价格突破24小时高点(%.4f)后迅速回落，突破时成交量%.2f", highestHigh, breakoutVolume);
                            String suggestion = String.format("上方存在强抛压，可能是机构诱多行为，谨防价格回调");
                            signals.add(new TradingSignal(symbol, TradingSignal.SignalType.SELL, "假突破信号-高位假突破",
                                    currentPrice, reason, suggestion, "高"));
                            break; // 找到信号后跳出循环
                        }
                    }
                }
            }

            // 检查是否突破了24小时低点
            if (candlesticks.get(currentIndex).getLow() < lowestLow) {
                // 检查是否在5根K线内收盘价回到关键位以上
                for (int i = 1; i <= Math.min(5, currentIndex); i++) {
                    if (candlesticks.get(currentIndex - i).getClose() > lowestLow) {
                        // 检查突破时的成交量
                        double breakoutVolume = candlesticks.get(currentIndex).getVolume();
                        List<Double> volumeHistory = recentCandles.stream().mapToDouble(Candlestick::getVolume).boxed().collect(Collectors.toList());
                        double avgVolume = volumeHistory.stream().mapToDouble(Double::doubleValue).average().orElse(1.0);

                        if (breakoutVolume > avgVolume * 2) { // 突破时成交量超过均量2倍
                            String reason = String.format("假突破警报：价格跌破24小时低点(%.4f)后迅速回升，突破时成交量%.2f", lowestLow, breakoutVolume);
                            String suggestion = String.format("下方存在强支撑，可能是机构诱空行为，谨防价格反弹");
                            signals.add(new TradingSignal(symbol, TradingSignal.SignalType.BUY, "假突破信号-低位假突破",
                                    currentPrice, reason, suggestion, "高"));
                            break; // 找到信号后跳出循环
                        }
                    }
                }
            }
        }
    }

    /**
     * 检测成交量异常信号
     */
    private void detectVolumeAnomalySignals(List<TradingSignal> signals, String symbol, List<Candlestick> candlesticks,
                                            double currentPrice, double currentVolume, double currentVolumeMA, int currentIndex) {
        // 检测异常放量但价格未突破
        if (currentVolume > currentVolumeMA * 3) { // 成交量是均量3倍以上
            // 检查价格是否在关键位置附近未突破
            List<Double> highs = candlesticks.stream().map(Candlestick::getHigh).collect(Collectors.toList());
            List<Double> lows = candlesticks.stream().map(Candlestick::getLow).collect(Collectors.toList());

            double recentHigh = highs.subList(Math.max(0, highs.size() - 24), highs.size()).stream().max(Double::compareTo).orElse(currentPrice);
            double recentLow = lows.subList(Math.max(0, lows.size() - 24), lows.size()).stream().min(Double::compareTo).orElse(currentPrice);

            // 检查价格是否在近期高低点附近但未突破
            boolean nearHigh = Math.abs(currentPrice - recentHigh) < (recentHigh - recentLow) * 0.02; // 距离高点2%以内
            boolean nearLow = Math.abs(currentPrice - recentLow) < (recentHigh - recentLow) * 0.02;  // 距离低点2%以内

            if (nearHigh || nearLow) {
                String direction = nearHigh ? "高位" : "低位";
                String reason = String.format("异常放量但价格未突破：%s出现异常成交量但价格未有效突破关键位", direction);
                String suggestion = String.format("疑似大资金试盘行为，可能出现洗盘或吸筹");
                signals.add(new TradingSignal(symbol, TradingSignal.SignalType.HOLD, "成交量信号-异常放量未突破",
                        currentPrice, reason, suggestion, "中"));
            }
        }
    }

    /**
     * 检测复合高优先级信号
     */
    private void detectCompositeHighPrioritySignals(List<TradingSignal> signals, String symbol, List<Candlestick> candlesticks,
                                                    double currentPrice, double currentAtr, int currentIndex) {
        int score = 0;
        List<String> contributingFactors = new ArrayList<>();

        // 检查结构破坏
        // 这里可以调用detectStructureSignals的逻辑来判断是否有结构信号
        // 简化版本：检查是否存在结构破坏信号
        List<Double> swingLows = new ArrayList<>();
        List<Double> swingHighs = new ArrayList<>();

        for (int i = 2; i < candlesticks.size() - 2; i++) {
            Candlestick current = candlesticks.get(i);
            Candlestick prev1 = candlesticks.get(i - 1);
            Candlestick prev2 = candlesticks.get(i - 2);
            Candlestick next1 = candlesticks.get(i + 1);
            Candlestick next2 = candlesticks.get(i + 2);

            if (current.getLow() <= prev1.getLow() && current.getLow() <= prev2.getLow() &&
                    current.getLow() <= next1.getLow() && current.getLow() <= next2.getLow()) {
                swingLows.add(current.getLow());
            }

            if (current.getHigh() >= prev1.getHigh() && current.getHigh() >= prev2.getHigh() &&
                    current.getHigh() >= next1.getHigh() && current.getHigh() >= next2.getHigh()) {
                swingHighs.add(current.getHigh());
            }
        }

        // 检查是否接近关键位置
        if (swingLows.size() >= 1) {
            double lastSwingLow = swingLows.get(swingLows.size() - 1);
            if (Math.abs(currentPrice - lastSwingLow) < currentAtr * 0.3) {
                score += 2;
                contributingFactors.add("接近关键支撑");
            }
        }

        if (swingHighs.size() >= 1) {
            double lastSwingHigh = swingHighs.get(swingHighs.size() - 1);
            if (Math.abs(currentPrice - lastSwingHigh) < currentAtr * 0.3) {
                score += 2;
                contributingFactors.add("接近关键阻力");
            }
        }

        // 检查RSI极值
        List<Double> rsi = TechnicalIndicators.calculateRSI(candlesticks, 14);
        Double currentRsi = rsi.get(rsi.size() - 1);
        if (currentRsi != null) {
            if (currentRsi < 20 || currentRsi > 80) {
                score += 1;
                contributingFactors.add("RSI极值");
            }
        }

        // 检查ATR下降趋势
        List<Double> atrList = TechnicalIndicators.calculateATR(candlesticks, 14);
        int atrDeclineCount = 0;
        for (int i = atrList.size() - 10; i < atrList.size() - 1; i++) {
            if (i >= 1 && atrList.get(i) != null && atrList.get(i - 1) != null &&
                    atrList.get(i) < atrList.get(i - 1)) {
                atrDeclineCount++;
            }
        }
        if (atrDeclineCount >= 7) {
            score += 1;
            contributingFactors.add("ATR下降");
        }

        // 如果得分达到阈值，生成复合信号
        if (score >= 4) { // 至少4分才认为是高优先级信号
            String reason = String.format("多信号共振：得分为%d，因素包括%s", score, String.join(", ", contributingFactors));
            String suggestion = String.format("当前为高风险/高机会区域，强烈建议人工确认后再操作");
            signals.add(new TradingSignal(symbol, TradingSignal.SignalType.HOLD, "复合信号-多信号共振",
                    currentPrice, reason, suggestion, "高"));
        }
    }



    /**
     * 生成市场行情描述
     */
    public MarketDescription generateMarketDescription(String symbol, List<Candlestick> candlesticks) {
        if (candlesticks == null || candlesticks.isEmpty()) {
            logger.warn("无法生成市场描述，蜡烛图数据为空，symbol: {}", symbol);
            return null;
        }

        // 从配置中获取技术指标参数
        int emaShortPeriod = tradeWiseProperties.getMarketAnalysis().getEmaShortPeriod();
        int emaLongPeriod = tradeWiseProperties.getMarketAnalysis().getEmaLongPeriod();
        int rsiPeriod = tradeWiseProperties.getMarketAnalysis().getRsiPeriod();
        int bbPeriod = tradeWiseProperties.getMarketAnalysis().getBbPeriod();
        double bbStdMultiplier = tradeWiseProperties.getMarketAnalysis().getBbStdMultiplier();

        // 计算各种技术指标
        List<Double> emaShort = TechnicalIndicators.calculateEMA(candlesticks, emaShortPeriod); // 短期EMA
        List<Double> emaLong = TechnicalIndicators.calculateEMA(candlesticks, emaLongPeriod);  // 长期EMA
        List<Double> rsi = TechnicalIndicators.calculateRSI(candlesticks, rsiPeriod);      // RSI
        Map<String, List<Double>> bollingerBands = TechnicalIndicators.calculateBollingerBands(candlesticks, bbPeriod, bbStdMultiplier);
        List<Double> atr = TechnicalIndicators.calculateATR(candlesticks, 14); // ATR用于风险管理
        List<Integer> trendStrength = TechnicalIndicators.calculateTrendStrength(candlesticks, 50); // 市场趋势强度
        List<Double> volumeMA = TechnicalIndicators.calculateVolumeMA(candlesticks, 20); // 成交量移动平均

        // 当前价格和指标值
        Candlestick currentCandle = candlesticks.get(candlesticks.size() - 1);
        double currentPrice = currentCandle.getClose();
        double currentVolume = currentCandle.getVolume();
        double currentVolumeMA = volumeMA.get(volumeMA.size() - 1) != null ? volumeMA.get(volumeMA.size() - 1) : 0;
        int currentTrend = trendStrength.get(trendStrength.size() - 1);
        Double currentRsi = rsi.get(rsi.size() - 1);
        Double currentAtr = atr.get(atr.size() - 1);

        // 获取布林带值
        List<Double> upperBand = bollingerBands.get("upper");
        List<Double> lowerBand = bollingerBands.get("lower");
        List<Double> middleBand = bollingerBands.get("middle");
        Double currentUpper = upperBand.get(upperBand.size() - 1);
        Double currentLower = lowerBand.get(lowerBand.size() - 1);
        Double currentMiddle = middleBand.get(middleBand.size() - 1);

        // 创建市场描述对象
        MarketDescription desc = new MarketDescription(symbol, currentPrice);

        // 设置趋势状态
        if (currentTrend > 0) {
            desc.setTrendStatus("上升趋势");
        } else if (currentTrend < 0) {
            desc.setTrendStatus("下降趋势");
        } else {
            desc.setTrendStatus("横盘整理");
        }

        // 设置波动水平
        if (currentAtr != null) {
            double volatilityPercentage = (currentAtr / currentPrice) * 100;
            if (volatilityPercentage > 3.0) {
                desc.setVolatilityLevel("高波动");
            } else if (volatilityPercentage > 1.5) {
                desc.setVolatilityLevel("中波动");
            } else {
                desc.setVolatilityLevel("低波动");
            }
        } else {
            desc.setVolatilityLevel("未知");
        }

        // 设置成交量状态
        if (currentVolume > currentVolumeMA * 1.5) {
            desc.setVolumeStatus("放量");
        } else if (currentVolume < currentVolumeMA * 0.7) {
            desc.setVolumeStatus("缩量");
        } else {
            desc.setVolumeStatus("正常");
        }

        // 设置RSI状态
        if (currentRsi != null) {
            if (currentRsi < 30) {
                desc.setRsiStatus("超卖(" + String.format("%.2f", currentRsi) + ")");
            } else if (currentRsi > 70) {
                desc.setRsiStatus("超买(" + String.format("%.2f", currentRsi) + ")");
            } else if (currentRsi > 50) {
                desc.setRsiStatus("偏强(" + String.format("%.2f", currentRsi) + ")");
            } else {
                desc.setRsiStatus("偏弱(" + String.format("%.2f", currentRsi) + ")");
            }
        } else {
            desc.setRsiStatus("未知");
        }

        // 设置布林带状态
        if (currentUpper != null && currentLower != null) {
            if (currentPrice > currentUpper * 0.995) {
                desc.setBollingerStatus("上轨附近");
            } else if (currentPrice < currentLower * 1.005) {
                desc.setBollingerStatus("下轨附近");
            } else if (currentPrice > currentMiddle) {
                desc.setBollingerStatus("中轨上方");
            } else {
                desc.setBollingerStatus("中轨下方");
            }
        } else {
            desc.setBollingerStatus("未知");
        }

        // 生成整体市场观点
        StringBuilder overallView = new StringBuilder();
        overallView.append("当前市场状态: ");
        overallView.append(desc.getTrendStatus()).append(", ");
        overallView.append(desc.getVolatilityLevel()).append(", ");
        overallView.append(desc.getVolumeStatus()).append(", ");
        overallView.append(desc.getRsiStatus()).append(", ");
        overallView.append(desc.getBollingerStatus()).append(". ");

        // 基于当前技术指标给出操作建议
        if (currentTrend > 0) {
            overallView.append("市场处于上升趋势，可关注回调机会。");
        } else if (currentTrend < 0) {
            overallView.append("市场处于下降趋势，需注意风险控制。");
        } else {
            overallView.append("市场处于横盘整理，观望为主。");
        }

        if (desc.getRsiStatus().contains("超卖")) {
            overallView.append("RSI显示超卖状态，可能出现反弹机会。");
        } else if (desc.getRsiStatus().contains("超买")) {
            overallView.append("RSI显示超买状态，需警惕回调风险。");
        }

        if (desc.getVolumeStatus().equals("放量")) {
            overallView.append("成交量放大，趋势可信度较高。");
        } else if (desc.getVolumeStatus().equals("缩量")) {
            overallView.append("成交量萎缩，趋势可能不可持续。");
        }

        desc.setOverallMarketView(overallView.toString());

        // 生成市场状态哈希值，用于判断市场状态是否发生变化
        String marketState = symbol + ":" + desc.getTrendStatus() + ":" + desc.getVolatilityLevel() + ":" +
                desc.getVolumeStatus() + ":" + desc.getRsiStatus() + ":" + desc.getBollingerStatus();
        desc.setMarketStateHash(Integer.toString(marketState.hashCode()));

        return desc;
    }

    /**
     * 获取市场数据（从真实的市场数据API获取）
     */
    public List<Candlestick> getMarketData(String symbol, String interval, int limit) {
        logger.info("获取{}的市场数据，周期: {}，数量: {}", symbol, interval, limit);

        try {
            // 使用MarketDataService获取真实的市场数据
            List<Candlestick> marketData = marketDataService.getKlines(symbol, interval, limit);

            if (marketData.isEmpty()) {
                logger.warn("未能获取 {} 的市场数据", symbol);
                return new ArrayList<>();
            }

            logger.info("成功获取 {} 个K线数据点", marketData.size());
            return marketData;

        } catch (Exception e) {
            logger.error("获取市场数据时发生错误，symbol: {}, interval: {}, limit: {}", symbol, interval, limit, e);
            // 返回空列表或处理错误
            return new ArrayList<>();
        }
    }

    /**
     * 分析特定交易对并生成信号
     */
    public void analyzeSymbol(String symbol) {
        logger.info("开始分析交易对: {}", symbol);

        // 检查是否处于经济事件敏感期
        if (!economicCalendarFilter.isSafeToTrade(symbol, LocalDateTime.now())) {
            logger.warn("检测到重大经济事件，暂停 {} 的信号生成", symbol);
            return;
        }

        // 从配置中获取参数
        String interval = tradeWiseProperties.getMarketAnalysis().getInterval();
        int limit = tradeWiseProperties.getMarketAnalysis().getLimit();

        // 获取市场数据
        List<Candlestick> marketData = getMarketData(symbol, interval, limit);

        if (marketData.isEmpty()) {
            logger.warn("未能获取 {} 的市场数据，跳过分析", symbol);
            return;
        }

        // 生成交易信号
        List<TradingSignal> signals = generateSignals(symbol, marketData);

        if (!signals.isEmpty()) {
            logger.info("为交易对 {} 生成了 {} 个交易信号", symbol, signals.size());

            // 发送信号通知（使用专门的模板）
            sendMarketSignalNotification(signals, symbol);
        } else {
            logger.info("交易对 {} 没有生成任何交易信号", symbol);
        }

        // 无论是否有交易信号，都生成市场行情描述
        MarketDescription marketDesc = generateMarketDescription(symbol, marketData);
        if (marketDesc != null) {
            logger.info("为交易对 {} 生成了市场行情描述", symbol);

            // 注意：市场行情描述通知功能已移除，只保留交易信号通知
        }
    }

    /**
     * 仅为特定交易对生成信号（供调度器批量调用）
     */
    public List<TradingSignal> generateSignalsForSymbol(String symbol) {
        return generateSignalsForSymbol(symbol, false);
    }
    
    /**
     * 仅为特定交易对生成信号（供调度器批量调用）
     * 
     * @param symbol 交易对符号
     * @param useEnhancedScoring 是否使用增强评分系统
     */
    public List<TradingSignal> generateSignalsForSymbol(String symbol, boolean useEnhancedScoring) {
        logger.debug("为交易对 {} 生成信号", symbol);

        // 检查是否处于经济事件敏感期
        if (!economicCalendarFilter.isSafeToTrade(symbol, LocalDateTime.now())) {
            logger.warn("检测到重大经济事件，暂停 {} 的信号生成", symbol);
            return new ArrayList<>();
        }

        // 从配置中获取参数
        String interval = tradeWiseProperties.getMarketAnalysis().getInterval();
        int limit = tradeWiseProperties.getMarketAnalysis().getLimit();

        // 获取市场数据
        List<Candlestick> marketData = getMarketData(symbol, interval, limit);

        if (marketData.isEmpty()) {
            logger.warn("未能获取 {} 的市场数据，跳过信号生成", symbol);
            return new ArrayList<>();
        }

        // 生成交易信号
        List<TradingSignal> signals = generateSignals(symbol, marketData);

        // 如果启用增强评分系统，也生成高级信号
        if (useEnhancedScoring) {
            List<TradingSignal> advancedSignals = generateAdvancedSignals(symbol, marketData);
            signals.addAll(advancedSignals);
            
            // 重新计算评分，使用增强评分系统
            for (TradingSignal signal : signals) {
                SignalScoreCalculator.calculateSignalScore(signal, marketData, 
                    marketData.get(marketData.size()-1).getClose(), 
                    getCurrentAtr(marketData));
            }
        }

        // 记录市场分析统计数据
        systemHealthService.recordMarketAnalysisPerformed();
        systemHealthService.recordTradingSignalsGenerated(signals.size());

        return signals;
    }
    
    /**
     * 获取当前ATR值
     */
    private double getCurrentAtr(List<Candlestick> candlesticks) {
        if (candlesticks.isEmpty()) {
            return 0.0;
        }
        
        List<Double> atrList = TechnicalIndicators.calculateATR(candlesticks, 14);
        if (!atrList.isEmpty() && atrList.get(atrList.size()-1) != null) {
            return atrList.get(atrList.size()-1);
        }
        
        return 0.0;
    }


    /**
     * 发送信号通知邮件
     */
    private void sendSignalNotifications(List<TradingSignal> signals) {
        if (signals.isEmpty()) {
            return;
        }

        // 按交易对分组信号
        Map<String, List<TradingSignal>> signalsBySymbol = signals.stream()
                .collect(Collectors.groupingBy(TradingSignal::getSymbol));

        // 为每个交易对发送通知
        for (Map.Entry<String, List<TradingSignal>> entry : signalsBySymbol.entrySet()) {
            String symbol = entry.getKey();
            List<TradingSignal> symbolSignals = entry.getValue();

            // 创建模拟订单用于邮件通知
            List<Order> mockOrders = new ArrayList<>();
            for (TradingSignal signal : symbolSignals) {
                Order order = new Order();
                order.setSymbol(signal.getSymbol());
                order.setSide(signal.getSignalType() == TradingSignal.SignalType.BUY ? "BUY" : "SELL");
                order.setType("MARKET");
                order.setPositionSide(signal.getSignalType() == TradingSignal.SignalType.BUY ? "LONG" : "SHORT");
                order.setExecutedQty(BigDecimal.valueOf(0.001)); // 模拟数量
                order.setAvgPrice(BigDecimal.valueOf(signal.getPrice()));
                order.setOrderTime(Date.from(signal.getTimestamp().atZone(ZoneId.systemDefault()).toInstant()).getTime());
                order.setTotalPnl(BigDecimal.ZERO); // 设置默认盈亏为0

                mockOrders.add(order);
            }

            // 发送邮件通知
            emailService.sendNewOrdersNotification(mockOrders, "市场信号分析系统 - " + symbol);
        }
    }

    /**
     * 发送市场信号通知邮件
     */
    private void sendMarketSignalNotification(List<TradingSignal> signals, String symbol) {
        if (signals.isEmpty()) {
            return;
        }

        try {
            // 获取K线数据用于信号增强
            List<Candlestick> candlesticks = marketDataService.getKlines(symbol, "1h", 200);
            
            // 增强信号，计算完整的交易计划
            List<TradingSignal> enhancedSignals = new ArrayList<>();
            for (TradingSignal signal : signals) {
                TradingSignal enhancedSignal = signalEnhancer.enhance(signal, candlesticks);
                enhancedSignals.add(enhancedSignal);
            }
            
            // 使用邮件过滤策略过滤信号
            List<TradingSignal> filteredSignals = emailFilterStrategy.filterSignals(enhancedSignals);
            
            if (filteredSignals.isEmpty()) {
                logger.debug("经过邮件过滤后，没有需要发送的信号: {}", symbol);
                return;
            }

            if (!tradeWiseProperties.getEmail().isEnabled()) {
                logger.info("邮件发送功能已禁用，跳过发送市场信号邮件通知");
                return;
            }

            // 从数据库获取启用的邮箱地址列表
            List<String> recipientEmails = emailService.getEmailAddressesFromDatabase();

            // 如果没有配置邮箱地址，直接返回
            if (recipientEmails.isEmpty()) {
                logger.warn("没有配置任何邮箱地址，跳过市场信号邮件发送");
                return;
            }

            // 准备邮件模板上下文
            Context context = new Context();
            context.setVariable("symbol", symbol);
            context.setVariable("signalsCount", filteredSignals.size());
            context.setVariable("signals", filteredSignals);

            // 生成HTML内容
            String htmlContent = emailService.getTemplateEngine().process("market-signal-template", context);

            // 设置邮件主题
            // 检查是否有LEVEL_1或LEVEL_2信号来确定邮件重要性
            boolean hasUrgentSignals = filteredSignals.stream()
                .anyMatch(s -> "LEVEL_1".equals(s.getSignalLevel()) || "LEVEL_2".equals(s.getSignalLevel()));
            
            String subject;
            if (hasUrgentSignals) {
                subject = String.format("[重要] %s 出现 %d 个交易信号", symbol, filteredSignals.size());
            } else {
                subject = String.format("[观察] %s 出现 %d 个交易信号", symbol, filteredSignals.size());
            }

            // 使用JavaMailSender发送邮件
            MimeMessage mimeMessage = emailService.getMailSender().createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(emailService.getSenderEmail());
            helper.setTo(recipientEmails.toArray(new String[0]));
            helper.setSubject(subject);
            helper.setText(htmlContent, true); // true表示内容是HTML

            emailService.getMailSender().send(mimeMessage);

            logger.info("成功发送市场信号邮件通知，交易对: {}，信号数量: {}，收件人: {}",
                    symbol, filteredSignals.size(), recipientEmails.size());
        } catch (Exception e) {
            logger.error("发送市场信号邮件通知失败", e);
        }
    }

    /**
     * 发送合并的市场信号通知邮件
     */
    public void sendCombinedMarketSignalNotification(List<TradingSignal> allSignals) {
        if (!tradeWiseProperties.getEmail().isEnabled()) {
            logger.info("邮件发送功能已禁用，跳过发送合并市场信号邮件通知");
            return;
        }
        
        if (allSignals.isEmpty()) {
            return;
        }

        try {
            // 增强所有信号，计算完整的交易计划
            List<TradingSignal> enhancedSignals = new ArrayList<>();
            for (TradingSignal signal : allSignals) {
                List<Candlestick> candlesticks = marketDataService.getKlines(signal.getSymbol(), "1h", 200);
                TradingSignal enhancedSignal = signalEnhancer.enhance(signal, candlesticks);
                enhancedSignals.add(enhancedSignal);
            }
            
            // 使用邮件过滤策略过滤信号
            List<TradingSignal> filteredSignals = emailFilterStrategy.filterSignals(enhancedSignals);
            
            if (filteredSignals.isEmpty()) {
                logger.debug("经过邮件过滤后，没有需要发送的合并信号");
                return;
            }

            // 按交易对分组信号
            Map<String, List<TradingSignal>> signalsBySymbol = filteredSignals.stream()
                    .collect(Collectors.groupingBy(TradingSignal::getSymbol));

            // 从数据库获取启用的邮箱地址列表
            List<String> recipientEmails = emailService.getEmailAddressesFromDatabase();

            // 如果没有配置邮箱地址，直接返回
            if (recipientEmails.isEmpty()) {
                logger.warn("没有配置任何邮箱地址，跳过合并市场信号邮件发送");
                return;
            }

            // 准备邮件模板上下文
            Context context = new Context();
            context.setVariable("signalsBySymbol", signalsBySymbol);
            context.setVariable("totalSignalsCount", filteredSignals.size());
            context.setVariable("totalSymbolsCount", signalsBySymbol.size());
            context.setVariable("signals", filteredSignals);

            // 生成HTML内容
            String htmlContent = emailService.getTemplateEngine().process("combined-market-signal-template", context);

            // 设置邮件主题
            // 检查是否有LEVEL_1或LEVEL_2信号来确定邮件重要性
            boolean hasUrgentSignals = filteredSignals.stream()
                .anyMatch(s -> "LEVEL_1".equals(s.getSignalLevel()) || "LEVEL_2".equals(s.getSignalLevel()));
            
            String subject;
            if (hasUrgentSignals) {
                subject = String.format("[重要汇总] 共 %d 个交易对出现 %d 个交易信号",
                        signalsBySymbol.size(), filteredSignals.size());
            } else {
                subject = String.format("[日常观察] 共 %d 个交易对出现 %d 个交易信号",
                        signalsBySymbol.size(), filteredSignals.size());
            }

            // 使用JavaMailSender发送邮件
            MimeMessage mimeMessage = emailService.getMailSender().createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(emailService.getSenderEmail());
            helper.setTo(recipientEmails.toArray(new String[0]));
            helper.setSubject(subject);
            helper.setText(htmlContent, true); // true表示内容是HTML

            emailService.getMailSender().send(mimeMessage);

            logger.info("成功发送合并市场信号邮件通知，交易对数量: {}，总信号数量: {}，收件人: {}",
                    signalsBySymbol.size(), filteredSignals.size(), recipientEmails.size());
        } catch (Exception e) {
            logger.error("发送合并市场信号邮件通知失败", e);
        }
    }


    /**
     * 信号聚合器 - 评分引擎
     * 将多个信号进行聚合和过滤，减少过多的邮件通知
     */
    public static class SignalAggregator {
        private Map<String, LocalDateTime> lastSignalTime = new ConcurrentHashMap<>();
        private static final long COOLDOWN_MINUTES_LEVEL_1 = 120; // LEVEL_1信号冷却时间（2小时）
        private static final long COOLDOWN_MINUTES_LEVEL_2 = 60; // LEVEL_2信号冷却时间（1小时）
        private static final long COOLDOWN_MINUTES_LEVEL_3 = 240; // LEVEL_3信号冷却时间（4小时）
        // 特殊处理HOLD信号，尤其是波动率信号，防止频繁发送
        private static final long COOLDOWN_MINUTES_HOLD_VOLATILITY = 480; // HOLD波动率信号冷却时间（8小时）
        
        // 信号评级阈值
        private int level1Threshold = 8;
        private int level2Threshold = 6;
        private int level3Threshold = 4;
        
        // 构造函数，接受配置的阈值
        public SignalAggregator(int level1Threshold, int level2Threshold, int level3Threshold) {
            this.level1Threshold = level1Threshold;
            this.level2Threshold = level2Threshold;
            this.level3Threshold = level3Threshold;
        }
        
        // 默认构造函数，使用默认阈值
        public SignalAggregator() {
            // 使用默认值
        }

        /**
         * 聚合和过滤信号
         */
        public List<TradingSignal> aggregateSignals(List<TradingSignal> rawSignals) {
            List<TradingSignal> filteredSignals = new ArrayList<>();

            for (TradingSignal signal : rawSignals) {
                // 应用冷却机制
                if (shouldSendSignal(signal)) {
                    // 根据信号的评分决定是否发送
                    int score = signal.getScore();

                    // 根据配置的阈值决定信号级别
                    if (score >= level1Threshold) { // LEVEL_1 高优先级信号
                        signal.setConfidence("极高");
                        signal.setSignalLevel("LEVEL_1");
                        signal.setReason(signal.getReason() + " [评分: " + score + "/10分]");
                        filteredSignals.add(signal);
                        // 更新最后发送时间
                        lastSignalTime.put(getSignalKey(signal), LocalDateTime.now());
                    } else if (score >= level2Threshold) { // LEVEL_2 中优先级信号
                        signal.setConfidence("高");
                        signal.setSignalLevel("LEVEL_2");
                        signal.setReason(signal.getReason() + " [评分: " + score + "/10分]");
                        filteredSignals.add(signal);
                        // 更新最后发送时间
                        lastSignalTime.put(getSignalKey(signal), LocalDateTime.now());
                    } else if (score >= level3Threshold) { // LEVEL_3 低优先级信号
                        signal.setConfidence("中");
                        signal.setSignalLevel("LEVEL_3");
                        signal.setReason(signal.getReason() + " [评分: " + score + "/10分]");
                        // 对于LEVEL_3信号，需要更严格的冷却机制
                        if (shouldSendLowPrioritySignal(signal)) {
                            filteredSignals.add(signal);
                            lastSignalTime.put(getSignalKey(signal), LocalDateTime.now());
                        }
                    }
                }
            }

            return filteredSignals;
        }

        /**
         * 判断是否应该发送信号（冷却机制）
         */
        private boolean shouldSendSignal(TradingSignal signal) {
            String key = getSignalKey(signal);
            LocalDateTime lastTime = lastSignalTime.get(key);

            if (lastTime == null) {
                return true; // 首次信号总是发送
            }

            // 根据信号等级和类型设置不同的冷却时间
            long cooldownMinutes;
            String signalLevel = signal.getSignalLevel();
            
            // 检查是否是HOLD类型的波动率信号
            boolean isHoldVolatilitySignal = signal.getSignalType() == TradingSignal.SignalType.HOLD && 
                                           (signal.getIndicator().contains("波动率") || signal.getIndicator().contains("挤压"));
            
            if (isHoldVolatilitySignal) {
                cooldownMinutes = COOLDOWN_MINUTES_HOLD_VOLATILITY; // 为HOLD波动率信号设置更长的冷却时间
            } else if ("LEVEL_1".equals(signalLevel)) {
                cooldownMinutes = COOLDOWN_MINUTES_LEVEL_1;
            } else if ("LEVEL_2".equals(signalLevel)) {
                cooldownMinutes = COOLDOWN_MINUTES_LEVEL_2;
            } else { // LEVEL_3
                cooldownMinutes = COOLDOWN_MINUTES_LEVEL_3;
            }

            Duration duration = Duration.between(lastTime, LocalDateTime.now());
            return duration.toMinutes() >= cooldownMinutes;
        }

        /**
         * 判断是否发送低优先级信号
         */
        private boolean shouldSendLowPrioritySignal(TradingSignal signal) {
            // 如果超过2小时没有收到任何信号，则发送低优先级信号
            String symbol = signal.getSymbol();
            boolean hasRecentSignal = false;

            for (Map.Entry<String, LocalDateTime> entry : lastSignalTime.entrySet()) {
                if (entry.getKey().startsWith(symbol + "_")) {
                    Duration duration = Duration.between(entry.getValue(), LocalDateTime.now());
                    if (duration.toHours() < 2) {
                        hasRecentSignal = true;
                        break;
                    }
                }
            }

            return !hasRecentSignal;
        }

        /**
         * 生成信号唯一键
         */
        private String getSignalKey(TradingSignal signal) {
            return signal.getSymbol() + "_" + signal.getIndicator();
        }
    }
    
    /**
     * 信号评分计算器 - 实现DeepSeek的多维度评分系统
     * 针对同源指标（Multicollinearity）问题进行了优化
     * 评分、风险和仓位管理已解耦
     */
    public static class SignalScoreCalculator {
        
        // 不同类别指标的权重上限，防止同源指标叠加
        private static final Map<String, Integer> CATEGORY_SCORE_LIMITS;
        
        static {
            Map<String, Integer> map = new HashMap<>();
            map.put("momentum", 2);    // 动量类指标（MACD/RSI）最高2分
            map.put("structure", 4);   // 结构类指标（BOS/假突破）最高4分
            map.put("volume", 2);      // 成交量类指标最高2分
            map.put("volatility", 1);  // 波动率类指标最高1分
            map.put("sentiment", 1);   // 情绪类指标最高1分
            CATEGORY_SCORE_LIMITS = Collections.unmodifiableMap(map);
        }
        
        /**
         * 计算交易信号评分 (0-10分) - 仅用于评估信号质量
         */
        public static int calculateSignalScore(TradingSignal signal, List<Candlestick> candlesticks, double currentPrice, double currentAtr) {
            int totalScore = 0;
            Map<String, Integer> scoreDetails = new HashMap<>();
            
            // 按类别分别计算得分，避免同源指标叠加
            int momentumScore = calculateMomentumScore(signal, candlesticks);
            int structureScore = calculateStructureScore(signal, candlesticks, currentPrice, currentAtr);
            int volumeScore = calculateVolumeScore(signal, candlesticks);
            int volatilityScore = calculateVolatilityScore(signal, candlesticks);
            int sentimentScore = calculateSentimentScore(signal, candlesticks);
            
            // 应用类别权重上限
            momentumScore = Math.min(momentumScore, CATEGORY_SCORE_LIMITS.get("momentum"));
            structureScore = Math.min(structureScore, CATEGORY_SCORE_LIMITS.get("structure"));
            volumeScore = Math.min(volumeScore, CATEGORY_SCORE_LIMITS.get("volume"));
            volatilityScore = Math.min(volatilityScore, CATEGORY_SCORE_LIMITS.get("volatility"));
            sentimentScore = Math.min(sentimentScore, CATEGORY_SCORE_LIMITS.get("sentiment"));
            
            // 累加各类别得分
            totalScore += momentumScore;
            totalScore += structureScore;
            totalScore += volumeScore;
            totalScore += volatilityScore;
            totalScore += sentimentScore;
            
            // 检查是否为与大周期趋势一致的信号（大周期定性，小周期入场）
            if (isConsistentWithHigherTF(signal, candlesticks)) {
                // 如果与大周期趋势一致，增加奖励分
                int consistencyBonus = 1;
                totalScore = Math.min(10, totalScore + consistencyBonus);
                scoreDetails.put("趋势一致性奖励", consistencyBonus);
            }
            
            // 限制最高分为10
            totalScore = Math.min(totalScore, 10);
            
            signal.setScore(totalScore);
            
            // 更新得分详情
            if (momentumScore > 0) scoreDetails.put("动量分", momentumScore);
            if (structureScore > 0) scoreDetails.put("结构分", structureScore);
            if (volumeScore > 0) scoreDetails.put("能量分", volumeScore);
            if (volatilityScore > 0) scoreDetails.put("波动分", volatilityScore);
            if (sentimentScore > 0) scoreDetails.put("情绪分", sentimentScore);
            
            signal.setScoreDetails(scoreDetails);
            
            return totalScore;
        }
        
        /**
         * 根据信号评分和市场状态计算置信度
         * 解耦评分和风险暴露
         */
        public static double calculateConfidence(TradingSignal signal, MarketRegimeController.MarketRegime regime, List<Candlestick> candlesticks) {
            int score = signal.getScore();
            double baseConfidence = score / 10.0; // 基础置信度为评分/10
            
            // 根据市场状态调整置信度
            double regimeAdjustment = getRegimeConfidenceAdjustment(regime);
            
            // 根据市场波动性调整置信度
            double volatilityAdjustment = getVolatilityConfidenceAdjustment(candlesticks);
            
            double adjustedConfidence = baseConfidence * regimeAdjustment * volatilityAdjustment;
            
            // 限制置信度在合理范围内
            return Math.max(0.0, Math.min(1.0, adjustedConfidence));
        }
        
        /**
         * 根据市场状态调整置信度
         */
        private static double getRegimeConfidenceAdjustment(MarketRegimeController.MarketRegime regime) {
            switch (regime) {
                case STRONG_TREND:
                    return 1.2; // 强趋势状态下，置信度提高
                case WEAK_TREND:
                    return 1.0; // 弱趋势状态下，置信度不变
                case RANGE:
                    return 0.8; // 震荡状态下，置信度降低
                case SQUEEZE:
                    return 0.7; // 压缩状态下，谨慎对待
                case VOLATILITY_EXPANSION:
                    return 0.9; // 波动扩张，适度降低置信度
                default:
                    return 1.0; // 默认情况
            }
        }
        
        /**
         * 根据市场波动性调整置信度
         */
        private static double getVolatilityConfidenceAdjustment(List<Candlestick> candlesticks) {
            if (candlesticks.size() < 20) return 1.0;
            
            // 计算ATR的变化率来判断波动性
            List<Double> atrList = TechnicalIndicators.calculateATR(candlesticks, 14);
            if (atrList.size() < 2) return 1.0;
            
            Double currentAtr = atrList.get(atrList.size() - 1);
            Double prevAtr = atrList.get(atrList.size() - 2);
            
            if (currentAtr == null || prevAtr == null) return 1.0;
            
            if (prevAtr == 0) return 1.0;
            
            double atrChangeRate = (currentAtr - prevAtr) / prevAtr;
            
            // 如果波动性急剧增加，降低置信度
            if (atrChangeRate > 0.5) {
                return 0.7; // 波动性急剧增加，降低置信度
            } else if (atrChangeRate < -0.3) {
                return 0.9; // 波动性下降，小幅提高置信度
            }
            
            return 1.0; // 正常波动性
        }
        
        /**
         * 根据置信度和风险预算计算仓位大小
         * 实现评分、风险和仓位的彻底解耦
         */
        public static double calculatePositionSize(double accountEquity, double confidence, 
                                                MarketRegimeController.MarketRegime regime, 
                                                TradingSignal signal, List<Candlestick> candlesticks) {
            // 基础风险预算
            double baseRiskPercentage = 0.02; // 每笔交易风险2%的账户资金
            
            // 根据市场状态调整风险预算
            double regimeRiskFactor = getRegimeRiskFactor(regime);
            
            // 计算基础风险金额
            double riskBudget = accountEquity * baseRiskPercentage * regimeRiskFactor;
            
            // 应用置信度调整
            double confidenceAdjustedRisk = riskBudget * confidence;
            
            // 根据信号类型和止损距离计算仓位
            double stopLossDistance;
            if (signal.getSignalType() == TradingSignal.SignalType.BUY) {
                stopLossDistance = Math.abs(signal.getPrice() - signal.getStopLoss());
            } else {
                stopLossDistance = Math.abs(signal.getPrice() - signal.getStopLoss());
            }
            
            if (stopLossDistance <= 0) {
                stopLossDistance = signal.getPrice() * 0.02; // 默认2%止损
            }
            
            // 计算仓位大小
            double positionValue = confidenceAdjustedRisk / stopLossDistance;
            
            // 应用最大仓位限制（不超过账户的5%）
            double maxPosition = accountEquity * 0.05;
            
            return Math.min(positionValue, maxPosition);
        }
        
        /**
         * 根据市场状态调整风险因子
         */
        private static double getRegimeRiskFactor(MarketRegimeController.MarketRegime regime) {
            switch (regime) {
                case STRONG_TREND:
                    return 1.2; // 强趋势状态下，可以适度加大仓位
                case WEAK_TREND:
                    return 1.0; // 弱趋势状态下，正常风险
                case RANGE:
                    return 0.7; // 震荡状态下，降低风险
                case SQUEEZE:
                    return 0.5; // 压缩状态下，极度谨慎
                case VOLATILITY_EXPANSION:
                    return 0.8; // 波动扩张，适度降低风险
                default:
                    return 1.0; // 默认情况
            }
        }
        
        /**
         * 计算动量分 - 包括RSI、MACD等动量指标
         */
        private static int calculateMomentumScore(TradingSignal signal, List<Candlestick> candlesticks) {
            int score = 0;
            
            // 检查是否包含动量指标信号
            if (signal.getIndicator().toLowerCase().contains("rsi") || 
                signal.getIndicator().toLowerCase().contains("macd") ||
                signal.getReason().toLowerCase().contains("rsi") ||
                signal.getReason().toLowerCase().contains("macd")) {
                // RSI极值或MACD金叉/死叉
                score += 1;
                
                // 如果是极值情况（RSI<30或>70），增加分数
                if (signal.getReason().toLowerCase().contains("极值") || 
                    signal.getReason().toLowerCase().contains("超买") || 
                    signal.getReason().toLowerCase().contains("超卖")) {
                    score += 1;
                }
            }
            
            return score;
        }
        
        /**
         * 计算结构分 - 包括BOS、假突破等结构信号
         */
        private static int calculateStructureScore(TradingSignal signal, List<Candlestick> candlesticks, double currentPrice, double currentAtr) {
            int score = 0;
            
            // 检查是否包含结构信号
            if (signal.getIndicator().contains("结构信号") || 
                signal.getIndicator().contains("BOS") ||
                signal.getIndicator().contains("假突破") ||
                signal.getIndicator().contains("关键位置")) {
                score += 3;
                
                // 如果是明确的结构突破，增加分数
                if (signal.getReason().toLowerCase().contains("突破") || 
                    signal.getReason().toLowerCase().contains("破坏")) {
                    score += 1;
                }
            }
            
            return score;
        }
        
        /**
         * 计算能量分 - 基于成交量确认
         */
        private static int calculateVolumeScore(TradingSignal signal, List<Candlestick> candlesticks) {
            int score = 0;
            
            // 检查成交量确认
            if (signal.getReason().toLowerCase().contains("成交量") || 
                signal.getReason().toLowerCase().contains("放量") ||
                signal.getReason().toLowerCase().contains("量能")) {
                score += 1;
                
                if (signal.getReason().toLowerCase().contains("爆量") || 
                    signal.getReason().toLowerCase().contains("巨量")) {
                    score += 1;
                }
            }
            
            return score;
        }
        
        /**
         * 计算波动分 - 基于波动率信号
         */
        private static int calculateVolatilityScore(TradingSignal signal, List<Candlestick> candlesticks) {
            int score = 0;
            
            // 检查波动率信号
            if (signal.getIndicator().contains("波动率") || 
                signal.getIndicator().contains("挤压") ||
                signal.getReason().toLowerCase().contains("波动")) {
                score += 1;
            }
            
            return score;
        }
        
        /**
         * 计算情绪分 - 基于市场情绪信号
         */
        private static int calculateSentimentScore(TradingSignal signal, List<Candlestick> candlesticks) {
            int score = 0;
            
            // 检查情绪指标
            if (signal.getIndicator().contains("情绪") || 
                signal.getReason().toLowerCase().contains("恐慌") ||
                signal.getReason().toLowerCase().contains("贪婪")) {
                score += 1;
            }
            
            return score;
        }
        
        /**
         * 检查信号是否与更高时间框架趋势一致
         * 实现"大周期定性，小周期入场"的逻辑
         */
        private static boolean isConsistentWithHigherTF(TradingSignal signal, List<Candlestick> candlesticks) {
            if (candlesticks.size() < 50) return true; // 数据不足时默认一致
            
            // 计算长期趋势（使用50周期均线判断）
            List<Double> longTermMA = TechnicalIndicators.calculateSMA(candlesticks, 50);
            Double latestLongTermMA = longTermMA.get(longTermMA.size() - 1);
            if (latestLongTermMA == null) return true; // 计算失败时默认一致
            
            double currentPrice = candlesticks.get(candlesticks.size() - 1).getClose();
            boolean isLongTermBullish = currentPrice > latestLongTermMA;
            
            // 检查信号方向是否与长期趋势一致
            boolean isSignalBullish = signal.getSignalType() == TradingSignal.SignalType.BUY;
            
            return isLongTermBullish == isSignalBullish;
        }
        
        /**
         * 检查价格是否在关键位附近
         */
        private static boolean isNearKeyLevels(List<Candlestick> candlesticks, double currentPrice, double currentAtr) {
            if (candlesticks.size() < 24) return false;
            
            // 获取近期高点和低点
            List<Double> highs = candlesticks.stream().map(Candlestick::getHigh).collect(Collectors.toList());
            List<Double> lows = candlesticks.stream().map(Candlestick::getLow).collect(Collectors.toList());
            
            double recentHigh = highs.subList(Math.max(0, highs.size() - 24), highs.size()).stream()
                    .max(Double::compareTo).orElse(currentPrice);
            double recentLow = lows.subList(Math.max(0, lows.size() - 24), lows.size()).stream()
                    .min(Double::compareTo).orElse(currentPrice);
            
            // 检查是否在ATR范围内
            return Math.abs(currentPrice - recentHigh) <= currentAtr || 
                   Math.abs(currentPrice - recentLow) <= currentAtr;
        }
        
        /**
         * 检查是否处于趋势市
         */
        private static boolean isInTrendingMarket(List<Candlestick> candlesticks) {
            if (candlesticks.size() < 14) return false;
            
            // 简单的趋势判断：价格偏离移动平均线的程度
            List<Double> closes = candlesticks.stream().map(Candlestick::getClose).collect(Collectors.toList());
            List<Double> sma20 = TechnicalIndicators.calculateSMA(candlesticks, 20);
            
            Double latestSma20 = sma20.get(sma20.size() - 1);
            if (latestSma20 == null) return false;
            
            double latestClose = closes.get(closes.size() - 1);
            double deviation = Math.abs(latestClose - latestSma20) / latestSma20;
            
            // 如果价格偏离均线超过2%，认为是在趋势市
            return deviation > 0.02;
        }
        
        /**
         * 检查信号是否与趋势一致
         */
        private static boolean isWithTrend(TradingSignal signal, List<Candlestick> candlesticks) {
            if (candlesticks.size() < 50) return false;
            
            // 计算50周期SMA判断趋势方向
            List<Double> sma50 = TechnicalIndicators.calculateSMA(candlesticks, 50);
            Double latestSma50 = sma50.get(sma50.size() - 1);
            if (latestSma50 == null) return false;
            
            double latestClose = candlesticks.get(candlesticks.size() - 1).getClose();
            
            // 如果是看涨信号且价格在均线之上，或者看跌信号且价格在均线之下，则为顺势
            if (signal.getSignalType() == TradingSignal.SignalType.BUY) {
                return latestClose > latestSma50;
            } else if (signal.getSignalType() == TradingSignal.SignalType.SELL) {
                return latestClose < latestSma50;
            }
            
            return false;
        }
        
        /**
         * 检查价格与持仓量是否同向变动
         */
        private static boolean hasPositiveCorrelationWithOI(List<Candlestick> candlesticks, double currentPrice) {
            // 这里简化实现，实际应用中需要接入真实的持仓量数据
            // 为了演示目的，我们简单地检查价格趋势
            if (candlesticks.size() < 10) {
                return false;
            }

            // 获取最近的价格趋势
            double prevPrice = candlesticks.get(candlesticks.size() - 10).getClose();
            return (currentPrice - prevPrice) > 0; // 价格上涨视为正向关系
        }
        
        /**
         * 检查带单员方向是否一致
         */
        private static boolean hasConsistentTraderPositions(String symbol) {
            // 这里简化实现，实际应用中需要获取真实的带单员数据
            // 为了演示目的，返回false，表示暂不使用该评分项
            return false;
        }
    }
    
    /**
     * 检测多时间框架共振信号
     * 基于DeepSeek的多时间框架共振思想：H4信号权重40%，H1信号权重35%，M15信号权重25%
     */
    private void detectMultiTimeframeResonanceSignals(List<TradingSignal> signals, String symbol, List<Candlestick> candlesticks,
                                                     double currentPrice, double currentAtr, int currentIndex) {
        // 简化实现：在同一时间框架内检测多重确认信号
        // 在实际应用中，这里会连接到不同时间框架的数据
        
        int resonanceCount = 0;
        List<String> contributingIndicators = new ArrayList<>();
        
        // 检查是否存在多个技术指标同时发出相同方向的信号
        List<Double> emaShort = TechnicalIndicators.calculateEMA(candlesticks, 12);
        List<Double> emaLong = TechnicalIndicators.calculateEMA(candlesticks, 26);
        List<Double> rsi = TechnicalIndicators.calculateRSI(candlesticks, 14);
        Map<String, List<Double>> bollingerBands = TechnicalIndicators.calculateBollingerBands(candlesticks, 20, 2.0);
        
        // 检查EMA金叉/死叉
        if (currentIndex >= 26) {
            Double currentEmaShort = emaShort.get(currentIndex);
            Double currentEmaLong = emaLong.get(currentIndex);
            Double prevEmaShort = emaShort.get(currentIndex - 1);
            Double prevEmaLong = emaLong.get(currentIndex - 1);
            
            if (currentEmaShort != null && currentEmaLong != null && 
                prevEmaShort != null && prevEmaLong != null) {
                
                // 检查金叉（看涨共振）
                if (prevEmaShort <= prevEmaLong && currentEmaShort > currentEmaLong) {
                    resonanceCount++;
                    contributingIndicators.add("EMA金叉");
                }
                // 检查死叉（看跌共振）
                else if (prevEmaShort >= prevEmaLong && currentEmaShort < currentEmaLong) {
                    resonanceCount++;
                    contributingIndicators.add("EMA死叉");
                }
            }
        }
        
        // 检查RSI极值
        if (currentIndex >= 14) {
            Double currentRsi = rsi.get(currentIndex);
            if (currentRsi != null) {
                if (currentRsi < 30) { // 超卖
                    resonanceCount++;
                    contributingIndicators.add("RSI超卖");
                } else if (currentRsi > 70) { // 超买
                    resonanceCount++;
                    contributingIndicators.add("RSI超买");
                }
            }
        }
        
        // 检查布林带位置
        List<Double> upperBand = bollingerBands.get("upper");
        List<Double> lowerBand = bollingerBands.get("lower");
        List<Double> middleBand = bollingerBands.get("middle");
        
        if (upperBand != null && lowerBand != null && middleBand != null &&
            currentIndex < upperBand.size() && currentIndex < lowerBand.size() && currentIndex < middleBand.size()) {
            
            Double currentUpper = upperBand.get(currentIndex);
            Double currentLower = lowerBand.get(currentIndex);
            Double currentMiddle = middleBand.get(currentIndex);
            
            if (currentUpper != null && currentLower != null && currentMiddle != null) {
                // 检查价格在布林带的位置
                if (currentPrice <= currentLower * 1.005) { // 接近下轨
                    resonanceCount++;
                    contributingIndicators.add("布林带下轨");
                } else if (currentPrice >= currentUpper * 0.995) { // 接近上轨
                    resonanceCount++;
                    contributingIndicators.add("布林带上轨");
                }
            }
        }
        
        // 如果有3个或以上的指标共振，则生成共振信号
        if (resonanceCount >= 3) {
            String direction = isBullishResonance(contributingIndicators) ? "多头" : "空头";
            String signalType = isBullishResonance(contributingIndicators) ? "BUY" : "SELL";
            String reason = String.format("多时间框架共振检测：共%d个指标确认，包含%s", 
                                        resonanceCount, String.join(", ", contributingIndicators));
            String suggestion = String.format("多指标共振确认%s趋势，可靠性较高，可考虑加仓或开新仓", direction);
            
            TradingSignal signal = new TradingSignal(symbol, 
                signalType.equals("BUY") ? TradingSignal.SignalType.BUY : TradingSignal.SignalType.SELL, 
                "共振信号-多时间框架共振", 
                currentPrice, 
                reason, 
                suggestion, 
                "高");
            
            signals.add(signal);
        }
    }
    
    /**
     * 判断是否为看涨共振
     */
    private boolean isBullishResonance(List<String> indicators) {
        int bullishCount = 0;
        int bearishCount = 0;
        
        for (String indicator : indicators) {
            switch (indicator) {
                case "EMA金叉":
                case "RSI超卖":
                case "布林带下轨":
                    bullishCount++;
                    break;
                case "EMA死叉":
                case "RSI超买":
                case "布林带上轨":
                    bearishCount++;
                    break;
            }
        }
        
        return bullishCount > bearishCount;
    }
    
    /**
     * 计算合约交易相关参数
     */
    public void calculateContractParameters(TradingSignal signal, double entryPrice, int leverage, double positionSize, double accountBalance) {
        // 计算所需保证金
        double marginRequired = (entryPrice * positionSize) / leverage;
        signal.setMarginRequired(marginRequired);
        
        // 计算预期收益率
        double profitAtTakeProfit = signal.getSignalType() == TradingSignal.SignalType.BUY ?
            (signal.getTakeProfit() - entryPrice) * positionSize :
            (entryPrice - signal.getTakeProfit()) * positionSize;
        double roiPercentage = (profitAtTakeProfit / marginRequired) * 100;
        signal.setRoiPercentage(roiPercentage);
        
        // 根据信号类型计算预估爆仓价格
        if (signal.getSignalType() == TradingSignal.SignalType.BUY) {
            // 多单爆仓价格 = 入场价 - (账户余额 / (杠杆 * 持仓数量))
            double liquidationPrice = entryPrice - (accountBalance / (leverage * positionSize));
            signal.setLiquidationPrice(liquidationPrice);
        } else {
            // 空单爆仓价格 = 入场价 + (账户余额 / (杠杆 * 持仓数量))
            double liquidationPrice = entryPrice + (accountBalance / (leverage * positionSize));
            signal.setLiquidationPrice(liquidationPrice);
        }
        
        // 计算最大头寸风险（占账户余额的百分比）
        double positionRiskPercentage = (marginRequired / accountBalance) * 100;
        signal.setMaxPositionRisk(positionRiskPercentage);
        
        // 设置杠杆倍数
        signal.setLeverage(leverage);
        
        // 设置仓位大小
        signal.setPositionSize(positionSize);
    }
    
    /**
     * 计算动态止损位
     * 根据DeepSeek的动态止损计算方法
     */
    public double calculateStopLoss(String signalType, double entryPrice, double atr, Map<String, Object> keyLevels) {
        if ("BOS突破".equals(signalType)) {
            // 结构突破：止损在原结构内
            Object originalStructure = keyLevels.get("originalStructure");
            if (originalStructure != null) {
                double originalLevel = (double) originalStructure;
                return entryPrice > originalLevel ? originalLevel - (atr * 1.5) : originalLevel + (atr * 1.5);
            }
            return entryPrice - (atr * 1.5); // 默认情况
        } else if ("假突破反向".equals(signalType)) {
            // 假突破：止损在假突破点外
            Object fakeBreakoutPoint = keyLevels.get("fakeBreakoutPoint");
            if (fakeBreakoutPoint != null) {
                double fakeBreakoutLevel = (double) fakeBreakoutPoint;
                return entryPrice > fakeBreakoutLevel ? fakeBreakoutLevel - (atr * 0.5) : fakeBreakoutLevel + (atr * 0.5);
            }
            return entryPrice - (atr * 1.5); // 默认情况
        } else if ("波动率突破".equals(signalType)) {
            // 横盘突破：止损在横盘区间内
            Object consolidationLevel = keyLevels.get("consolidationLevel");
            if (consolidationLevel != null) {
                double zoneLevel = (double) consolidationLevel;
                return entryPrice > zoneLevel ? zoneLevel - (atr * 0.3) : zoneLevel + (atr * 0.3);
            }
            return entryPrice - (atr * 1.5); // 默认情况
        } else {
            // 默认止损计算
            return entryPrice - (atr * 1.5); // 默认止损为1.5倍ATR
        }
    }
    
    /**
     * 根据信号分数动态调整仓位
     * 根据DeepSeek的仓位建议算法
     */
    public double positionSizing(int score, double accountSize, double riskPerTradePercent) {
        // 根据信号分数调整风险倍数
        double riskMultiplier;
        switch (score) {
            case 10:
                riskMultiplier = 1.5; // 满分信号，可以适当超配
                break;
            case 9:
                riskMultiplier = 1.2;
                break;
            case 8:
                riskMultiplier = 1.0; // 标准仓位
                break;
            case 7:
                riskMultiplier = 0.8;
                break;
            case 6:
                riskMultiplier = 0.6; // 减半仓位
                break;
            case 5:
                riskMultiplier = 0.4; // 迷你仓位
                break;
            default:
                riskMultiplier = 0.2; // 一般情况下使用保守仓位
                break;
        }
        
        // 计算基础风险金额
        double baseRiskAmount = accountSize * (riskPerTradePercent / 100.0);
        
        // 应用风险倍数调整
        return baseRiskAmount * riskMultiplier;
    }
    
    /**
     * 为交易信号计算止损和止盈
     */
    public void calculateRiskManagementLevels(TradingSignal signal, List<Candlestick> candlesticks, double currentAtr) {
        if (candlesticks.isEmpty()) return;
        
        double currentPrice = candlesticks.get(candlesticks.size() - 1).getClose();
        
        // 根据信号类型计算止损
        String signalType = signal.getIndicator();
        Map<String, Object> keyLevels = new HashMap<>();
        
        // 简单的信号类型判断
        if (signal.getIndicator().contains("结构信号")) {
            signalType = "BOS突破";
        } else if (signal.getIndicator().contains("假突破")) {
            signalType = "假突破反向";
        } else if (signal.getIndicator().contains("波动率") || signal.getIndicator().contains("挤压")) {
            signalType = "波动率突破";
        }
        
        // 计算止损位
        double stopLoss = calculateStopLoss(signalType, currentPrice, currentAtr, keyLevels);
        signal.setStopLoss(stopLoss);
        
        // 计算止盈位 (风险回报比1:2或1:3)
        double riskDistance = Math.abs(currentPrice - stopLoss);
        double takeProfit;
        
        if (signal.getSignalType() == TradingSignal.SignalType.BUY) {
            // 多单：止盈 = 当前价格 + 风险距离 * 风险回报比
            double ratio = signal.getScore() >= 8 ? 3.0 : (signal.getScore() >= 6 ? 2.5 : 2.0);
            takeProfit = currentPrice + (riskDistance * ratio);
        } else {
            // 空单：止盈 = 当前价格 - 风险距离 * 风险回报比
            double ratio = signal.getScore() >= 8 ? 3.0 : (signal.getScore() >= 6 ? 2.5 : 2.0);
            takeProfit = currentPrice - (riskDistance * ratio);
        }
        
        signal.setTakeProfit(takeProfit);
        
        // 计算合约交易参数（使用解耦的仓位计算方法）
        double accountBalance = 10000.0; // 示例账户余额
        int leverage = determineOptimalLeverage(signal); // 根据信号质量确定最优杠杆
        
        // 使用解耦的仓位计算方法，考虑市场状态
        MarketRegimeController.MarketRegime currentRegime = marketRegimeController.detect(signal.getSymbol(), candlesticks);
        double confidence = SignalScoreCalculator.calculateConfidence(signal, currentRegime, candlesticks);
        double positionSize = SignalScoreCalculator.calculatePositionSize(accountBalance, confidence, currentRegime, signal, candlesticks);
        
        calculateContractParameters(signal, currentPrice, leverage, positionSize, accountBalance);
    }
    
    /**
     * 根据信号质量确定最优杠杆
     */
    public int determineOptimalLeverage(TradingSignal signal) {
        // 根据信号评分和市场波动性确定合适的杠杆
        if (signal.getScore() >= 8) {
            // 高质量信号，可适当提高杠杆
            return 20;
        } else if (signal.getScore() >= 6) {
            // 中等质量信号，适中杠杆
            return 10;
        } else {
            // 低质量信号，保守杠杆
            return 5;
        }
    }
    
    /**
     * 计算最优仓位大小
     */
    public double calculateOptimalPositionSize(TradingSignal signal, double accountBalance, int leverage, double entryPrice) {
        // 使用固定比例资金管理策略
        double riskPercentage = 0.02; // 每笔交易风险2%的账户资金
        double riskAmount = accountBalance * riskPercentage;
        
        double stopLossDistance;
        if (signal.getSignalType() == TradingSignal.SignalType.BUY) {
            stopLossDistance = entryPrice - signal.getStopLoss();
        } else {
            stopLossDistance = signal.getTakeProfit() - entryPrice;
        }
        
        // 避免除零错误
        if (stopLossDistance <= 0) {
            stopLossDistance = entryPrice * 0.02; // 默认2%止损
        }
        
        // 计算仓位大小
        double positionValue = (riskAmount * leverage) / stopLossDistance;
        return Math.min(positionValue, accountBalance * 0.1); // 最大不超过账户的10%
    }
    
    /**
     * 根据信号获取对应的信号模型
     */
    private MarketRegimeController.SignalModel getSignalModel(TradingSignal signal) {
        String indicator = signal.getIndicator().toUpperCase();
        
        if (indicator.contains("趋势") || indicator.contains("动量") || indicator.contains("共振")) {
            return MarketRegimeController.SignalModel.TREND_MOMENTUM_RESONANCE;
        } else if (indicator.contains("机构") || indicator.contains("资金")) {
            return MarketRegimeController.SignalModel.INSTITUTIONAL_FLOW;
        } else if (indicator.contains("波动") || indicator.contains("突破")) {
            return MarketRegimeController.SignalModel.VOLATILITY_BREAKOUT;
        } else if (indicator.contains("关键") || indicator.contains("位置") || indicator.contains("博弈")) {
            return MarketRegimeController.SignalModel.KEY_LEVEL_BATTLEGROUNDS;
        } else if (indicator.contains("情绪") || indicator.contains("极端")) {
            return MarketRegimeController.SignalModel.SENTIMENT_EXTREMES;
        } else if (indicator.contains("相关") || indicator.contains("套利")) {
            return MarketRegimeController.SignalModel.CORRELATION_ARBITRAGE;
        } else {
            // 默认返回趋势动量共振模型
            return MarketRegimeController.SignalModel.TREND_MOMENTUM_RESONANCE;
        }
    }
    
    /**
     * 记录信号绩效
     */
    public void recordSignalPerformance(List<TradingSignal> signals) {
        for (TradingSignal signal : signals) {
            signalPerformanceTracker.recordSignal(signal);
            // 同时记录到性能监控器
            performanceMonitor.recordSignalGenerated(signal);
        }
    }
}
    

