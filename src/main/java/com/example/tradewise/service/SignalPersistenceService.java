package com.example.tradewise.service;

import com.example.tradewise.entity.Signal;
import com.example.tradewise.mapper.SignalMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 信号持久化服务
 * 负责将生成的交易信号自动保存到数据库
 */
@Service
public class SignalPersistenceService {

    private static final Logger logger = LoggerFactory.getLogger(SignalPersistenceService.class);

    @Autowired
    private SignalMapper signalMapper;

    @Autowired
    private com.example.tradewise.service.SystemAlertService systemAlertService; // 注入系统告警服务

    /**
     * 保存交易信号到数据库
     */
    public void saveSignal(MarketAnalysisService.TradingSignal tradingSignal) {
        try {
            Signal signal = convertToEntity(tradingSignal);
            signalMapper.insert(signal);
            logger.info("成功保存信号到数据库: {} - {} - {}", 
                signal.getSymbol(), signal.getSignalType(), signal.getPrice());
            
            // 重置数据库错误计数器
            systemAlertService.resetErrorCounter("DB_SignalInsert");
        } catch (Exception e) {
            logger.error("保存信号到数据库失败: {}", tradingSignal.getSymbol(), e);
            
            // 记录数据库失败告警
            systemAlertService.recordDatabaseFailure("SignalInsert", 
                String.format("保存信号失败: %s - %s", tradingSignal.getSymbol(), e.getMessage()));
        }
    }

    /**
     * 批量保存交易信号（带去重逻辑）
     */
    public void saveSignals(List<MarketAnalysisService.TradingSignal> tradingSignals) {
        int savedCount = 0;
        int duplicateCount = 0;
        
        for (MarketAnalysisService.TradingSignal tradingSignal : tradingSignals) {
            if (!isDuplicateSignal(tradingSignal)) {
                saveSignal(tradingSignal);
                savedCount++;
            } else {
                duplicateCount++;
                logger.debug("跳过重复信号: {} - {} - {}", 
                    tradingSignal.getSymbol(), tradingSignal.getSignalType(), tradingSignal.getPrice());
            }
        }
        
        logger.info("批量保存信号完成: 总数={}, 保存={}, 重复={}", 
            tradingSignals.size(), savedCount, duplicateCount);
    }

    /**
     * 检查是否为重复信号
     * 判断标准：最近N小时内同一交易对、同一方向、价格相近5%以内
     */
    private boolean isDuplicateSignal(MarketAnalysisService.TradingSignal newSignal) {
        try {
            // 获取最近4小时内的活跃信号
            LocalDateTime cutoffTime = LocalDateTime.now().minusHours(4);
            List<Signal> recentSignals = signalMapper.findByConditions(
                newSignal.getSymbol(), 
                null, 
                "ACTIVE", 
                cutoffTime, 
                null, 
                0, 
                100
            );
            
            // 重置数据库错误计数器
            systemAlertService.resetErrorCounter("DB_SignalQuery");
            
            for (Signal existingSignal : recentSignals) {
                // 检查方向是否相同
                if (!existingSignal.getSignalType().equals(newSignal.getSignalType().name())) {
                    continue;
                }
                
                // 检查价格是否相近（5%以内）
                BigDecimal existingPrice = existingSignal.getPrice();
                BigDecimal newPrice = BigDecimal.valueOf(newSignal.getPrice());
                
                if (existingPrice != null && existingPrice.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal priceDiff = newPrice.subtract(existingPrice).abs();
                    BigDecimal priceChangePercent = priceDiff.divide(existingPrice, 4, BigDecimal.ROUND_HALF_UP)
                                                              .multiply(new BigDecimal("100"));
                    
                    if (priceChangePercent.compareTo(new BigDecimal("5")) <= 0) {
                        logger.debug("发现重复信号: {} 价格相近 {}% (ID: {})", 
                            newSignal.getSymbol(), priceChangePercent, existingSignal.getId());
                        return true;
                    }
                }
            }
            
            return false;
        } catch (Exception e) {
            logger.error("检查重复信号失败", e);
            
            // 记录数据库失败告警
            systemAlertService.recordDatabaseFailure("SignalQuery", 
                String.format("查询信号失败: %s - %s", newSignal.getSymbol(), e.getMessage()));
            
            return false; // 出错时不阻止保存
        }
    }

    /**
     * 将TradingSignal转换为Signal实体
     */
    private Signal convertToEntity(MarketAnalysisService.TradingSignal tradingSignal) {
        Signal signal = new Signal();
        
        signal.setSymbol(tradingSignal.getSymbol());
        signal.setSignalTime(tradingSignal.getTimestamp());
        signal.setSignalType(tradingSignal.getSignalType().name());
        signal.setIndicator(tradingSignal.getIndicator());
        signal.setPrice(BigDecimal.valueOf(tradingSignal.getPrice()));
        signal.setStopLoss(BigDecimal.valueOf(tradingSignal.getStopLoss()));
        signal.setTakeProfit(BigDecimal.valueOf(tradingSignal.getTakeProfit()));
        signal.setScore(tradingSignal.getScore());
        signal.setConfidence(tradingSignal.getConfidence());
        signal.setReason(tradingSignal.getReason());
        signal.setStatus("ACTIVE"); // 新生成的信号默认为活跃状态
        
        return signal;
    }
}
