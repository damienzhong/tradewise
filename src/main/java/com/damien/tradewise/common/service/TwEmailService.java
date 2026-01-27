package com.damien.tradewise.common.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import javax.mail.internet.MimeMessage;
import java.util.Map;

/**
 * 通用邮件发送服务
 * 支持不同端配置不同的邮箱
 */
@Service
public class TwEmailService {
    
    private static final Logger logger = LoggerFactory.getLogger(TwEmailService.class);
    
    @Autowired
    private JavaMailSender mailSender;
    
    @Autowired
    private TemplateEngine templateEngine;
    
    @Value("${spring.mail.username}")
    private String defaultSenderEmail;
    
    /**
     * 发送HTML邮件
     * 
     * @param to 收件人邮箱
     * @param subject 邮件主题
     * @param templateName 模板名称
     * @param variables 模板变量
     */
    public void sendHtmlEmail(String to, String subject, String templateName, Map<String, Object> variables) {
        sendHtmlEmail(defaultSenderEmail, to, subject, templateName, variables);
    }
    
    /**
     * 发送HTML邮件（指定发件人）
     * 
     * @param from 发件人邮箱
     * @param to 收件人邮箱
     * @param subject 邮件主题
     * @param templateName 模板名称
     * @param variables 模板变量
     */
    public void sendHtmlEmail(String from, String to, String subject, String templateName, Map<String, Object> variables) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            Context context = new Context();
            if (variables != null) {
                variables.forEach(context::setVariable);
            }
            
            String htmlContent = templateEngine.process(templateName, context);
            
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
            
            logger.info("邮件发送成功: from={}, to={}, subject={}", from, to, subject);
        } catch (Exception e) {
            logger.error("邮件发送失败: from={}, to={}, subject={}", from, to, subject, e);
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
        sendTextEmail(defaultSenderEmail, to, subject, content);
    }
    
    /**
     * 发送纯文本邮件（指定发件人）
     * 
     * @param from 发件人邮箱
     * @param to 收件人邮箱
     * @param subject 邮件主题
     * @param content 邮件内容
     */
    public void sendTextEmail(String from, String to, String subject, String content) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content, false);
            
            mailSender.send(message);
            
            logger.info("纯文本邮件发送成功: from={}, to={}, subject={}", from, to, subject);
        } catch (Exception e) {
            logger.error("纯文本邮件发送失败: from={}, to={}, subject={}", from, to, subject, e);
            throw new RuntimeException("邮件发送失败: " + e.getMessage(), e);
        }
    }
}
