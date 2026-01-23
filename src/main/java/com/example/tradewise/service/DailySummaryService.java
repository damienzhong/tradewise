package com.example.tradewise.service;

import com.example.tradewise.service.MarketAnalysisService.TradingSignal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import javax.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 每日摘要服务 - 发送低优先级信号汇总
 */
@Service
public class DailySummaryService {

    private static final Logger logger = LoggerFactory.getLogger(DailySummaryService.class);

    @Autowired
    private SignalFilterService signalFilterService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private TemplateEngine templateEngine;

    /**
     * 发送每日信号摘要
     */
    public void sendDailySummary() {
        List<TradingSignal> lowPrioritySignals = signalFilterService.getLowPrioritySignals();

        if (lowPrioritySignals.isEmpty()) {
            logger.info("没有低优先级信号，跳过每日摘要发送");
            return;
        }

        try {
            // 按交易对分组
            Map<String, List<TradingSignal>> signalsBySymbol = lowPrioritySignals.stream()
                    .collect(Collectors.groupingBy(TradingSignal::getSymbol));

            // 获取收件人列表
            List<String> recipientEmails = emailService.getEmailAddressesFromDatabase();

            if (recipientEmails.isEmpty()) {
                logger.warn("没有配置邮箱地址，跳过每日摘要发送");
                return;
            }

            // 准备邮件内容
            Context context = new Context();
            context.setVariable("signalsBySymbol", signalsBySymbol);
            context.setVariable("totalSignalsCount", lowPrioritySignals.size());
            context.setVariable("totalSymbolsCount", signalsBySymbol.size());
            context.setVariable("summaryDate", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            context.setVariable("statistics", signalFilterService.getStatistics());

            // 生成HTML内容
            String htmlContent = templateEngine.process("daily-summary-template", context);

            // 发送邮件
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(emailService.getSenderEmail());
            helper.setTo(recipientEmails.toArray(new String[0]));
            helper.setSubject(String.format("[每日摘要] %d个交易对的%d个观察信号",
                    signalsBySymbol.size(), lowPrioritySignals.size()));
            helper.setText(htmlContent, true);

            mailSender.send(mimeMessage);

            logger.info("每日摘要发送成功: {}个交易对，{}个信号，{}个收件人",
                    signalsBySymbol.size(), lowPrioritySignals.size(), recipientEmails.size());

            // 清空缓存
            signalFilterService.clearLowPrioritySignals();

        } catch (Exception e) {
            logger.error("发送每日摘要失败", e);
        }
    }
}
