package com.example.tradewise.controller;

import com.example.tradewise.service.*;
import com.example.tradewise.service.MarketAnalysisService.Candlestick;
import com.example.tradewise.service.SignalFusionEngine;
import com.example.tradewise.service.SmartExecutionOptimizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 高级信号控制器
 * 用于管理六维智能合约交易系统的各个组件
 */
@RestController
@RequestMapping("/api/advanced-signals")
public class AdvancedSignalController {

    private static final Logger logger = LoggerFactory.getLogger(AdvancedSignalController.class);

    @Autowired
    private MarketAnalysisService marketAnalysisService;

    @Autowired
    private MarketDataService marketDataService;

    @Autowired
    private SmartDataEngine smartDataEngine;

    /**
     * 生成六维智能合约交易系统信号
     */
    @PostMapping("/generate-six-dimension-signal")
    public Map<String, Object> generateSixDimensionSignal(@RequestParam String symbol) {
        try {
            logger.info("开始生成六维智能合约交易系统信号，交易对: {}", symbol);

            // 获取多时间框架数据
            Map<String, List<Candlestick>> multiTimeframeData = smartDataEngine.getMultiTimeframeData(symbol);

            if (multiTimeframeData.isEmpty()) {
                throw new RuntimeException("无法获取 " + symbol + " 的多时间框架数据");
            }

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
            Map<String, List<Candlestick>> allSymbolsData = new HashMap<>();
            allSymbolsData.put(symbol, multiTimeframeData.get("1h")); // 使用1小时数据作为基础
            correlationArbitrageModel.detect(symbol, multiTimeframeData, allSymbolsData).ifPresent(detectedSignals::add);

            // 创建信号融合引擎
            SignalFusionEngine fusionEngine = new SignalFusionEngine();

            // 识别市场状态
            SmartDataEngine.MarketStateDetector detector = new SmartDataEngine.MarketStateDetector();
            SmartDataEngine.MarketStateDetector.MarketState marketState = detector.detectMarketState(symbol, multiTimeframeData);

            // 将内部市场状态转换为DataEngine的MarketRegime
            DataEngine.MarketRegime regime = smartDataEngine.mapToMarketRegime(marketState);

            // 执行信号融合
            SignalFusionEngine.FusionResult fusionResult = fusionEngine.fuseSignals(detectedSignals, regime, symbol, multiTimeframeData.get("1h"));

            // 创建智能执行优化器
            SmartExecutionOptimizer executionOptimizer = new SmartExecutionOptimizer();

            // 生成执行计划
            SmartExecutionOptimizer.ExecutionPlan executionPlan = executionOptimizer.generateExecutionPlan(
                fusionResult, symbol, multiTimeframeData);

            // 构建响应
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("symbol", symbol);
            response.put("marketState", marketState.name());
            response.put("detectedSignals", detectedSignals.size());
            Map<String, Object> fusionResultMap = new HashMap<>();
            fusionResultMap.put("decision", fusionResult.getFinalDecision().name());
            fusionResultMap.put("aggregatedStrength", fusionResult.getAggregatedStrength());
            fusionResultMap.put("confidence", fusionResult.getConfidence());
            fusionResultMap.put("reasoning", fusionResult.getReasoning());
            response.put("fusionResult", fusionResultMap);
            
            Map<String, Object> executionPlanMap = new HashMap<>();
            executionPlanMap.put("decision", executionPlan.getDecision().name());
            executionPlanMap.put("entryPrice", executionPlan.getEntryPrice());
            executionPlanMap.put("stopLoss", executionPlan.getStopLoss());
            executionPlanMap.put("takeProfit", executionPlan.getTakeProfit());
            executionPlanMap.put("positionSize", executionPlan.getPositionSize());
            executionPlanMap.put("entryStrategy", executionPlan.getEntryStrategy());
            executionPlanMap.put("exitStrategy", executionPlan.getExitStrategy());
            executionPlanMap.put("riskLevel", executionPlan.getRiskLevel());
            executionPlanMap.put("riskRewardRatio", executionPlan.getRiskRewardRatio());
            executionPlanMap.put("executionSteps", executionPlan.getExecutionSteps());
            response.put("executionPlan", executionPlanMap);
            response.put("timestamp", System.currentTimeMillis());

            logger.info("成功生成六维智能合约交易系统信号，交易对: {}, 检测到信号数: {}", symbol, detectedSignals.size());
            return response;

        } catch (Exception e) {
            logger.error("生成六维智能合约交易系统信号时发生错误，交易对: {}", symbol, e);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            response.put("symbol", symbol);
            response.put("timestamp", System.currentTimeMillis());

            return response;
        }
    }

    /**
     * 获取市场状态
     */
    @GetMapping("/market-state/{symbol}")
    public Map<String, Object> getMarketState(@PathVariable String symbol) {
        try {
            logger.info("获取市场状态，交易对: {}", symbol);

            // 获取多时间框架数据
            Map<String, List<Candlestick>> multiTimeframeData = smartDataEngine.getMultiTimeframeData(symbol);

            if (multiTimeframeData.isEmpty()) {
                throw new RuntimeException("无法获取 " + symbol + " 的多时间框架数据");
            }

            // 识别市场状态
            SmartDataEngine.MarketStateDetector detector = new SmartDataEngine.MarketStateDetector();
            SmartDataEngine.MarketStateDetector.MarketState marketState = detector.detectMarketState(symbol, multiTimeframeData);

            // 计算衍生数据
            Map<String, Object> derivedData = smartDataEngine.calculateDerivedData(symbol, multiTimeframeData);

            // 构建响应
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("symbol", symbol);
            response.put("marketState", marketState.name());
            response.put("derivedData", derivedData);
            response.put("timestamp", System.currentTimeMillis());

            logger.info("成功获取市场状态，交易对: {}, 状态: {}", symbol, marketState.name());
            return response;

        } catch (Exception e) {
            logger.error("获取市场状态时发生错误，交易对: {}", symbol, e);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            response.put("symbol", symbol);
            response.put("timestamp", System.currentTimeMillis());

            return response;
        }
    }

    /**
     * 测试单个模型
     */
    @PostMapping("/test-model/{modelName}")
    public Map<String, Object> testModel(@PathVariable String modelName, @RequestParam String symbol) {
        try {
            logger.info("测试模型: {}, 交易对: {}", modelName, symbol);

            // 获取多时间框架数据
            Map<String, List<Candlestick>> multiTimeframeData = smartDataEngine.getMultiTimeframeData(symbol);

            if (multiTimeframeData.isEmpty()) {
                throw new RuntimeException("无法获取 " + symbol + " 的多时间框架数据");
            }

            SignalFusionEngine.TradingSignal signal = null;

            switch (modelName.toLowerCase()) {
                case "trendmomentum":
                    TrendMomentumResonanceModel trendMomentumModel = new TrendMomentumResonanceModel();
                    signal = trendMomentumModel.detect(symbol, multiTimeframeData).orElse(null);
                    break;
                case "institutionalflow":
                    InstitutionalFlowModel institutionalFlowModel = new InstitutionalFlowModel();
                    signal = institutionalFlowModel.detect(symbol, multiTimeframeData).orElse(null);
                    break;
                case "volatilitybreakout":
                    VolatilityBreakoutModel volatilityBreakoutModel = new VolatilityBreakoutModel();
                    signal = volatilityBreakoutModel.detect(symbol, multiTimeframeData).orElse(null);
                    break;
                case "keylevelbattlefield":
                    KeyLevelBattlefieldModel keyLevelBattlefieldModel = new KeyLevelBattlefieldModel();
                    signal = keyLevelBattlefieldModel.detect(symbol, multiTimeframeData).orElse(null);
                    break;
                case "sentimentextremes":
                    SentimentExtremesModel sentimentExtremesModel = new SentimentExtremesModel();
                    signal = sentimentExtremesModel.detect(symbol, multiTimeframeData).orElse(null);
                    break;
                case "correlationarbitrage":
                    CorrelationArbitrageModel correlationArbitrageModel = new CorrelationArbitrageModel();
                    Map<String, List<Candlestick>> allSymbolsData = new HashMap<>();
                    allSymbolsData.put(symbol, multiTimeframeData.get("1h"));
                    signal = correlationArbitrageModel.detect(symbol, multiTimeframeData, allSymbolsData).orElse(null);
                    break;
                default:
                    throw new IllegalArgumentException("未知的模型名称: " + modelName);
            }

            // 构建响应
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("symbol", symbol);
            response.put("modelName", modelName);
            response.put("signalDetected", signal != null);
            if (signal != null) {
                Map<String, Object> signalMap = new HashMap<>();
                signalMap.put("model", signal.getModel().name());
                signalMap.put("direction", signal.getDirection().name());
                signalMap.put("strength", signal.getStrength());
                signalMap.put("reason", signal.getReason());
                signalMap.put("confidence", signal.getConfidence());
                signalMap.put("metadata", signal.getMetadata());
                response.put("signal", signalMap);
            }
            response.put("timestamp", System.currentTimeMillis());

            logger.info("成功测试模型: {}, 交易对: {}, 信号检测: {}", modelName, symbol, signal != null);
            return response;

        } catch (Exception e) {
            logger.error("测试模型时发生错误，模型: {}, 交易对: {}", modelName, symbol, e);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            response.put("modelName", modelName);
            response.put("symbol", symbol);
            response.put("timestamp", System.currentTimeMillis());

            return response;
        }
    }
}