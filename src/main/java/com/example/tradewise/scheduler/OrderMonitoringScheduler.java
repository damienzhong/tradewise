package com.example.tradewise.scheduler;

import com.example.tradewise.config.TradeWiseProperties;
import com.example.tradewise.entity.TraderConfig;
import com.example.tradewise.service.OrderService;
import com.example.tradewise.service.TraderConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class OrderMonitoringScheduler {
    
    private static final Logger logger = LoggerFactory.getLogger(OrderMonitoringScheduler.class);

    @Autowired
    private OrderService orderService;
    
    @Autowired
    private TradeWiseProperties
            tradeWiseProperties;
    
    @Autowired
    private TraderConfigService traderConfigService;    
    /**
     * 每1分钟执行一次订单监控，降低API调用频率以避免速率限制
     * 使用cron表达式: 秒 分 时 日 月 周
     */
    @Scheduled(cron = "0 */1 * * * ?")
    public void monitorOrders() {
        // 检查交易员跟单功能是否启用
        if (!tradeWiseProperties.getCopyTrading().isEnabled()) {
            logger.debug("交易员跟单功能已禁用，跳过本次监控");
            return;
        }
        
        logger.info("Starting scheduled order monitoring task...");
        
        try {
            // 从数据库获取所有启用的交易员配置
            List<TraderConfig> traders = traderConfigService.getAllEnabledTraderConfigs();
            if (traders == null || traders.isEmpty()) {
                logger.warn("No traders configured for monitoring in database");
                return;
            }
            
            // 异步处理所有交易员的订单
            orderService.fetchOrdersForAllTraders(traders).join();
            
            logger.info("Completed scheduled order monitoring task for {} traders", traders.size());
        } catch (Exception e) {
            logger.error("Error during scheduled order monitoring", e);
        }
    }
    
    /**
     * 每天凌晨清理过期的日志或临时数据（可选）
     */
    @Scheduled(cron = "0 0 2 * * ?") // 每天凌晨2点执行
    public void dailyCleanup() {
        logger.info("Running daily cleanup task...");
        // 可以在这里添加清理过期数据的逻辑
    }
}