package com.example.tradewise.service;

import com.example.tradewise.service.MarketAnalysisService.TradingSignal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 邮件过滤策略
 * 用于决定是否发送邮件通知
 */
@Service
public class EmailFilterStrategy {
    
    private static final Logger logger = LoggerFactory.getLogger(EmailFilterStrategy.class);
    
    /**
     * 判断是否应该发送邮件
     */
    public boolean shouldSendEmail(TradingSignal signal) {
        // 条件1：LEVEL_1和LEVEL_2发送即时邮件，LEVEL_3只记录不发送
        if ("LEVEL_3".equals(signal.getSignalLevel())) {
            logger.debug("LEVEL_3信号不发送即时邮件，仅记录: {} - {}", signal.getSymbol(), signal.getIndicator());
            return false;
        }
        
        // 条件2：检查是否有有效的风险回报比
        Object riskRewardRatioObj = signal.getSignalExplanation().get("risk_reward_ratio");
        if (riskRewardRatioObj instanceof Double) {
            double riskRewardRatio = (Double) riskRewardRatioObj;
            if (riskRewardRatio < 1.5) {
                logger.debug("风险回报比小于1.5，不发送邮件: {} - {} (RR: {})", 
                    signal.getSymbol(), signal.getIndicator(), riskRewardRatio);
                return false;
            }
        } else {
            // 如果没有风险回报比信息，要求信号评分至少为6分
            if (signal.getScore() < 6) {
                logger.debug("信号评分低于6分且无风险回报比，不发送邮件: {} - {} (Score: {})", 
                    signal.getSymbol(), signal.getIndicator(), signal.getScore());
                return false;
            }
        }
        
        // 条件3：检查是否具有完整的交易计划
        if (signal.getStopLoss() == 0.0 || signal.getTakeProfit() == 0.0) {
            logger.debug("缺少止损或止盈价格，不发送邮件: {} - {}", signal.getSymbol(), signal.getIndicator());
            return false;
        }
        
        logger.debug("信号通过过滤条件，将发送邮件: {} - {}", signal.getSymbol(), signal.getIndicator());
        return true;
    }
    
    /**
     * 判断邮件优先级
     */
    public EmailPriority determinePriority(TradingSignal signal) {
        if ("LEVEL_1".equals(signal.getSignalLevel())) {
            Object riskRewardRatioObj = signal.getSignalExplanation().get("risk_reward_ratio");
            if (riskRewardRatioObj instanceof Double && (Double) riskRewardRatioObj > 2.0) {
                return EmailPriority.URGENT;  // LEVEL_1 + 高风险回报比
            }
            return EmailPriority.HIGH;  // LEVEL_1
        } else if ("LEVEL_2".equals(signal.getSignalLevel())) {
            return EmailPriority.MEDIUM;  // LEVEL_2
        } else {
            return EmailPriority.LOW;  // LEVEL_3 (虽然通常不会发送邮件)
        }
    }
    
    /**
     * 过滤信号列表，只返回应该发送邮件的信号
     */
    public List<TradingSignal> filterSignals(List<TradingSignal> signals) {
        return signals.stream()
            .filter(this::shouldSendEmail)
            .peek(signal -> logger.debug("发送邮件信号: {} - {} (Level: {}, Score: {})", 
                signal.getSymbol(), signal.getIndicator(), signal.getSignalLevel(), signal.getScore()))
            .collect(Collectors.toList());
    }
    
    /**
     * 邮件优先级枚举
     */
    public enum EmailPriority {
        URGENT("紧急", "immediate"),      // LEVEL_1 + 风险回报比>2
        HIGH("重要", "within_5_min"),     // LEVEL_1
        MEDIUM("常规", "within_15_min"),  // LEVEL_2 + 多个信号共振
        LOW("观察", "daily_summary");     // LEVEL_3或低质量信号
        
        private final String displayName;
        private final String deliverySchedule;
        
        EmailPriority(String displayName, String deliverySchedule) {
            this.displayName = displayName;
            this.deliverySchedule = deliverySchedule;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public String getDeliverySchedule() {
            return deliverySchedule;
        }
    }
}