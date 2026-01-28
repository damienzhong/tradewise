package com.damien.tradewise.common.service;

import com.damien.tradewise.admin.entity.TwSystemEmailConfig;
import com.damien.tradewise.admin.service.TwSystemEmailConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import javax.mail.internet.MimeMessage;
import java.util.Map;

/**
 * 通用邮件发送服务
 * 从数据库获取邮件配置
 */
@Service
public class TwEmailService {
    
    private static final Logger logger = LoggerFactory.getLogger(TwEmailService.class);
    
    @Autowired
    private TwSystemEmailConfigService emailConfigService;
    
    @Autowired
    private TemplateEngine templateEngine;
    
    /**
     * 发送HTML邮件
     * 
     * @param to 收件人邮箱
     * @param subject 邮件主题
     * @param templateName 模板名称
     * @param variables 模板变量
     */
    public void sendHtmlEmail(String to, String subject, String templateName, Map<String, Object> variables) {
        try {
            // 从数据库获取邮件配置
            TwSystemEmailConfig emailConfig = emailConfigService.getDefaultConfig();
            if (emailConfig == null || !emailConfig.getEnabled()) {
                throw new RuntimeException("未配置默认邮件服务或邮件服务已禁用");
            }
            
            // 创建邮件发送器
            JavaMailSenderImpl mailSender = emailConfigService.createMailSender(emailConfig);
            
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            Context context = new Context();
            if (variables != null) {
                variables.forEach(context::setVariable);
            }
            
            String htmlContent = templateEngine.process(templateName, context);
            
            helper.setFrom(emailConfig.getFromAddress(), emailConfig.getFromName());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
            
            logger.info("邮件发送成功: from={}, to={}, subject={}", emailConfig.getFromAddress(), to, subject);
        } catch (Exception e) {
            logger.error("邮件发送失败: to={}, subject={}", to, subject, e);
            throw new RuntimeException("邮件发送失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 发送纯文本邮件
     * 
     * @param to 收件人邮箱
     * @param subject 邮件主题
     * @param content 邮件内容
     */
    public void sendTextEmail(String to, String subject, String content) {
        try {
            // 从数据库获取邮件配置
            TwSystemEmailConfig emailConfig = emailConfigService.getDefaultConfig();
            if (emailConfig == null || !emailConfig.getEnabled()) {
                throw new RuntimeException("未配置默认邮件服务或邮件服务已禁用");
            }
            
            // 创建邮件发送器
            JavaMailSenderImpl mailSender = emailConfigService.createMailSender(emailConfig);
            
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(emailConfig.getFromAddress(), emailConfig.getFromName());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content, false);
            
            mailSender.send(message);
            
            logger.info("纯文本邮件发送成功: from={}, to={}, subject={}", emailConfig.getFromAddress(), to, subject);
        } catch (Exception e) {
            logger.error("纯文本邮件发送失败: to={}, subject={}", to, subject, e);
            throw new RuntimeException("邮件发送失败: " + e.getMessage(), e);
        }
    }
}
