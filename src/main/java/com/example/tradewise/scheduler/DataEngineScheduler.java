package com.example.tradewise.scheduler;

import com.example.tradewise.service.DataEngine;
import com.example.tradewise.config.TradeWiseProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 数据引擎调度器 - 每分钟运行一次数据引擎
 */
@Component
@Configuration
@EnableScheduling
public class DataEngineScheduler {

    private static final Logger logger = LoggerFactory.getLogger(DataEngineScheduler.class);

    @Autowired
    private DataEngine dataEngine;

    @Autowired
    private TradeWiseProperties tradeWiseProperties;

    /**
     * 每分钟执行一次数据引擎主逻辑
     * 这个调度器实现了Gemini提出的模块化交易辅助引擎
     */
    @Scheduled(cron = "0 * * * * ?") // 每分钟执行一次
    public void runDataEngine() {
        // 检查数据引擎功能是否启用
        if (!tradeWiseProperties.getMarketAnalysis().isDataEngineEnabled()) {
            logger.debug("数据引擎功能已禁用，跳过本次执行");
            return;
        }

        logger.info("开始执行数据引擎主逻辑...");

        try {
            // 执行数据引擎主逻辑
            dataEngine.execute();

            // 清理过期缓存
            dataEngine.cleanupExpiredCache();

            logger.info("完成数据引擎主逻辑执行");
        } catch (Exception e) {
            logger.error("执行数据引擎主逻辑时发生错误", e);
        }
    }
}