package com.example.tradewise.service;

import com.example.tradewise.service.MarketAnalysisService.TradingSignal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 信号绩效跟踪器
 * 记录每个信号的实际表现，用于优化评分算法
 */
@Component
public class SignalPerformanceTracker {
    
    private static final Logger logger = LoggerFactory.getLogger(SignalPerformanceTracker.class);
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Autowired
    private MarketDataService marketDataService; // 使用依赖注入获取MarketDataService
    
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    
    @PostConstruct
    public void init() {
        // 初始化数据库表（如果不存在）
        createSignalPerformanceTable();
    }
    
    /**
     * 记录信号发出
     */
    public void recordSignal(TradingSignal signal) {
        String sql = "INSERT INTO signal_performance (symbol, signal_time, signal_type, indicator, price, stop_loss, take_profit, score, status, confidence, reason) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try {
            jdbcTemplate.update(sql,
                signal.getSymbol(),
                Timestamp.valueOf(signal.getTimestamp()),
                signal.getSignalType().name(),
                signal.getIndicator(),
                signal.getPrice(),
                signal.getStopLoss(),
                signal.getTakeProfit(),
                signal.getScore(),
                "PENDING",  // 等待后续更新状态
                signal.getConfidence(),
                signal.getReason()
            );
            
            logger.debug("已记录信号: {} {} at {}", signal.getSymbol(), signal.getSignalType(), signal.getPrice());
            
            // 安排4小时后检查信号结果
            scheduleSignalResultCheck(signal.getSymbol(), signal.getTimestamp());
            
        } catch (Exception e) {
            logger.error("记录信号失败: {}", signal.getSymbol(), e);
        }
    }
    
    /**
     * 安排在指定时间后检查信号结果
     */
    private void scheduleSignalResultCheck(String symbol, LocalDateTime signalTime) {
        // 4小时后检查信号结果
        long delay = 4 * 60 * 60; // 4小时（秒）
        
        scheduler.schedule(() -> {
            evaluateSignalResult(symbol, signalTime);
        }, delay, TimeUnit.SECONDS);
    }
    
    /**
     * 评估信号结果（4小时后）
     */
    private void evaluateSignalResult(String symbol, LocalDateTime signalTime) {
        try {
            // 获取信号详情
            String selectSql = "SELECT price, stop_loss, take_profit, signal_type FROM signal_performance WHERE symbol = ? AND signal_time = ?";
            SignalRecord signalRecord = jdbcTemplate.queryForObject(selectSql, 
                (rs, rowNum) -> {
                    return new SignalRecord(
                        rs.getDouble("price"),
                        rs.getDouble("stop_loss"),
                        rs.getDouble("take_profit"),
                        rs.getString("signal_type")
                    );
                }, symbol, Timestamp.valueOf(signalTime));
            
            if (signalRecord == null) {
                logger.warn("未找到信号记录: {} at {}", symbol, signalTime);
                return;
            }
            
            // 获取当前市场价格
            double currentPrice = getCurrentPrice(symbol);
            
            // 判断信号结果
            SignalOutcome outcome = determineSignalOutcome(signalRecord, currentPrice);
            
            // 更新信号结果
            String updateSql = "UPDATE signal_performance SET status = ?, outcome_time = ?, final_price = ?, pnl_percentage = ?, notes = ? WHERE symbol = ? AND signal_time = ?";
            
            double pnlPercentage = calculatePnLPercentage(signalRecord, currentPrice);
            String notes = String.format("4H Result: Current Price=%.4f, SL=%.4f, TP=%.4f", 
                currentPrice, signalRecord.stopLoss, signalRecord.takeProfit);
            
            jdbcTemplate.update(updateSql,
                outcome.getStatus(),
                Timestamp.valueOf(LocalDateTime.now()),
                currentPrice,
                pnlPercentage,
                notes,
                symbol,
                Timestamp.valueOf(signalTime)
            );
            
            logger.info("信号结果更新: {} {} -> {}, PnL: {:.2f}%", 
                symbol, signalRecord.signalType, outcome.getStatus(), pnlPercentage);
                
        } catch (Exception e) {
            logger.error("评估信号结果失败: {} at {}", symbol, signalTime, e);
        }
    }
    
    /**
     * 确定信号结果
     */
    private SignalOutcome determineSignalOutcome(SignalRecord record, double currentPrice) {
        if ("BUY".equals(record.signalType)) {
            if (currentPrice >= record.takeProfit) {
                return SignalOutcome.HIT_TP;
            } else if (currentPrice <= record.stopLoss) {
                return SignalOutcome.HIT_SL;
            } else {
                return SignalOutcome.TIMEOUT;
            }
        } else { // SELL
            if (currentPrice <= record.takeProfit) {
                return SignalOutcome.HIT_TP;
            } else if (currentPrice >= record.stopLoss) {
                return SignalOutcome.HIT_SL;
            } else {
                return SignalOutcome.TIMEOUT;
            }
        }
    }
    
    /**
     * 计算盈亏百分比
     */
    private double calculatePnLPercentage(SignalRecord record, double currentPrice) {
        if ("BUY".equals(record.signalType)) {
            return ((currentPrice - record.entryPrice) / record.entryPrice) * 100.0;
        } else { // SELL
            return ((record.entryPrice - currentPrice) / record.entryPrice) * 100.0;
        }
    }
    
    /**
     * 获取当前价格（从市场数据获取）
     */
    private double getCurrentPrice(String symbol) {
        try {
            // 获取最新的1小时K线数据
            List<MarketAnalysisService.Candlestick> candles = marketDataService.getKlines(symbol, "1h", 1);
            if (candles != null && !candles.isEmpty()) {
                return candles.get(0).getClose();
            }
            
            // 如果K线数据获取失败，尝试获取当前价格
            return marketDataService.getCurrentPrice(symbol);
        } catch (Exception e) {
            logger.warn("获取实时价格失败: {}", symbol, e);
        }
        
        // 如果获取失败，返回一个默认值（实际应用中应该有更好的处理方式）
        return 0.0;
    }
    
    /**
     * 创建信号绩效表
     */
    private void createSignalPerformanceTable() {
        String createTableSql = 
            "CREATE TABLE IF NOT EXISTS signal_performance (" +
            "id BIGINT AUTO_INCREMENT PRIMARY KEY," +
            "symbol VARCHAR(20) NOT NULL," +
            "signal_time TIMESTAMP NOT NULL," +
            "signal_type VARCHAR(10) NOT NULL," +
            "indicator VARCHAR(100)," +
            "price DECIMAL(20, 8) NOT NULL," +
            "stop_loss DECIMAL(20, 8)," +
            "take_profit DECIMAL(20, 8)," +
            "score INT," +
            "confidence VARCHAR(20)," +
            "reason TEXT," +
            "status VARCHAR(20) DEFAULT 'PENDING'," +
            "outcome_time TIMESTAMP NULL," +
            "final_price DECIMAL(20, 8)," +
            "pnl_percentage DECIMAL(10, 4)," +
            "notes TEXT," +
            "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
            "INDEX idx_symbol_time (symbol, signal_time)," +
            "INDEX idx_status (status)," +
            "INDEX idx_created_at (created_at)" +
            ")";
        
        try {
            jdbcTemplate.execute(createTableSql);
            logger.info("信号绩效表已创建或已存在");
        } catch (Exception e) {
            logger.error("创建信号绩效表失败", e);
        }
    }
    
    /**
     * 信号记录内部类
     */
    private static class SignalRecord {
        double entryPrice;
        double stopLoss;
        double takeProfit;
        String signalType;
        
        SignalRecord(double entryPrice, double stopLoss, double takeProfit, String signalType) {
            this.entryPrice = entryPrice;
            this.stopLoss = stopLoss;
            this.takeProfit = takeProfit;
            this.signalType = signalType;
        }
    }
    
    /**
     * 信号结果枚举
     */
    public enum SignalOutcome {
        HIT_TP("HIT_TP", "止盈达成"),
        HIT_SL("HIT_SL", "止损触发"), 
        TIMEOUT("TIMEOUT", "超时未触发");
        
        private final String status;
        private final String description;
        
        SignalOutcome(String status, String description) {
            this.status = status;
            this.description = description;
        }
        
        public String getStatus() { return status; }
        public String getDescription() { return description; }
    }
}