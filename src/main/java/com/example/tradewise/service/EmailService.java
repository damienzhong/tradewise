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

    public void sendNewOrdersNotification(List<Order> newOrders, String traderName) {
        try {
            sendNewOrdersNotificationWithQualityAssessment(newOrders, traderName, null);
        } catch (Exception e) {
            logger.error("发送带质量评估的邮件失败，尝试发送基本邮件通知", e);
            sendBasicOrderNotification(newOrders, traderName);
        }
    }

    private void sendBasicOrderNotification(List<Order> newOrders, String traderName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            List<String> recipientEmails = getEmailAddressesFromDatabase();

            if (recipientEmails.isEmpty()) {
                logger.warn("没有配置任何邮箱地址，跳过邮件发送");
                return;
            }

            DecimalFormat df = new DecimalFormat("#.#####");
            String subject = String.format("[跟单提醒] %s 有新的订单 (%d个)", traderName, newOrders.size());

            Context context = new Context();
            context.setVariable("traderName", traderName);
            context.setVariable("newOrdersCount", newOrders.size());
            context.setVariable("newOrders", newOrders);
            context.setVariable("df", df);

            String htmlContent = templateEngine.process("order-notification-template", context);

            helper.setFrom(senderEmail);
            helper.setTo(recipientEmails.toArray(new String[0]));
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);

            logger.info("Successfully sent basic email notification for {} new orders from trader: {} to {} recipients",
                    newOrders.size(), traderName, recipientEmails.size());
        } catch (Exception e) {
            logger.error("发送基本邮件通知失败", e);
        }
    }

    public void sendNewOrdersNotificationWithQualityAssessment(List<Order> newOrders, String traderName, CopyTradeSignalEvaluator signalEvaluator) {
        if (signalEvaluator == null) {
            sendBasicOrderNotification(newOrders, traderName);
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            List<String> recipientEmails = getEmailAddressesFromDatabase();

            if (recipientEmails.isEmpty()) {
                logger.warn("没有配置任何邮箱地址，跳过邮件发送");
                return;
            }

            List<OrderWithQuality> ordersWithQuality = newOrders.stream()
                    .map(order -> {
                        CopyTradeSignalEvaluator.SignalQualityAssessment assessment =
                                signalEvaluator.evaluateSignalQuality(order);
                        return new OrderWithQuality(order, assessment);
                    })
                    .collect(Collectors.toList());

            DecimalFormat df = new DecimalFormat("#.#####");
            String subject = String.format("[跟单提醒] %s 有新的订单 (%d个)", traderName, newOrders.size());

            Context context = new Context();
            context.setVariable("traderName", traderName);
            context.setVariable("newOrdersCount", newOrders.size());
            context.setVariable("ordersWithQuality", ordersWithQuality);
            context.setVariable("df", df);

            String htmlContent = templateEngine.process("order-notification-with-quality-template", context);

            helper.setFrom(senderEmail);
            helper.setTo(recipientEmails.toArray(new String[0]));
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);

            logger.info("Successfully sent email notification with quality assessment for {} new orders from trader: {} to {} recipients",
                    newOrders.size(), traderName, recipientEmails.size());
        } catch (MailAuthenticationException e) {
            logger.error("邮件认证失败，请检查邮箱配置和密码: {}", e.getMessage());
        } catch (MessagingException e) {
            logger.error("邮件发送失败", e);
        } catch (Exception e) {
            logger.error("发送邮件通知时发生未知错误", e);
        }
    }

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

    public static class OrderWithQuality {
        private final Order order;
        private final CopyTradeSignalEvaluator.SignalQualityAssessment qualityAssessment;

        public OrderWithQuality(Order order, CopyTradeSignalEvaluator.SignalQualityAssessment qualityAssessment) {
            this.order = order;
            this.qualityAssessment = qualityAssessment;
        }

        public Order getOrder() { return order; }
        public CopyTradeSignalEvaluator.SignalQualityAssessment getQualityAssessment() { return qualityAssessment; }
    }
}