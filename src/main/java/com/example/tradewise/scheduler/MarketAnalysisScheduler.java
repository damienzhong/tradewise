package com.example.tradewise.scheduler;

import com.example.tradewise.service.MarketAnalysisService;
import com.example.tradewise.service.MarketAnalysisService.Candlestick;
import com.example.tradewise.service.MarketAnalysisService.TradingSignal;
import com.example.tradewise.config.TradeWiseProperties;
import com.example.tradewise.service.SignalStateManager;
import com.example.tradewise.service.AdaptiveParameterSystem;
import com.example.tradewise.service.SignalFilterService;
import com.example.tradewise.service.DailySummaryService;
import com.example.tradewise.service.HighQualitySignalEnhancer;
import com.example.tradewise.service.MarketDataService;
import com.example.tradewise.service.SignalMonitorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Configuration
@EnableScheduling
public class MarketAnalysisScheduler {

    private static final Logger logger = LoggerFactory.getLogger(MarketAnalysisScheduler.class);

    @Autowired
    private MarketAnalysisService marketAnalysisService;

    @Autowired
    private TradeWiseProperties tradeWiseProperties;

    @Autowired
    private SignalStateManager signalStateManager; // 注入信号状态管理器

    @Autowired
    private AdaptiveParameterSystem adaptiveParameterSystem; // 注入自适应参数系统

    @Autowired
    private SignalFilterService signalFilterService; // 注入信号过滤服务

    @Autowired
    private DailySummaryService dailySummaryService; // 注入每日摘要服务

    @Autowired
    private HighQualitySignalEnhancer highQualitySignalEnhancer; // 注入高质量信号增强器

    @Autowired
    private MarketDataService marketDataService; // 注入市场数据服务

    @Autowired
    private com.example.tradewise.service.SymbolConfigService symbolConfigService; // 注入币对配置服务

    @Autowired
    private com.example.tradewise.service.SystemAlertService systemAlertService; // 注入系统告警服务

    @Autowired
    private com.example.tradewise.service.SignalPersistenceService signalPersistenceService;

    @Autowired
    private SignalMonitorService signalMonitorService;

    /**
     * 每15分钟执行一次标准市场分析，生成交易信号
     * 这个调度器会定期分析市场数据并生成交易信号
     */
    @Scheduled(cron = "0 * * * * ?") // 每1分钟执行一次
    public void analyzeMarket() {
        // 检查市场分析功能是否启用
        if (!tradeWiseProperties.getMarketAnalysis().isEnabled()) {
            logger.debug("市场分析功能已禁用，跳过本次分析");
            return;
        }

        logger.info("开始执行标准市场行情分析任务...");

        try {
            performMarketAnalysis("标准");

            logger.info("完成标准市场行情分析任务");
        } catch (Exception e) {
            logger.error("执行标准市场行情分析任务时发生错误", e);
            
            // 记录系统异常
            systemAlertService.recordSystemException("MarketAnalysisScheduler", 
                "标准市场分析任务失败", e);
        }
    }

    /**
     * 每5分钟执行一次快速市场分析，重点关注短期机会
     */
    @Scheduled(cron = "0 */5 * * * ?") // 每5分钟执行一次
    public void analyzeMarketFast() {
        // 检查快速分析功能是否启用
        if (!tradeWiseProperties.getMarketAnalysis().isFastAnalysisEnabled()) {
            logger.debug("快速分析功能已禁用，跳过本次分析");
            return;
        }

        logger.info("开始执行快速市场行情分析任务...");

        try {
            performMarketAnalysis("快速");

            logger.info("完成快速市场行情分析任务");
        } catch (Exception e) {
            logger.error("执行快速市场行情分析任务时发生错误", e);
            
            // 记录系统异常
            systemAlertService.recordSystemException("MarketAnalysisScheduler", 
                "快速市场分析任务失败", e);
        }
    }

    /**
     * 每小时执行一次深度技术分析
     */
    @Scheduled(cron = "0 0 * * * ?") // 每小时执行一次
    public void deepTechnicalAnalysis() {
        // 检查深度分析功能是否启用
        if (!tradeWiseProperties.getMarketAnalysis().isEnabled() ||
                !tradeWiseProperties.getMarketAnalysis().isDeepAnalysisEnabled()) {
            logger.debug("深度分析功能已禁用，跳过本次分析");
            return;
        }

        logger.info("开始执行深度技术分析任务...");

        try {
            // 可以在这里实现更复杂的技术分析算法
            // 例如多时间框架分析、机器学习模型预测等

            logger.info("完成深度技术分析任务");
        } catch (Exception e) {
            logger.error("执行深度技术分析任务时发生错误", e);
        }
    }

    /**
     * 每30分钟清理一次过期的信号状态
     */
    @Scheduled(cron = "0 */30 * * * ?") // 每30分钟执行一次
    public void cleanupExpiredSignalStates() {
        logger.debug("开始清理过期的信号状态...");
        signalStateManager.cleanupExpiredSignals();
        logger.debug("完成信号状态清理");
    }

    /**
     * 每日执行参数优化
     */
    @Scheduled(cron = "0 0 2 * * ?") // 每天凌晨2点执行参数优化
    public void optimizeParameters() {
        logger.info("开始执行参数优化...");
        adaptiveParameterSystem.optimizeParameters();
        logger.info("完成参数优化");
    }

    /**
     * 每天晚上8点发送每日信号摘要
     */
    @Scheduled(cron = "0 0 20 * * ?") // 每天晚上8点执行
    public void sendDailySummary() {
        logger.info("开始发送每日信号摘要...");
        try {
            dailySummaryService.sendDailySummary();
            logger.info("每日信号摘要发送完成");
        } catch (Exception e) {
            logger.error("发送每日信号摘要时发生错误", e);
        }
    }

    /**
     * 执行市场分析的核心方法
     *
     * @param analysisType 分析类型（标准/快速）
     */
    private void performMarketAnalysis(String analysisType) {
        // 从数据库获取启用的币对列表
        List<String> symbolsToMonitor = symbolConfigService.getEnabledSymbolNames();
        
        if (symbolsToMonitor.isEmpty()) {
            logger.warn("没有启用的币对，跳过市场分析");
            return;
        }

        logger.info("开始分析 {} 个币对: {}", symbolsToMonitor.size(), symbolsToMonitor);
        
        // 1. 收集多时间框架数据
        java.util.Map<String, List<Candlestick>> candlesticksMap = new java.util.HashMap<>();
        java.util.Map<String, java.util.Map<String, List<Candlestick>>> multiTimeframeDataMap = new java.util.HashMap<>();

        for (String symbol : symbolsToMonitor) {
            try {
                // 获取1小时数据（主要分析周期）
                List<Candlestick> hourlyData = marketDataService.getKlines(symbol, "1h", 200);
                candlesticksMap.put(symbol, hourlyData);

                // 获取多时间框架数据用于确认
                java.util.Map<String, List<Candlestick>> mtfData = new java.util.HashMap<>();
                mtfData.put("4h", marketDataService.getKlines(symbol, "4h", 100));
                mtfData.put("1d", marketDataService.getKlines(symbol, "1d", 30));
                multiTimeframeDataMap.put(symbol, mtfData);
            } catch (Exception e) {
                logger.error("获取 {} 的市场数据失败: {}", symbol, e.getMessage());
            }
        }

        // 2. 生成原始信号
        List<TradingSignal> allSignals = new ArrayList<>();
        for (String symbol : symbolsToMonitor) {
            List<TradingSignal> signals = marketAnalysisService.generateSignalsForSymbol(symbol, true);
            if (signals != null && !signals.isEmpty()) {
                allSignals.addAll(signals);
                logger.debug("记录原始信号: {} - {}个", symbol, signals.size());
                signalMonitorService.recordRawSignals(symbol, signals.size());
            }
        }

        logger.info("生成原始信号: {}个", allSignals.size());

        // 3. 使用高质量信号增强器过滤和增强信号
        List<TradingSignal> enhancedSignals = new ArrayList<>();
        for (TradingSignal signal : allSignals) {
            List<Candlestick> candlesticks = candlesticksMap.get(signal.getSymbol());
            java.util.Map<String, List<Candlestick>> mtfData = multiTimeframeDataMap.get(signal.getSymbol());

            if (candlesticks != null && !candlesticks.isEmpty()) {
                TradingSignal enhanced = highQualitySignalEnhancer.enhanceSignal(signal, candlesticks, mtfData);
                if (enhanced != null && enhanced.getScore() >= 6) {
                    enhancedSignals.add(enhanced);
                    signalMonitorService.recordEnhancedSignals(enhanced.getSymbol(), 1, enhanced.getScore(), enhanced.getSignalLevel());
                } else if (enhanced != null) {
                    // 记录所有评分的信号，包括<6分的
                    signalMonitorService.recordEnhancedSignals(enhanced.getSymbol(), 1, enhanced.getScore(), enhanced.getSignalLevel());
                }
            }
        }

        logger.info("高质量信号: {}个（过滤率: {:.1f}%）",
                enhancedSignals.size(),
                allSignals.isEmpty() ? 0 : (1 - (double)enhancedSignals.size() / allSignals.size()) * 100);

        // 4. 应用信号过滤器（每日限额和冷却）
        List<TradingSignal> filteredSignals = signalFilterService.filterSignalsForImmediateSend(enhancedSignals);
        for (TradingSignal signal : filteredSignals) {
            signalMonitorService.recordFilteredSignals(signal.getSymbol(), 1);
        }
        
        logger.info("信号级别分布: LEVEL_1={}, LEVEL_2={}, LEVEL_3={}",
            enhancedSignals.stream().filter(s -> "LEVEL_1".equals(s.getSignalLevel())).count(),
            enhancedSignals.stream().filter(s -> "LEVEL_2".equals(s.getSignalLevel())).count(),
            enhancedSignals.stream().filter(s -> "LEVEL_3".equals(s.getSignalLevel())).count());

        // 5. 持久化所有增强信号到数据库
        if (!enhancedSignals.isEmpty()) {
            signalPersistenceService.saveSignals(enhancedSignals);
        }

        // 6. 发送高质量信号
        if (!filteredSignals.isEmpty()) {
            marketAnalysisService.sendCombinedMarketSignalNotification(filteredSignals);
            marketAnalysisService.recordSignalPerformance(filteredSignals);
            for (TradingSignal signal : filteredSignals) {
                String signalInfo = String.format("%s %s @ %.2f (Score: %d)",
                    signal.getSignalType(), signal.getIndicator(), signal.getPrice(), signal.getScore());
                signalMonitorService.recordSentSignal(signal.getSymbol(), signalInfo);
            }
        }

        logger.info("完成{}市场行情分析: 原始{}个 → 高质量{}个 → 发送{}个",
                analysisType, allSignals.size(), enhancedSignals.size(), filteredSignals.size());
        
        // 输出当前统计
        logger.debug("当前统计: {}", signalMonitorService.getSummary());
    }
}
