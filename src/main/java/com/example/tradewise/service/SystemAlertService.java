package com.example.tradewise.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 系统监控告警服务
 * 监控系统异常并发送告警通知
 */
@Service
public class SystemAlertService {

    private static final Logger logger = LoggerFactory.getLogger(SystemAlertService.class);

    @Autowired
    private EmailService emailService;

    // 错误计数器
    private final Map<String, AtomicInteger> errorCounters = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> lastAlertTime = new ConcurrentHashMap<>();
    
    // 告警阈值
    private static final int API_FAILURE_THRESHOLD = 3; // API连续失败3次告警
    private static final int DB_FAILURE_THRESHOLD = 2; // 数据库连续失败2次告警
    private static final int ALERT_COOLDOWN_MINUTES = 30; // 告警冷却时间30分钟

    /**
     * 记录API调用失败
     */
    public void recordApiFailure(String apiName, String errorMessage) {
        String key = "API_" + apiName;
        int count = errorCounters.computeIfAbsent(key, k -> new AtomicInteger(0)).incrementAndGet();
        
        logger.warn("API调用失败: {} (连续失败{}次) - {}", apiName, count, errorMessage);
        
        if (count >= API_FAILURE_THRESHOLD && shouldSendAlert(key)) {
            sendAlert("API调用异常", 
                String.format("API [%s] 连续失败 %d 次\n最后错误: %s", apiName, count, errorMessage));
        }
    }

    /**
     * 记录数据库操作失败
     */
    public void recordDatabaseFailure(String operation, String errorMessage) {
        String key = "DB_" + operation;
        int count = errorCounters.computeIfAbsent(key, k -> new AtomicInteger(0)).incrementAndGet();
        
        logger.error("数据库操作失败: {} (连续失败{}次) - {}", operation, count, errorMessage);
        
        if (count >= DB_FAILURE_THRESHOLD && shouldSendAlert(key)) {
            sendAlert("数据库异常", 
                String.format("数据库操作 [%s] 连续失败 %d 次\n最后错误: %s", operation, count, errorMessage));
        }
    }

    /**
     * 记录系统异常
     */
    public void recordSystemException(String component, String errorMessage, Throwable throwable) {
        String key = "SYS_" + component;
        
        logger.error("系统异常: {} - {}", component, errorMessage, throwable);
        
        if (shouldSendAlert(key)) {
            sendAlert("系统异常", 
                String.format("组件 [%s] 发生异常\n错误信息: %s\n异常类型: %s", 
                    component, errorMessage, throwable != null ? throwable.getClass().getSimpleName() : "Unknown"));
        }
    }

    /**
     * 重置错误计数器（操作成功时调用）
     */
    public void resetErrorCounter(String key) {
        errorCounters.remove(key);
    }

    /**
     * 判断是否应该发送告警（考虑冷却时间）
     */
    private boolean shouldSendAlert(String key) {
        LocalDateTime lastAlert = lastAlertTime.get(key);
        if (lastAlert == null) {
            return true;
        }
        
        LocalDateTime now = LocalDateTime.now();
        return now.isAfter(lastAlert.plusMinutes(ALERT_COOLDOWN_MINUTES));
    }

    /**
     * 发送告警邮件
     */
    private void sendAlert(String subject, String message) {
        try {
            // 获取管理员邮箱列表
            java.util.List<String> adminEmails = emailService.getEmailAddressesFromDatabase();
            
            if (adminEmails.isEmpty()) {
                logger.warn("没有配置管理员邮箱，无法发送告警");
                return;
            }

            // 构建告警邮件
            String fullMessage = String.format(
                "【TradeWise系统告警】\n\n" +
                "告警类型: %s\n" +
                "告警时间: %s\n" +
                "详细信息:\n%s\n\n" +
                "请及时检查系统状态。",
                subject,
                LocalDateTime.now(),
                message
            );

            // 发送邮件
            javax.mail.internet.MimeMessage mimeMessage = emailService.getMailSender().createMimeMessage();
            org.springframework.mail.javamail.MimeMessageHelper helper = 
                new org.springframework.mail.javamail.MimeMessageHelper(mimeMessage, true, "UTF-8");
            
            helper.setFrom(emailService.getSenderEmail());
            helper.setTo(adminEmails.toArray(new String[0]));
            helper.setSubject("[告警] " + subject);
            helper.setText(fullMessage);

            emailService.getMailSender().send(mimeMessage);
            
            logger.info("系统告警邮件已发送: {}", subject);
            
            // 更新最后告警时间
            lastAlertTime.put(subject, LocalDateTime.now());
            
        } catch (Exception e) {
            logger.error("发送告警邮件失败", e);
        }
    }

    /**
     * 获取当前错误统计
     */
    public Map<String, Integer> getErrorStatistics() {
        Map<String, Integer> stats = new ConcurrentHashMap<>();
        errorCounters.forEach((key, counter) -> stats.put(key, counter.get()));
        return stats;
    }
}
