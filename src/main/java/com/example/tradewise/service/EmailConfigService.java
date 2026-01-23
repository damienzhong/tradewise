package com.example.tradewise.service;

import com.example.tradewise.entity.EmailConfig;
import com.example.tradewise.mapper.EmailConfigMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmailConfigService {

    @Autowired
    private EmailConfigMapper emailConfigMapper;

    public List<EmailConfig> getAllEnabledEmailConfigs() {
        return emailConfigMapper.findAllEnabled();
    }

    public EmailConfig addEmailConfig(EmailConfig emailConfig) {
        // 检查邮箱是否已存在
        EmailConfig existingConfig = emailConfigMapper.findByEmailAddress(emailConfig.getEmailAddress());
        if (existingConfig != null) {
            throw new RuntimeException("邮箱地址已存在: " + emailConfig.getEmailAddress());
        }
        
        if (emailConfig.getEnabled() == null) {
            emailConfig.setEnabled(true);
        }
        
        emailConfigMapper.insert(emailConfig);
        return emailConfig;
    }

    public void updateEmailConfig(EmailConfig emailConfig) {
        emailConfigMapper.update(emailConfig);
    }

    public void deleteEmailConfigById(Integer id) {
        emailConfigMapper.deleteById(id);
    }

    public void deleteEmailConfigByEmail(String emailAddress) {
        emailConfigMapper.deleteByEmailAddress(emailAddress);
    }

    public EmailConfig findByEmailAddress(String emailAddress) {
        return emailConfigMapper.findByEmailAddress(emailAddress);
    }

    public int getTotalEmailConfigsCount() {
        return emailConfigMapper.countAll();
    }
}