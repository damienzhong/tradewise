package com.example.tradewise.service;

import com.example.tradewise.config.TradeWiseProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 数据引擎 - 实现Gemini提出的模块化交易辅助引擎
 * 每分钟运行一次，整合多时间框架数据和外部信号
 */
@Service
public class DataEngine {

    private static final Logger logger = LoggerFactory.getLogger(DataEngine.class);

    @Autowired
    private MarketDataService marketDataService;

    @Autowired
    private SmartDataEngine smartDataEngine;

    @Autowired
    private TradeWiseProperties tradeWiseProperties;

    // 缓存最近的市场数据，避免重复请求
    private final Map<String, CachedMarketData> cachedData = new ConcurrentHashMap<>();

    // 用于存储优秀带单员的当前持仓方向
    private final Map<String, SignalDirection> traderPositions = new ConcurrentHashMap<>();

    // 冷却检查：同币种1小时内不重复发信
    private final Map<String, LocalDateTime> lastSignalTime = new ConcurrentHashMap<>();
    
    // 信号状态管理
    private final Map<String, SignalStateManager> signalStates = new ConcurrentHashMap<>();

    /**
     * 市场状态(Regime)枚举
     */
    public enum MarketRegime {
        STRONG_TREND,      // 强趋势
        WEAK_TREND,        // 弱趋势
        RANGE,             // 震荡
        VOLATILITY_EXPAND, // 波动扩张
        VOLATILITY_SQUEEZE // 波动收缩
    }
    
    /**
     * 信号状态枚举
     */
    public enum SignalState {
        SETUP,        // 结构形成中
        TRIGGERED,    // 已触发
        CONFIRMED,    // 已验证
        INVALIDATED,  // 已失效
        COOLDOWN      // 冷却期
    }
    
    /**
     * 信号状态管理器
     */
    public static class SignalStateManager {
        private SignalState state;
        private LocalDateTime stateChangeTime;
        private String signalType;
        private String symbol;
        
        public SignalStateManager(String signalType, String symbol) {
            this.state = SignalState.SETUP;
            this.stateChangeTime = LocalDateTime.now();
            this.signalType = signalType;
            this.symbol = symbol;
        }
        
        public SignalState getState() {
            return state;
        }
        
        public void setState(SignalState newState) {
            this.state = newState;
            this.stateChangeTime = LocalDateTime.now();
        }
        
        public LocalDateTime getStateChangeTime() {
            return stateChangeTime;
        }
        
        public String getSignalType() {
            return signalType;
        }
        
        public String getSymbol() {
            return symbol;
        }
        
        public boolean isCooldownExpired() {
            int cooldownMinutes = 60; // 默认60分钟冷却期
            return LocalDateTime.now().isAfter(stateChangeTime.plusMinutes(cooldownMinutes));
        }
    }

    /**
     * 缓存的市场数据
     */
    public static class CachedMarketData {
        private final Map<String, List<MarketAnalysisService.Candlestick>> candlesticksByTimeframe;
        private final LocalDateTime timestamp;
        private MarketRegime currentRegime;

        public CachedMarketData() {
            this.candlesticksByTimeframe = new HashMap<>();
            this.timestamp = LocalDateTime.now();
            this.currentRegime = MarketRegime.RANGE; // 默认为震荡
        }

        public void addCandlesticks(String timeframe, List<MarketAnalysisService.Candlestick> candlesticks) {
            candlesticksByTimeframe.put(timeframe, candlesticks);
        }

        public List<MarketAnalysisService.Candlestick> getCandlesticks(String timeframe) {
            return candlesticksByTimeframe.get(timeframe);
        }

        public LocalDateTime getTimestamp() {
            return timestamp;
        }
        
        public MarketRegime getCurrentRegime() {
            return currentRegime;
        }
        
        public void setCurrentRegime(MarketRegime regime) {
            this.currentRegime = regime;
        }
    }

    /**
     * 信号方向枚举
     */
    public enum SignalDirection {
        LONG, SHORT, NEUTRAL
    }

    /**
     * 信号评分结果
     */
    public static class SignalScoreResult {
        private final int score;
        private final List<String> contributingFactors;
        private final double riskRewardRatio;
        private final double positionSize;
        private final Map<String, Object> signalExplanation; // 信号解释，用于回放和调试

        public SignalScoreResult(int score, List<String> contributingFactors, double riskRewardRatio, double positionSize, Map<String, Object> signalExplanation) {
            this.score = score;
            this.contributingFactors = contributingFactors;
            this.riskRewardRatio = riskRewardRatio;
            this.positionSize = positionSize;
            this.signalExplanation = signalExplanation != null ? signalExplanation : new HashMap<>();
        }

        // getters
        public int getScore() { return score; }
        public List<String> getContributingFactors() { return contributingFactors; }
        public double getRiskRewardRatio() { return riskRewardRatio; }
        public double getPositionSize() { return positionSize; }
        public Map<String, Object> getSignalExplanation() { return signalExplanation; }
    }

    /**
     * 执行数据引擎主逻辑
     */
    public void execute() {
        logger.info("开始执行数据引擎主逻辑");

        // 1. 获取监控的交易对列表
        String[] symbolsToMonitor = tradeWiseProperties.getMarketAnalysis().getSymbolsToMonitor();

        for (String symbol : symbolsToMonitor) {
            try {
                // 2. 获取多时间框架数据
                Map<String, List<MarketAnalysisService.Candlestick>> multiTimeframeData = getMultiTimeframeData(symbol);
                
                // 3. 识别市场状态(Regime)
                MarketRegime currentRegime = identifyMarketRegime(symbol, multiTimeframeData);
                CachedMarketData cached = cachedData.get(symbol + "_1h");
                if (cached != null) {
                    cached.setCurrentRegime(currentRegime);
                }

                // 4. 使用六维智能合约交易系统进行分析
                performSixDimensionAnalysis(symbol, multiTimeframeData, currentRegime);

                // 5. 为每个交易对执行完整的信号分析流程
                analyzeSymbol(symbol, multiTimeframeData, currentRegime);

            } catch (Exception e) {
                logger.error("处理交易对 {} 时发生错误", symbol, e);
            }
        }

        logger.info("数据引擎主逻辑执行完成");
    }

    /**
     * 使用六维智能合约交易系统进行分析
     */
    private void performSixDimensionAnalysis(String symbol, Map<String, List<MarketAnalysisService.Candlestick>> multiTimeframeData, MarketRegime currentRegime) {
        logger.info("开始六维智能合约交易系统分析，交易对: {}", symbol);

        try {
            // 创建六维模型实例
            TrendMomentumResonanceModel trendMomentumModel = new TrendMomentumResonanceModel();
            InstitutionalFlowModel institutionalFlowModel = new InstitutionalFlowModel();
            VolatilityBreakoutModel volatilityBreakoutModel = new VolatilityBreakoutModel();
            KeyLevelBattlefieldModel keyLevelBattlefieldModel = new KeyLevelBattlefieldModel();
            SentimentExtremesModel sentimentExtremesModel = new SentimentExtremesModel();
            CorrelationArbitrageModel correlationArbitrageModel = new CorrelationArbitrageModel();

            // 检测各模型信号
            List<SignalFusionEngine.TradingSignal> detectedSignals = new ArrayList<>();

            trendMomentumModel.detect(symbol, multiTimeframeData).ifPresent(detectedSignals::add);
            institutionalFlowModel.detect(symbol, multiTimeframeData).ifPresent(detectedSignals::add);
            volatilityBreakoutModel.detect(symbol, multiTimeframeData).ifPresent(detectedSignals::add);
            keyLevelBattlefieldModel.detect(symbol, multiTimeframeData).ifPresent(detectedSignals::add);
            sentimentExtremesModel.detect(symbol, multiTimeframeData).ifPresent(detectedSignals::add);

            // 相关性套利模型需要额外的交易对数据
            Map<String, List<MarketAnalysisService.Candlestick>> allSymbolsData = new HashMap<>();
            allSymbolsData.put(symbol, multiTimeframeData.get("1h")); // 使用1小时数据作为基础
            correlationArbitrageModel.detect(symbol, multiTimeframeData, allSymbolsData).ifPresent(detectedSignals::add);

            if (!detectedSignals.isEmpty()) {
                logger.info("六维智能合约交易系统检测到 {} 个信号，交易对: {}", detectedSignals.size(), symbol);

                // 创建信号融合引擎
                SignalFusionEngine fusionEngine = new SignalFusionEngine();

                // 执行信号融合
                SignalFusionEngine.FusionResult fusionResult = fusionEngine.fuseSignals(detectedSignals, currentRegime, symbol, multiTimeframeData.get("1h"));

                // 如果融合结果为交易决策，则执行智能执行优化
                if (fusionResult.getFinalDecision() != SignalFusionEngine.Decision.NO_TRADE) {
                    logger.info("信号融合结果: 决策={}, 强度={}, 置信度={}, 交易对: {}", 
                              fusionResult.getFinalDecision(), fusionResult.getAggregatedStrength(), 
                              fusionResult.getConfidence(), symbol);

                    // 创建智能执行优化器
                    SmartExecutionOptimizer executionOptimizer = new SmartExecutionOptimizer();

                    // 生成执行计划
                    SmartExecutionOptimizer.ExecutionPlan executionPlan = executionOptimizer.generateExecutionPlan(
                        fusionResult, symbol, multiTimeframeData);

                    // 记录执行计划
                    logger.info("生成执行计划: 决策={}, 入场价={}, 止损={}, 止盈={}, 仓位={}, 风险等级={}, 交易对: {}", 
                              executionPlan.getDecision(), executionPlan.getEntryPrice(), 
                              executionPlan.getStopLoss(), executionPlan.getTakeProfit(),
                              executionPlan.getPositionSize(), executionPlan.getRiskLevel(), symbol);

                    // 这里可以添加实际的订单执行逻辑
                    // executeOrder(executionPlan);
                }
            } else {
                logger.info("六维智能合约交易系统未检测到信号，交易对: {}", symbol);
            }
        } catch (Exception e) {
            logger.error("执行六维智能合约交易系统分析时发生错误，交易对: {}", symbol, e);
        }
    }

    /**
     * 识别市场状态(Regime)
     */
    private MarketRegime identifyMarketRegime(String symbol, Map<String, List<MarketAnalysisService.Candlestick>> multiTimeframeData) {
        List<MarketAnalysisService.Candlestick> hourlyData = multiTimeframeData.get("1h");
        if (hourlyData == null || hourlyData.size() < 50) {
            return MarketRegime.RANGE; // 数据不足时默认为震荡
        }

        // 计算趋势强度
        MarketAnalysisService.TechnicalIndicators indicators = new MarketAnalysisService.TechnicalIndicators();
        List<Double> ema50 = indicators.calculateEMA(hourlyData, 50);
        List<Double> atr14 = indicators.calculateATR(hourlyData, 14);
        
        if (ema50.isEmpty() || atr14.isEmpty()) {
            return MarketRegime.RANGE;
        }
        
        // 获取最新的价格和指标
        double currentPrice = hourlyData.get(hourlyData.size() - 1).getClose();
        Double latestEma50 = ema50.get(ema50.size() - 1);
        Double latestAtr = atr14.get(atr14.size() - 1);
        
        if (latestEma50 == null || latestAtr == null) {
            return MarketRegime.RANGE;
        }
        
        // 计算价格偏离均线的程度
        double deviation = Math.abs(currentPrice - latestEma50) / latestEma50;
        
        // 计算ATR的变化趋势
        int atrExpansionCount = 0;
        for (int i = Math.max(0, atr14.size() - 10); i < atr14.size() - 1; i++) {
            if (atr14.get(i + 1) != null && atr14.get(i) != null && atr14.get(i + 1) > atr14.get(i)) {
                atrExpansionCount++;
            }
        }
        
        // 根据不同指标判断市场状态
        if (deviation > 0.03) { // 价格偏离均线超过3%
            return MarketRegime.STRONG_TREND;
        } else if (deviation > 0.015) { // 价格偏离均线超过1.5%
            return MarketRegime.WEAK_TREND;
        } else if (atrExpansionCount >= 7) { // ATR连续扩张
            return MarketRegime.VOLATILITY_EXPAND;
        } else if (atrExpansionCount <= 2) { // ATR连续收缩
            return MarketRegime.VOLATILITY_SQUEEZE;
        } else {
            return MarketRegime.RANGE; // 震荡市场
        }
    }

    /**
     * 获取多时间框架数据
     */
    private Map<String, List<MarketAnalysisService.Candlestick>> getMultiTimeframeData(String symbol) {
        Map<String, List<MarketAnalysisService.Candlestick>> result = new HashMap<>();
        String[] timeframes = {"15m", "1h", "4h"};

        for (String timeframe : timeframes) {
            // 检查缓存
            CachedMarketData cached = cachedData.get(symbol + "_" + timeframe);
            if (cached != null && cached.getTimestamp().isAfter(LocalDateTime.now().minusMinutes(1))) {
                result.put(timeframe, cached.getCandlesticks(timeframe));
                continue;
            }

            try {
                // 从市场数据服务获取数据
                List<MarketAnalysisService.Candlestick> candlesticks = marketDataService.getKlines(symbol, timeframe, 200);
                
                if (!candlesticks.isEmpty()) {
                    result.put(timeframe, candlesticks);
                    
                    // 缓存数据
                    CachedMarketData cacheEntry = new CachedMarketData();
                    cacheEntry.addCandlesticks(timeframe, candlesticks);
                    cachedData.put(symbol + "_" + timeframe, cacheEntry);
                } else {
                    logger.warn("未能获取 {} 的 {} 数据", symbol, timeframe);
                }
            } catch (Exception e) {
                logger.error("获取 {} 的 {} 数据时发生错误", symbol, timeframe, e);
            }
        }

        return result;
    }

    /**
     * 分析单个交易对
     */
    private void analyzeSymbol(String symbol, Map<String, List<MarketAnalysisService.Candlestick>> multiTimeframeData, MarketRegime currentRegime) {
        // 获取主要时间框架的数据（1小时）
        List<MarketAnalysisService.Candlestick> hourlyData = multiTimeframeData.get("1h");
        if (hourlyData == null || hourlyData.isEmpty()) {
            logger.warn("缺少 {} 的1小时数据，跳过分析", symbol);
            return;
        }

        MarketAnalysisService.Candlestick currentHourlyCandle = hourlyData.get(hourlyData.size() - 1);
        double currentPrice = currentHourlyCandle.getClose();

        // 1. 执行评分制逻辑
        SignalScoreResult scoreResult = calculateSignalScore(symbol, multiTimeframeData, currentRegime);

        // 2. 检查是否满足信号触发条件
        if (scoreResult.getScore() >= 6) {
            logger.info("交易对 {} 得分 {}，进入下一步分析", symbol, scoreResult.getScore());

            // 3. 计算风险参数
            double stopLoss = calculateStopLoss(symbol, multiTimeframeData);
            double riskRewardRatio = scoreResult.getRiskRewardRatio();

            // 4. 关键过滤：RR < 配置的最小RR直接放弃
            if (riskRewardRatio >= tradeWiseProperties.getMarketAnalysis().getRiskManagement().getMinRiskRewardRatio()) {
                logger.info("交易对 {} 风险回报比 {} >= 配置阈值，符合要求", symbol, riskRewardRatio);

                // 5. 计算仓位
                double position = calculatePositionSize(scoreResult.getPositionSize(), currentPrice, stopLoss, currentRegime, scoreResult.getScore());

                // 6. 执行自适应风险检查
                if (isRiskCheckPassed(symbol)) {
                    // 7. 发送通知
                    sendSignalNotification(symbol, currentPrice, scoreResult, stopLoss, riskRewardRatio, position, currentRegime);
                } else {
                    logger.info("交易对 {} 未通过风险检查，跳过发送", symbol);
                }
            } else {
                logger.info("交易对 {} 风险回报比 {} < 配置阈值，不符合要求", symbol, riskRewardRatio);
            }
        } else {
            logger.debug("交易对 {} 得分 {} < 6，未达到触发条件", symbol, scoreResult.getScore());
        }
    }

    /**
     * 计算信号评分
     */
    private SignalScoreResult calculateSignalScore(String symbol, Map<String, List<MarketAnalysisService.Candlestick>> multiTimeframeData, MarketRegime currentRegime) {
        int rawScore = 0;
        List<String> contributingFactors = new ArrayList<>();
        double riskRewardRatio = 0.0;
        double positionSize = 0.0;
        
        // 信号解释信息
        Map<String, Object> signalExplanation = new HashMap<>();
        signalExplanation.put("regime", currentRegime.name());
        signalExplanation.put("timestamp", LocalDateTime.now());

        // 获取不同时间框架的数据
        List<MarketAnalysisService.Candlestick> hourlyData = multiTimeframeData.get("1h");
        List<MarketAnalysisService.Candlestick> fifteenMinData = multiTimeframeData.get("15m");
        List<MarketAnalysisService.Candlestick> fourHourData = multiTimeframeData.get("4h");

        if (hourlyData == null || hourlyData.isEmpty()) {
            return new SignalScoreResult(0, contributingFactors, riskRewardRatio, positionSize, signalExplanation);
        }

        MarketAnalysisService.Candlestick currentHourlyCandle = hourlyData.get(hourlyData.size() - 1);
        double currentPrice = currentHourlyCandle.getClose();
        double currentHigh = currentHourlyCandle.getHigh();
        double currentLow = currentHourlyCandle.getLow();

        // 1. 趋势分：1h 价格在 EMA200 同侧 (+2分)
        if (isPriceAboveEMA200(hourlyData, currentPrice)) {
            rawScore += 2;
            contributingFactors.add("趋势分: 价格高于EMA200");
        }

        // 2. 结构分：15m 发生 BOS (结构破坏) (+3分)
        if (fifteenMinData != null && detectBOS(fifteenMinData, currentPrice)) {
            rawScore += 3;
            contributingFactors.add("结构分: 检测到BOS结构破坏");
        }

        // 3. 位置分：价格在关键位 ±ATR 范围内 (+2分)
        if (isNearKeyLevel(hourlyData, currentPrice)) {
            rawScore += 2;
            contributingFactors.add("位置分: 价格在关键位附近");
        }

        // 4. 量能分：突破 K 线成交量 > 均量 (+1分)
        if (hasVolumeBreakout(hourlyData)) {
            rawScore += 1;
            contributingFactors.add("量能分: 成交量突破均量");
        }

        // 5. 持仓分：价格与 OI (持仓量) 同向变动 (+1分)
        // 这里简化处理，实际应用中需要获取持仓量数据
        if (hasPositiveCorrelationWithOI(hourlyData, currentPrice)) {
            rawScore += 1;
            contributingFactors.add("持仓分: 价格与持仓量同向变动");
        }

        // 6. 辅助分：至少 2 名带单员与信号方向一致 (+2分)
        if (hasConsistentTraderPositions(symbol)) {
            rawScore += 2;
            contributingFactors.add("辅助分: 带单员方向一致");
        }

        // 应用市场状态(Regime)调整因子
        double marketConfidenceFactor = getMarketConfidenceFactor(currentRegime, contributingFactors);
        double signalQualityFactor = getSignalQualityFactor(contributingFactors); // 根据信号类型调整
        double volatilityPenalty = getVolatilityPenalty(currentRegime); // 波动率惩罚
        
        // 非线性评分调整
        int finalScore = (int) Math.round(rawScore * marketConfidenceFactor * signalQualityFactor * volatilityPenalty);
        finalScore = Math.max(0, Math.min(10, finalScore)); // 限制在0-10分之间
        
        signalExplanation.put("rawScore", rawScore);
        signalExplanation.put("adjustedFactors", Arrays.asList(
            "市场信心因子: " + marketConfidenceFactor,
            "信号质量因子: " + signalQualityFactor,
            "波动率惩罚: " + volatilityPenalty
        ));

        // 计算风险回报比
        riskRewardRatio = calculateRiskRewardRatio(symbol, multiTimeframeData, currentPrice);

        // 计算仓位大小
        positionSize = calculateBasePositionSize();

        return new SignalScoreResult(finalScore, contributingFactors, riskRewardRatio, positionSize, signalExplanation);
    }
    
    /**
     * 获取市场信心因子，根据市场状态调整
     */
    private double getMarketConfidenceFactor(MarketRegime regime, List<String> contributingFactors) {
        switch (regime) {
            case STRONG_TREND:
                // 在强趋势中，BOS信号更可靠
                if (contributingFactors.contains("结构分: 检测到BOS结构破坏")) {
                    return 1.2;
                }
                return 1.0;
            case WEAK_TREND:
                // 在弱趋势中，所有信号都稍微降低权重
                return 0.9;
            case RANGE:
                // 在震荡市场中，假突破信号更可靠
                if (contributingFactors.contains("结构分: 检测到BOS结构破坏")) {
                    return 0.7; // BOS在震荡市场中可靠性较低
                }
                return 1.0;
            case VOLATILITY_SQUEEZE:
                // 在波动收缩中，突破信号更有价值
                return 1.1;
            case VOLATILITY_EXPAND:
                // 在波动扩张中，趋势信号更可靠
                return 1.0;
            default:
                return 1.0;
        }
    }
    
    /**
     * 获取信号质量因子，根据历史统计调整
     */
    private double getSignalQualityFactor(List<String> contributingFactors) {
        // 这里可以根据历史数据进行调整
        // 暂时返回一个默认值，实际应用中可以基于历史胜率进行调整
        return 1.0;
    }
    
    /**
     * 获取波动率惩罚，高波动环境下降低信号权重
     */
    private double getVolatilityPenalty(MarketRegime regime) {
        switch (regime) {
            case VOLATILITY_EXPAND:
                // 在高波动扩张环境中，给予一定惩罚
                return 0.8;
            case VOLATILITY_SQUEEZE:
                // 在波动收缩环境中，信号可能更有价值
                return 1.0;
            default:
                return 1.0;
        }
    }

    /**
     * 检查价格是否在EMA200上方
     */
    private boolean isPriceAboveEMA200(List<MarketAnalysisService.Candlestick> hourlyData, double currentPrice) {
        if (hourlyData.size() < 200) {
            return false;
        }

        // 计算200周期EMA
        MarketAnalysisService.TechnicalIndicators indicators = new MarketAnalysisService.TechnicalIndicators();
        List<Double> ema200 = indicators.calculateEMA(hourlyData, 200);

        if (!ema200.isEmpty() && ema200.get(ema200.size() - 1) != null) {
            return currentPrice > ema200.get(ema200.size() - 1);
        }

        return false;
    }

    /**
     * 检测BOS (Break of Structure) 结构破坏
     */
    private boolean detectBOS(List<MarketAnalysisService.Candlestick> fifteenMinData, double currentPrice) {
        if (fifteenMinData.size() < 10) {
            return false;
        }

        // 简化BOS检测：寻找摆动高低点的突破
        // 查找最近的摆动高点和低点
        List<Double> swingHighs = new ArrayList<>();
        List<Double> swingLows = new ArrayList<>();

        for (int i = 2; i < fifteenMinData.size() - 2; i++) {
            MarketAnalysisService.Candlestick current = fifteenMinData.get(i);
            MarketAnalysisService.Candlestick prev1 = fifteenMinData.get(i - 1);
            MarketAnalysisService.Candlestick prev2 = fifteenMinData.get(i - 2);
            MarketAnalysisService.Candlestick next1 = fifteenMinData.get(i + 1);
            MarketAnalysisService.Candlestick next2 = fifteenMinData.get(i + 2);

            // 检测摆动高点（当前高点比相邻两个高点都高）
            if (current.getHigh() > prev1.getHigh() && current.getHigh() > prev2.getHigh() &&
                current.getHigh() > next1.getHigh() && current.getHigh() > next2.getHigh()) {
                swingHighs.add(current.getHigh());
            }

            // 检测摆动低点（当前低点比相邻两个低点都低）
            if (current.getLow() < prev1.getLow() && current.getLow() < prev2.getLow() &&
                current.getLow() < next1.getLow() && current.getLow() < next2.getLow()) {
                swingLows.add(current.getLow());
            }
        }

        // 检查当前价格是否突破了最近的摆动点
        if (!swingHighs.isEmpty() && currentPrice > swingHighs.get(swingHighs.size() - 1)) {
            return true; // 向上突破结构
        }
        if (!swingLows.isEmpty() && currentPrice < swingLows.get(swingLows.size() - 1)) {
            return true; // 向下突破结构
        }

        return false;
    }

    /**
     * 检查价格是否在关键位附近
     */
    private boolean isNearKeyLevel(List<MarketAnalysisService.Candlestick> hourlyData, double currentPrice) {
        if (hourlyData.size() < 24) {
            return false;
        }

        // 获取近期的高点和低点作为关键位
        List<Double> highs = new ArrayList<>();
        List<Double> lows = new ArrayList<>();
        for (MarketAnalysisService.Candlestick candle : hourlyData) {
            highs.add(candle.getHigh());
            lows.add(candle.getLow());
        }

        // 获取最近24小时的最高和最低价
        double recentHigh = highs.subList(Math.max(0, highs.size() - 24), highs.size())
                .stream().max(Double::compareTo).orElse(currentPrice);
        double recentLow = lows.subList(Math.max(0, lows.size() - 24), lows.size())
                .stream().min(Double::compareTo).orElse(currentPrice);

        // 计算ATR
        MarketAnalysisService.TechnicalIndicators indicators = new MarketAnalysisService.TechnicalIndicators();
        List<Double> atrList = indicators.calculateATR(hourlyData, 14);
        double currentAtr = atrList.get(atrList.size() - 1);

        if (currentAtr > 0) {
            // 检查价格是否在关键位±ATR范围内
            return (Math.abs(currentPrice - recentHigh) <= currentAtr) ||
                   (Math.abs(currentPrice - recentLow) <= currentAtr);
        }

        return false;
    }

    /**
     * 检查是否有成交量突破
     */
    private boolean hasVolumeBreakout(List<MarketAnalysisService.Candlestick> hourlyData) {
        if (hourlyData.size() < 20) {
            return false;
        }

        // 计算成交量均值
        double volumeSum = 0;
        for (int i = Math.max(0, hourlyData.size() - 20); i < hourlyData.size(); i++) {
            volumeSum += hourlyData.get(i).getVolume();
        }
        double avgVolume = volumeSum / Math.min(20, hourlyData.size());

        // 检查当前成交量是否超过均量
        double currentVolume = hourlyData.get(hourlyData.size() - 1).getVolume();
        return currentVolume > avgVolume;
    }

    /**
     * 检查价格与持仓量是否同向变动（简化实现）
     */
    private boolean hasPositiveCorrelationWithOI(List<MarketAnalysisService.Candlestick> hourlyData, double currentPrice) {
        // 这里简化实现，实际应用中需要接入真实的持仓量数据
        // 为了演示目的，我们简单地检查价格趋势
        if (hourlyData.size() < 10) {
            return false;
        }

        // 获取最近的价格趋势
        double prevPrice = hourlyData.get(hourlyData.size() - 10).getClose();
        return (currentPrice - prevPrice) > 0; // 价格上涨视为正向关系
    }

    /**
     * 检查带单员方向是否一致
     */
    private boolean hasConsistentTraderPositions(String symbol) {
        // 检查是否有至少2名带单员与当前信号方向一致
        SignalDirection currentPosition = getCurrentMarketDirection(symbol);
        SignalDirection trader1Pos = traderPositions.getOrDefault("TRADER_1_" + symbol, SignalDirection.NEUTRAL);
        SignalDirection trader2Pos = traderPositions.getOrDefault("TRADER_2_" + symbol, SignalDirection.NEUTRAL);
        SignalDirection trader3Pos = traderPositions.getOrDefault("TRADER_3_" + symbol, SignalDirection.NEUTRAL);

        int consistentCount = 0;
        if (currentPosition == trader1Pos && trader1Pos != SignalDirection.NEUTRAL) consistentCount++;
        if (currentPosition == trader2Pos && trader2Pos != SignalDirection.NEUTRAL) consistentCount++;
        if (currentPosition == trader3Pos && trader3Pos != SignalDirection.NEUTRAL) consistentCount++;

        return consistentCount >= 2;
    }

    /**
     * 获取当前市场方向（基于简单趋势判断）
     */
    private SignalDirection getCurrentMarketDirection(String symbol) {
        List<MarketAnalysisService.Candlestick> hourlyData = cachedData.get(symbol + "_1h").getCandlesticks("1h");
        if (hourlyData == null || hourlyData.size() < 10) {
            return SignalDirection.NEUTRAL;
        }

        double currentPrice = hourlyData.get(hourlyData.size() - 1).getClose();
        double prevPrice = hourlyData.get(hourlyData.size() - 10).getClose();

        return currentPrice > prevPrice ? SignalDirection.LONG : SignalDirection.SHORT;
    }

    /**
     * 计算风险回报比
     */
    private double calculateRiskRewardRatio(String symbol, Map<String, List<MarketAnalysisService.Candlestick>> multiTimeframeData, double entryPrice) {
        // 计算止损位
        double stopLoss = calculateStopLoss(symbol, multiTimeframeData);
        
        // 简化的目标位计算（可以根据具体策略调整）
        List<MarketAnalysisService.Candlestick> hourlyData = multiTimeframeData.get("1h");
        if (hourlyData == null || hourlyData.isEmpty()) {
            return 0.0;
        }

        // 使用ATR来估算目标位
        MarketAnalysisService.TechnicalIndicators indicators = new MarketAnalysisService.TechnicalIndicators();
        List<Double> atrList = indicators.calculateATR(hourlyData, 14);
        double currentAtr = atrList.get(atrList.size() - 1);

        if (currentAtr <= 0) {
            return 0.0;
        }

        // 目标位 = 入场价 + (风险距离 * 2.0) - 这是一个简化的计算
        double riskDistance = Math.abs(entryPrice - stopLoss);
        double targetPrice = entryPrice + riskDistance * 2.0; // 假设2:1的风险回报比

        // 计算实际的风险回报比
        if (riskDistance > 0) {
            return (targetPrice - entryPrice) / riskDistance;
        }

        return 0.0;
    }

    /**
     * 计算止损位
     */
    private double calculateStopLoss(String symbol, Map<String, List<MarketAnalysisService.Candlestick>> multiTimeframeData) {
        List<MarketAnalysisService.Candlestick> hourlyData = multiTimeframeData.get("1h");
        if (hourlyData == null || hourlyData.isEmpty()) {
            return 0.0;
        }

        MarketAnalysisService.Candlestick currentCandle = hourlyData.get(hourlyData.size() - 1);
        double currentPrice = currentCandle.getClose();

        // 使用ATR计算止损位，简化为当前价格 - 1.5倍ATR
        MarketAnalysisService.TechnicalIndicators indicators = new MarketAnalysisService.TechnicalIndicators();
        List<Double> atrList = indicators.calculateATR(hourlyData, 14);
        double currentAtr = atrList.get(atrList.size() - 1);

        if (currentAtr > 0) {
            return currentPrice - (currentAtr * 1.5);
        }

        return currentPrice * 0.98; // 默认2%止损
    }

    /**
     * 计算基础仓位大小
     */
    private double calculateBasePositionSize() {
        // 固定仓位计算：账户总额 * 配置的最大风险百分比 / abs(入场价 - 止损价)
        // 这里使用配置的参数，而不是硬编码
        double accountBalance = 10000.0; // 默认账户余额
        double riskPercentage = tradeWiseProperties.getMarketAnalysis().getRiskManagement().getMaxPositionRiskPercent() / 100.0; // 转换为小数

        return accountBalance * riskPercentage;
    }

    /**
     * 根据风险距离计算最终仓位大小，考虑市场状态和信号质量
     */
    private double calculatePositionSize(double baseRisk, double entryPrice, double stopLoss, MarketRegime regime, int score) {
        if (Math.abs(entryPrice - stopLoss) <= 0) {
            return 0.0;
        }

        // 仓位 = 风险金额 / |入场价 - 止损价|
        double position = baseRisk / Math.abs(entryPrice - stopLoss);
        
        // 根据市场状态调整仓位
        double regimeMultiplier = getRegimeMultiplier(regime);
        
        // 根据信号质量调整仓位
        double kellyFraction = getKellyFraction(score); // 基于信号评分
        
        // 最大仓位限制，防止过度自信
        double confidenceCap = 1.2; // 永远不超过1.2倍基础仓位
        
        return Math.min(position * regimeMultiplier * kellyFraction, position * confidenceCap);
    }
    
    /**
     * 获取基于市场状态的仓位乘数
     */
    private double getRegimeMultiplier(MarketRegime regime) {
        switch (regime) {
            case STRONG_TREND:
                return 1.0; // 强趋势中正常仓位
            case WEAK_TREND:
                return 0.8; // 弱趋势中减小仓位
            case RANGE:
                return 0.6; // 震荡市场中小仓位
            case VOLATILITY_EXPAND:
                return 0.7; // 高波动中谨慎
            case VOLATILITY_SQUEEZE:
                return 0.9; // 波动收缩后突破需要谨慎
            default:
                return 0.8; // 默认较为保守
        }
    }
    
    /**
     * 基于信号评分获取凯利分数
     */
    private double getKellyFraction(int score) {
        // 根据信号评分调整仓位，但不过度自信
        if (score >= 9) {
            return 1.0; // 评分很高，全仓
        } else if (score >= 7) {
            return 0.8; // 评分较高，80%仓位
        } else if (score >= 6) {
            return 0.6; // 评分中等，60%仓位
        } else {
            return 0.4; // 评分较低，40%仓位
        }
    }

    /**
     * 风险检查：检查是否满足发送信号的条件
     */
    private boolean isRiskCheckPassed(String symbol) {
        // 冷却检查：同币种1小时内不重复发信
        LocalDateTime lastSignal = lastSignalTime.get(symbol);
        if (lastSignal != null && lastSignal.isAfter(LocalDateTime.now().minusHours(1))) {
            logger.debug("交易对 {} 在1小时内已有信号，跳过发送", symbol);
            return false;
        }

        // 黑名单检查：避开成交额极低或手续费极高的币种
        // 这里简化实现，实际应用中需要检查流动性数据
        if (isBlacklistedSymbol(symbol)) {
            logger.debug("交易对 {} 在黑名单中，跳过发送", symbol);
            return false;
        }

        return true;
    }

    /**
     * 检查是否为黑名单币种
     */
    private boolean isBlacklistedSymbol(String symbol) {
        // 简化实现：检查是否为低流动性币种
        // 实际应用中可以从配置或数据库中加载黑名单
        String[] blacklist = {"LOWLIQUSDT", "ILLIQUSDT"}; // 示例黑名单
        for (String blacklisted : blacklist) {
            if (blacklisted.equalsIgnoreCase(symbol)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 发送信号通知
     */
    private void sendSignalNotification(String symbol, double price, SignalScoreResult scoreResult, 
                                      double stopLoss, double riskRewardRatio, double positionSize, MarketRegime regime) {
        logger.info("[{}] - {} - {} - 得分: {} - Regime: {} - 止损: {} - 止盈: {} - 仓位: {}", 
                   getSignalLevel(scoreResult.getScore()), symbol, 
                   getCurrentMarketDirection(symbol), 
                   scoreResult.getScore(),
                   regime,
                   stopLoss, 
                   price + (price - stopLoss) * riskRewardRatio, // 简化的止盈计算
                   positionSize);

        // 更新最后信号时间
        lastSignalTime.put(symbol, LocalDateTime.now());

        // 记录信号解释信息，用于回放和调试
        logger.info("信号解释: {}", scoreResult.getSignalExplanation());
        logger.info("信号详情: 因素={}, RR={}", String.join(", ", scoreResult.getContributingFactors()), riskRewardRatio);
    }

    /**
     * 根据得分获取信号级别
     */
    private String getSignalLevel(int score) {
        if (score >= 8) {
            return "LEVEL_1";
        } else if (score >= 6) {
            return "LEVEL_2";
        } else {
            return "LEVEL_3";
        }
    }

    /**
     * 更新带单员持仓方向
     */
    public void updateTraderPosition(String traderId, String symbol, SignalDirection direction) {
        traderPositions.put(traderId + "_" + symbol, direction);
    }

    /**
     * 清理过期的缓存数据
     */
    public void cleanupExpiredCache() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(10); // 10分钟前的数据视为过期
        cachedData.entrySet().removeIf(entry -> entry.getValue().getTimestamp().isBefore(cutoff));
    }
}