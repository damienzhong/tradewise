package com.damien.tradewise.admin.service;

import com.damien.tradewise.admin.entity.TwSystemEmailConfig;
import com.damien.tradewise.admin.mapper.TwSystemEmailConfigMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.util.List;
import java.util.Properties;

@Service
public class TwSystemEmailConfigService {
    
    @Autowired
    private TwSystemEmailConfigMapper emailConfigMapper;
    
    public List<TwSystemEmailConfig> getAllConfigs() {
        return emailConfigMapper.findAll();
    }
    
    public TwSystemEmailConfig getDefaultConfig() {
        return emailConfigMapper.findDefault();
    }
    
    public void addConfig(TwSystemEmailConfig config) {
        if (config.getIsDefault()) {
            emailConfigMapper.clearDefault();
        }
        config.setTestStatus("UNTESTED");
        emailConfigMapper.insert(config);
    }
    
    public void updateConfig(TwSystemEmailConfig config) {
        if (config.getIsDefault()) {
            emailConfigMapper.clearDefault();
        }
        emailConfigMapper.update(config);
    }
    
    public void deleteConfig(Long id) {
        emailConfigMapper.delete(id);
    }
    
    public void setDefault(Long id) {
        emailConfigMapper.clearDefault();
        TwSystemEmailConfig config = emailConfigMapper.findById(id);
        config.setIsDefault(true);
        emailConfigMapper.update(config);
    }
    
    public boolean testConnection(Long id) {
        TwSystemEmailConfig config = emailConfigMapper.findById(id);
        if (config == null) {
            return false;
        }
        
        try {
            JavaMailSenderImpl mailSender = createMailSender(config);
            mailSender.testConnection();
            emailConfigMapper.updateTestStatus(id, "SUCCESS");
            return true;
        } catch (Exception e) {
            emailConfigMapper.updateTestStatus(id, "FAILED");
            return false;
        }
    }
    
    public JavaMailSenderImpl createMailSender(TwSystemEmailConfig config) {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(config.getSmtpHost());
        mailSender.setPort(config.getSmtpPort());
        mailSender.setUsername(config.getUsername());
        mailSender.setPassword(config.getPassword());
        
        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", config.getUseTls() ? "true" : "false");
        props.put("mail.smtp.ssl.enable", config.getUseSsl() ? "true" : "false");
        
        return mailSender;
    }
}
