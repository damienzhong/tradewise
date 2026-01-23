package com.example.tradewise.service;

import com.example.tradewise.entity.Order;
import com.example.tradewise.mapper.OrderMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 交易员表现分析服务
 * 用于分析交易员的历史表现和绩效指标
 */
@Service
public class TraderPerformanceService {

    private static final Logger logger = LoggerFactory.getLogger(TraderPerformanceService.class);

    @Autowired
    private OrderMapper orderMapper;

    /**
     * 交易员绩效统计信息类
     */
    public static class TraderPerformanceStats {
        private String traderId;
        private String traderName;
        private int totalOrders;
        private int profitableOrders;
        private int unprofitableOrders;
        private BigDecimal totalProfit;
        private BigDecimal totalLoss;
        private BigDecimal netProfit;
        private BigDecimal profitFactor; // 盈利因子
        private BigDecimal winRate; // 胜率
        private BigDecimal avgProfitPerWinningTrade;
        private BigDecimal avgLossPerLosingTrade;
        private BigDecimal maxDrawdown;
        private LocalDateTime firstOrderTime;
        private LocalDateTime lastOrderTime;
        private String riskLevel; // 风险等级

        // 构造函数
        public TraderPerformanceStats(String traderId, String traderName) {
            this.traderId = traderId;
            this.traderName = traderName;
            this.totalProfit = BigDecimal.ZERO;
            this.totalLoss = BigDecimal.ZERO;
            this.netProfit = BigDecimal.ZERO;
            this.profitFactor = BigDecimal.ZERO;
            this.winRate = BigDecimal.ZERO;
            this.avgProfitPerWinningTrade = BigDecimal.ZERO;
            this.avgLossPerLosingTrade = BigDecimal.ZERO;
            this.maxDrawdown = BigDecimal.ZERO;
        }

        // Getters and Setters
        public String getTraderId() { return traderId; }
        public void setTraderId(String traderId) { this.traderId = traderId; }

        public String getTraderName() { return traderName; }
        public void setTraderName(String traderName) { this.traderName = traderName; }

        public int getTotalOrders() { return totalOrders; }
        public void setTotalOrders(int totalOrders) { this.totalOrders = totalOrders; }

        public int getProfitableOrders() { return profitableOrders; }
        public void setProfitableOrders(int profitableOrders) { this.profitableOrders = profitableOrders; }

        public int getUnprofitableOrders() { return unprofitableOrders; }
        public void setUnprofitableOrders(int unprofitableOrders) { this.unprofitableOrders = unprofitableOrders; }

        public BigDecimal getTotalProfit() { return totalProfit; }
        public void setTotalProfit(BigDecimal totalProfit) { this.totalProfit = totalProfit; }

        public BigDecimal getTotalLoss() { return totalLoss; }
        public void setTotalLoss(BigDecimal totalLoss) { this.totalLoss = totalLoss.abs().negate(); } // 总亏损通常表示为负数

        public BigDecimal getNetProfit() { return netProfit; }
        public void setNetProfit(BigDecimal netProfit) { this.netProfit = netProfit; }

        public BigDecimal getProfitFactor() { return profitFactor; }
        public void setProfitFactor(BigDecimal profitFactor) { this.profitFactor = profitFactor; }

        public BigDecimal getWinRate() { return winRate; }
        public void setWinRate(BigDecimal winRate) { this.winRate = winRate; }

        public BigDecimal getAvgProfitPerWinningTrade() { return avgProfitPerWinningTrade; }
        public void setAvgProfitPerWinningTrade(BigDecimal avgProfitPerWinningTrade) { this.avgProfitPerWinningTrade = avgProfitPerWinningTrade; }

        public BigDecimal getAvgLossPerLosingTrade() { return avgLossPerLosingTrade; }
        public void setAvgLossPerLosingTrade(BigDecimal avgLossPerLosingTrade) { this.avgLossPerLosingTrade = avgLossPerLosingTrade.abs().negate(); }

        public BigDecimal getMaxDrawdown() { return maxDrawdown; }
        public void setMaxDrawdown(BigDecimal maxDrawdown) { this.maxDrawdown = maxDrawdown.abs().negate(); }

        public LocalDateTime getFirstOrderTime() { return firstOrderTime; }
        public void setFirstOrderTime(LocalDateTime firstOrderTime) { this.firstOrderTime = firstOrderTime; }

        public LocalDateTime getLastOrderTime() { return lastOrderTime; }
        public void setLastOrderTime(LocalDateTime lastOrderTime) { this.lastOrderTime = lastOrderTime; }

        public String getRiskLevel() { return riskLevel; }
        public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
    }

    /**
     * 获取指定交易员的表现统计
     *
     * @param traderId 交易员ID
     * @return 交易员表现统计信息
     */
    public TraderPerformanceStats getTraderPerformanceStats(String traderId) {
        logger.debug("获取交易员 {} 的表现统计", traderId);

        // 从数据库获取该交易员的所有订单
        List<Order> orders = orderMapper.findByTraderId(traderId);

        if (orders.isEmpty()) {
            logger.info("交易员 {} 没有任何订单记录", traderId);
            return createEmptyStats(traderId, "未知交易员");
        }

        // 按时间排序
        orders.sort(Comparator.comparingLong(Order::getOrderTime));

        // 初始化统计信息
        TraderPerformanceStats stats = new TraderPerformanceStats(traderId, orders.get(0).getTraderName());

        // 计算统计信息
        calculatePerformanceStats(stats, orders);

        // 计算风险等级
        stats.setRiskLevel(calculateRiskLevel(stats));

        logger.debug("交易员 {} 表现统计完成: 总订单={}, 盈利订单={}, 净利润={}", 
            traderId, stats.getTotalOrders(), stats.getProfitableOrders(), stats.getNetProfit());

        return stats;
    }

    /**
     * 获取所有交易员的表现统计
     */
    public List<TraderPerformanceStats> getAllTradersPerformanceStats() {
        logger.debug("获取所有交易员的表现统计");

        // 获取所有订单
        List<Order> allOrders = orderMapper.findAll();

        // 按交易员分组
        Map<String, List<Order>> ordersByTrader = allOrders.stream()
            .collect(Collectors.groupingBy(Order::getTraderId));

        List<TraderPerformanceStats> allStats = new ArrayList<>();

        for (Map.Entry<String, List<Order>> entry : ordersByTrader.entrySet()) {
            String traderId = entry.getKey();
            List<Order> orders = entry.getValue();

            if (!orders.isEmpty()) {
                // 初始化统计信息
                TraderPerformanceStats stats = new TraderPerformanceStats(traderId, orders.get(0).getTraderName());

                // 计算统计信息
                calculatePerformanceStats(stats, orders);

                // 计算风险等级
                stats.setRiskLevel(calculateRiskLevel(stats));

                allStats.add(stats);
            }
        }

        // 按净收益排序（降序）
        allStats.sort((s1, s2) -> s2.getNetProfit().compareTo(s1.getNetProfit()));

        logger.debug("总共分析了 {} 个交易员的表现", allStats.size());

        return allStats;
    }

    /**
     * 计算交易员表现统计信息
     */
    private void calculatePerformanceStats(TraderPerformanceStats stats, List<Order> orders) {
        stats.setTotalOrders(orders.size());

        if (orders.isEmpty()) {
            return;
        }

        // 设置第一个和最后一个订单的时间
        stats.setFirstOrderTime(new Date(orders.get(0).getOrderTime()).toInstant()
            .atZone(ZoneId.systemDefault()).toLocalDateTime());
        stats.setLastOrderTime(new Date(orders.get(orders.size() - 1).getOrderTime()).toInstant()
            .atZone(ZoneId.systemDefault()).toLocalDateTime());

        // 计算盈利和亏损统计
        int profitableOrders = 0;
        int unprofitableOrders = 0;
        BigDecimal totalProfit = BigDecimal.ZERO;
        BigDecimal totalLoss = BigDecimal.ZERO;
        BigDecimal positiveProfitsSum = BigDecimal.ZERO;
        BigDecimal negativeLossesSum = BigDecimal.ZERO;

        for (Order order : orders) {
            BigDecimal pnl = order.getTotalPnl();

            if (pnl.compareTo(BigDecimal.ZERO) > 0) {
                profitableOrders++;
                totalProfit = totalProfit.add(pnl);
                positiveProfitsSum = positiveProfitsSum.add(pnl);
            } else if (pnl.compareTo(BigDecimal.ZERO) < 0) {
                unprofitableOrders++;
                totalLoss = totalLoss.add(pnl.abs());
                negativeLossesSum = negativeLossesSum.add(pnl.abs());
            }
        }

        stats.setProfitableOrders(profitableOrders);
        stats.setUnprofitableOrders(unprofitableOrders);
        stats.setTotalProfit(totalProfit);
        stats.setTotalLoss(totalLoss.negate()); // 总亏损通常表示为负数
        stats.setNetProfit(totalProfit.subtract(totalLoss));

        // 计算胜率
        if (stats.getTotalOrders() > 0) {
            stats.setWinRate(BigDecimal.valueOf(profitableOrders)
                .divide(BigDecimal.valueOf(stats.getTotalOrders()), 4, BigDecimal.ROUND_HALF_UP)
                .multiply(BigDecimal.valueOf(100))); // 百分比形式
        }

        // 计算平均盈利/亏损
        if (profitableOrders > 0) {
            stats.setAvgProfitPerWinningTrade(positiveProfitsSum.divide(BigDecimal.valueOf(profitableOrders), 8, BigDecimal.ROUND_HALF_UP));
        }
        if (unprofitableOrders > 0) {
            stats.setAvgLossPerLosingTrade(negativeLossesSum.divide(BigDecimal.valueOf(unprofitableOrders), 8, BigDecimal.ROUND_HALF_UP).negate());
        }

        // 计算盈利因子（盈利/亏损的绝对值之比）
        if (totalLoss.compareTo(BigDecimal.ZERO) > 0) {
            stats.setProfitFactor(totalProfit.divide(totalLoss, 4, BigDecimal.ROUND_HALF_UP));
        } else if (totalProfit.compareTo(BigDecimal.ZERO) > 0) {
            stats.setProfitFactor(BigDecimal.valueOf(Double.MAX_VALUE)); // 只有盈利没有亏损
        }

        // 计算最大回撤（这里简化处理，实际应用中需要更复杂的计算）
        // 在这个简单的实现中，我们将最大回撤设定为最大的单笔亏损
        BigDecimal maxSingleLoss = orders.stream()
            .map(Order::getTotalPnl)
            .filter(pnl -> pnl.compareTo(BigDecimal.ZERO) < 0)
            .min(BigDecimal::compareTo)
            .orElse(BigDecimal.ZERO);
        
        stats.setMaxDrawdown(maxSingleLoss);
    }

    /**
     * 根据统计信息计算风险等级
     */
    private String calculateRiskLevel(TraderPerformanceStats stats) {
        // 风险评估逻辑：
        // 1. 胜率高的风险较低
        // 2. 盈利因子大于1.5的相对安全
        // 3. 订单数量少的参考价值低
        // 4. 净利润为正的相对安全

        if (stats.getTotalOrders() < 5) {
            return "UNKNOWN"; // 订单数量太少，无法评估
        }

        if (stats.getNetProfit().compareTo(BigDecimal.ZERO) < 0) {
            return "HIGH"; // 净亏损，高风险
        }

        if (stats.getWinRate().compareTo(BigDecimal.valueOf(40)) < 0) {
            return "HIGH"; // 胜率太低，高风险
        }

        if (stats.getWinRate().compareTo(BigDecimal.valueOf(60)) < 0 && 
            stats.getProfitFactor().compareTo(BigDecimal.valueOf(1.2)) < 0) {
            return "MEDIUM"; // 胜率和盈利因子都不够理想，中等风险
        }

        if (stats.getWinRate().compareTo(BigDecimal.valueOf(50)) >= 0 && 
            stats.getProfitFactor().compareTo(BigDecimal.valueOf(1.5)) >= 0) {
            return "LOW"; // 胜率和盈利因子都较好，低风险
        }

        return "MEDIUM"; // 默认中等风险
    }

    /**
     * 创建空的统计信息
     */
    private TraderPerformanceStats createEmptyStats(String traderId, String traderName) {
        TraderPerformanceStats stats = new TraderPerformanceStats(traderId, traderName);
        stats.setRiskLevel("UNKNOWN");
        return stats;
    }
}