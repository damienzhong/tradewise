package com.damien.tradewise.common.service;

import com.damien.tradewise.admin.entity.TwSystemEmailConfig;
import com.damien.tradewise.admin.service.TwSystemEmailConfigService;
import com.damien.tradewise.common.entity.TwTraderOrder;
import com.damien.tradewise.user.mapper.TwUserSubscriptionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import javax.mail.internet.MimeMessage;
import java.util.List;

@Service
public class OrderEmailNotificationService {
    
    private static final Logger logger = LoggerFactory.getLogger(OrderEmailNotificationService.class);
    
    @Autowired
    private TwSystemEmailConfigService emailConfigService;
    
    @Autowired
    private TwUserSubscriptionMapper subscriptionMapper;
    
    @Autowired
    private TemplateEngine templateEngine;
    
    /**
     * 发送新订单通知邮件
     */
    public void sendNewOrderNotification(TwTraderOrder order, String traderName) {
        try {
            // 1. 获取默认邮件配置
            TwSystemEmailConfig emailConfig = emailConfigService.getDefaultConfig();
            if (emailConfig == null || !emailConfig.getEnabled()) {
                logger.warn("未配置默认邮件服务或邮件服务已禁用");
                return;
            }
            
            // 2. 获取订阅了该交易员的用户邮箱列表
            List<String> recipientEmails = subscriptionMapper.findSubscribedUserEmails(order.getTraderId());
            if (recipientEmails.isEmpty()) {
                logger.info("没有用户订阅交易员 {}, 跳过邮件发送", traderName);
                return;
            }
            
            // 3. 创建邮件发送器
            JavaMailSenderImpl mailSender = emailConfigService.createMailSender(emailConfig);
            
            // 4. 构建邮件内容
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(emailConfig.getFromAddress(), emailConfig.getFromName());
            helper.setTo(recipientEmails.toArray(new String[0]));
            helper.setSubject(String.format("[跟单提醒] %s 有新订单 - %s %s", 
                traderName, order.getSymbol(), order.getActionType()));
            
            // 5. 使用模板生成HTML内容
            Context context = new Context();
            context.setVariable("traderName", traderName);
            context.setVariable("order", order);
            
            String htmlContent = templateEngine.process("order-notification", context);
            helper.setText(htmlContent, true);
            
            // 6. 发送邮件
            mailSender.send(message);
            
            logger.info("成功发送订单通知邮件: traderId={}, orderId={}, recipients={}", 
                order.getTraderId(), order.getOrderId(), recipientEmails.size());
                
        } catch (Exception e) {
            logger.error("发送订单通知邮件失败: orderId={}, error={}", 
                order.getOrderId(), e.getMessage(), e);
        }
    }
}
