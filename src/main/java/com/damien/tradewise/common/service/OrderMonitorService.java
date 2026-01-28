package com.damien.tradewise.common.service;

import com.damien.tradewise.admin.mapper.TwTraderConfigMapper;
import com.damien.tradewise.admin.entity.TwTraderConfig;
import com.damien.tradewise.common.entity.TwTraderOrder;
import com.damien.tradewise.common.mapper.TwTraderOrderMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
public class OrderMonitorService {
    
    private static final Logger logger = LoggerFactory.getLogger(OrderMonitorService.class);
    
    @Autowired
    private TwTraderConfigMapper traderConfigMapper;
    
    @Autowired
    private TwTraderOrderMapper orderMapper;
    
    @Autowired
    private BinanceApiService binanceApiService;
    
    @Autowired
    private OrderEmailNotificationService emailNotificationService;
    
    /**
     * 监控单个交易员的订单
     */
    @Transactional
    public void monitorTraderOrders(TwTraderConfig trader) {
        logger.info("开始监控交易员: id={}, name={}, portfolioId={}", 
            trader.getId(), trader.getTraderName(), trader.getPortfolioId());
        
        try {
            // 1. 调用币安API获取订单
            List<JsonNode> apiOrders = binanceApiService.getTraderOrders(trader.getPortfolioId());
            
            if (apiOrders.isEmpty()) {
                logger.info("交易员 {} 暂无订单数据", trader.getTraderName());
                return;
            }
            
            int newOrderCount = 0;
            LocalDateTime latestOrderTime = null;
            
            // 2. 遍历订单，检测新订单
            for (JsonNode orderNode : apiOrders) {
                // 使用orderTime作为订单ID（币安返回的字段名）
                String orderId = String.valueOf(orderNode.get("orderTime").asLong());
                
                // 检查订单是否已存在
                if (orderMapper.existsByExchangeAndOrderId("BINANCE", orderId) > 0) {
                    continue;
                }
                
                // 3. 保存新订单
                TwTraderOrder order = parseOrderFromJson(orderNode, trader.getId());
                orderMapper.insert(order);
                newOrderCount++;
                
                // 4. 发送邮件通知
                emailNotificationService.sendNewOrderNotification(order, trader.getTraderName());
                
                if (latestOrderTime == null || order.getOrderTime().isAfter(latestOrderTime)) {
                    latestOrderTime = order.getOrderTime();
                }
                
                logger.info("发现新订单: traderId={}, orderId={}, symbol={}, side={}", 
                    trader.getId(), orderId, order.getSymbol(), order.getSide());
            }
            
            // 4. 更新交易员统计信息
            if (newOrderCount > 0) {
                traderConfigMapper.updateOrderStatistics(trader.getId(), newOrderCount, latestOrderTime);
                logger.info("交易员 {} 新增 {} 条订单", trader.getTraderName(), newOrderCount);
            }
            
            // 5. 更新最后检查时间
            traderConfigMapper.updateLastCheckTime(trader.getId(), LocalDateTime.now());
            
        } catch (Exception e) {
            logger.error("监控交易员订单失败: traderId={}, error={}", trader.getId(), e.getMessage(), e);
        }
    }
    
    /**
     * 从JSON解析订单对象
     */
    private TwTraderOrder parseOrderFromJson(JsonNode node, Long traderId) {
        TwTraderOrder order = new TwTraderOrder();
        order.setTraderId(traderId);
        order.setExchange("BINANCE");
        
        // 使用orderTime作为订单ID
        order.setOrderId(String.valueOf(node.get("orderTime").asLong()));
        order.setSymbol(node.get("symbol").asText());
        order.setSide(node.get("side").asText());
        order.setPositionSide(node.has("positionSide") ? node.get("positionSide").asText() : null);
        
        // 价格和数量
        if (node.has("avgPrice")) {
            order.setAvgPrice(new BigDecimal(node.get("avgPrice").asText()));
        }
        if (node.has("executedQty")) {
            BigDecimal executedQty = new BigDecimal(node.get("executedQty").asText());
            order.setExecutedQty(executedQty);
            
            // 总价值 = 均价 × 成交数量
            if (order.getAvgPrice() != null) {
                order.setTotalValue(order.getAvgPrice().multiply(executedQty));
            }
        }
        
        // 已实现盈亏（仅平仓订单有）
        if (node.has("totalPnl")) {
            order.setRealizedPnl(new BigDecimal(node.get("totalPnl").asText()));
        }
        
        // 计算操作类型：开多/开空/平多/平空
        String actionType = calculateActionType(order.getSide(), order.getPositionSide());
        order.setActionType(actionType);
        
        // 时间转换（币安返回毫秒时间戳）
        long timeMillis = node.get("orderTime").asLong();
        order.setOrderTime(LocalDateTime.ofInstant(Instant.ofEpochMilli(timeMillis), ZoneId.systemDefault()));
        
        if (node.has("orderUpdateTime")) {
            long updateTimeMillis = node.get("orderUpdateTime").asLong();
            order.setUpdateTime(LocalDateTime.ofInstant(Instant.ofEpochMilli(updateTimeMillis), ZoneId.systemDefault()));
        }
        
        return order;
    }
    
    /**
     * 计算操作类型
     */
    private String calculateActionType(String side, String positionSide) {
        if ("BUY".equals(side) && "LONG".equals(positionSide)) {
            return "开多";
        } else if ("SELL".equals(side) && "SHORT".equals(positionSide)) {
            return "开空";
        } else if ("SELL".equals(side) && "LONG".equals(positionSide)) {
            return "平多";
        } else if ("BUY".equals(side) && "SHORT".equals(positionSide)) {
            return "平空";
        }
        return "未知";
    }
}
