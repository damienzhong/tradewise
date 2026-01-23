package com.example.tradewise.scheduler;

import com.example.tradewise.service.MarketAnalysisService;
import com.example.tradewise.config.TradeWiseProperties;
import com.example.tradewise.service.SignalStateManager;
import com.example.tradewise.service.AdaptiveParameterSystem;
import com.example.tradewise.service.SignalFilterService;
import com.example.tradewise.service.DailySummaryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
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

    /**
     * 每15分钟执行一次标准市场分析，生成交易信号
     * 这个调度器会定期分析市场数据并生成交易信号
     */
    @Scheduled(cron = "0 */15 * * * ?") // 每15分钟执行一次
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
        // 从配置中获取要监控的交易对
        String[] symbolsToMonitor = tradeWiseProperties.getMarketAnalysis().getSymbolsToMonitor();

        // 收集所有交易对的信号
        List<MarketAnalysisService.TradingSignal> allSignals = new ArrayList<>();

        // 遍历所有要监控的交易对
        for (String symbol : symbolsToMonitor) {
            // 获取该交易对的信号，使用增强评分系统
            List<MarketAnalysisService.TradingSignal> signals = marketAnalysisService.
                    generateSignalsForSymbol(symbol, true); // 启用增强评分

            // 添加到总信号列表
            if (signals != null && !signals.isEmpty()) {
                allSignals.addAll(signals);
                logger.info("为交易对 {} 生成了 {} 个交易信号（{}分析）",
                        symbol, signals.size(), analysisType);
            }
        }

        // 使用新的信号过滤服务，只发送高质量信号
        List<MarketAnalysisService.TradingSignal> filteredSignals =
                signalFilterService.filterSignalsForImmediateSend(allSignals);

        // 如果有高质量信号，立即发送邮件通知
        if (!filteredSignals.isEmpty()) {
            marketAnalysisService.sendCombinedMarketSignalNotification(filteredSignals);

            // 记录信号到绩效跟踪器（通过MarketAnalysisService传递）
            marketAnalysisService.recordSignalPerformance(filteredSignals);
        }

        logger.info("完成{}市场行情分析任务，共分析了 {} 个交易对，生成 {} 个信号，发送 {} 个高质量信号",
                analysisType, symbolsToMonitor.length, allSignals.size(), filteredSignals.size());
    }
}