package com.example.tradewise.service;

import com.example.tradewise.entity.EmailConfig;
import com.example.tradewise.entity.Order;
import com.example.tradewise.mapper.EmailConfigMapper;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EmailService {
    
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    
    @Autowired
    private JavaMailSender mailSender;
    
    @Autowired
    private TemplateEngine templateEngine;
    
    @Autowired
    private EmailConfigService emailConfigService;
    
    @Value("${spring.mail.username}")
    private String senderEmail;
    
    // 移除了 @Value("${tradewise.email.to}") private String recipientEmailsString;
    
    public void sendNewOrdersNotification(List<Order> newOrders, String traderName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            // 从数据库获取启用的邮箱地址列表
            List<String> recipientEmails = getEmailAddressesFromDatabase();
            
            // 如果没有配置邮箱地址，直接返回
            if (recipientEmails.isEmpty()) {
                logger.warn("没有配置任何邮箱地址，跳过邮件发送");
                return;
            }
            
            // 设置邮件主题
            DecimalFormat df = new DecimalFormat("#.#####");
            String subject = String.format("[跟单提醒] %s 有新的订单 (%d个)", traderName, newOrders.size());
            
            // 准备邮件模板上下文
            Context context = new Context();
            context.setVariable("traderName", traderName);
            context.setVariable("newOrdersCount", newOrders.size());
            context.setVariable("newOrders", newOrders);
            context.setVariable("df", df);
            
            // 生成HTML内容
            String htmlContent = templateEngine.process("order-notification-template", context);
            
            // 设置邮件内容
            helper.setFrom(senderEmail);
            helper.setTo(recipientEmails.toArray(new String[0]));
            helper.setSubject(subject);
            helper.setText(htmlContent, true); // true表示内容是HTML
            
            // 发送邮件
            mailSender.send(message);
            
            logger.info("Successfully sent email notification for {} new orders from trader: {} to {} recipients", newOrders.size(), traderName, recipientEmails.size());
        } catch (MailAuthenticationException e) {
            logger.error("邮件认证失败，请检查邮箱配置和密码: {}", e.getMessage());
            logger.error("邮箱配置可能存在问题，请参考README.md中的邮箱配置说明");
        } catch (MessagingException e) {
            logger.error("邮件发送失败", e);
        } catch (Exception e) {
            logger.error("发送邮件通知时发生未知错误", e);
        }
    }
    
    /**
     * 从数据库获取启用的邮箱地址列表
     */
    public List<String> getEmailAddressesFromDatabase() {
        List<EmailConfig> emailConfigs = emailConfigService.getAllEnabledEmailConfigs();
        return emailConfigs.stream()
                .map(EmailConfig::getEmailAddress)
                .collect(Collectors.toList());
    }
    
    public JavaMailSender getMailSender() {
        return mailSender;
    }
    
    public TemplateEngine getTemplateEngine() {
        return templateEngine;
    }
    
    public String getSenderEmail() {
        return senderEmail;
    }
}