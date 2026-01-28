package com.damien.tradewise.common.scheduler;

import com.damien.tradewise.admin.entity.TwTraderConfig;
import com.damien.tradewise.admin.mapper.TwTraderConfigMapper;
import com.damien.tradewise.common.service.OrderMonitorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OrderMonitorScheduler {
    
    private static final Logger logger = LoggerFactory.getLogger(OrderMonitorScheduler.class);
    
    @Autowired
    private TwTraderConfigMapper traderConfigMapper;
    
    @Autowired
    private OrderMonitorService orderMonitorService;
    
    /**
     * 每分钟执行一次订单监控
     */
    @Scheduled(cron = "0 * * * * ?")
    public void monitorOrders() {
        logger.info("========== 开始执行订单监控任务 ==========");
        
        try {
            // 获取所有启用的交易员
            List<TwTraderConfig> traders = traderConfigMapper.selectEnabledTraders();
            
            if (traders.isEmpty()) {
                logger.info("没有启用的交易员，跳过监控");
                return;
            }
            
            logger.info("共有 {} 个启用的交易员需要监控", traders.size());
            
            // 遍历监控每个交易员
            for (TwTraderConfig trader : traders) {
                try {
                    orderMonitorService.monitorTraderOrders(trader);
                } catch (Exception e) {
                    logger.error("监控交易员失败: traderId={}, error={}", trader.getId(), e.getMessage());
                }
            }
            
            logger.info("========== 订单监控任务执行完成 ==========");
            
        } catch (Exception e) {
            logger.error("订单监控任务执行失败", e);
        }
    }
}
