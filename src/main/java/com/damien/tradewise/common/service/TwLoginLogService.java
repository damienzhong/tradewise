package com.damien.tradewise.common.service;

import com.damien.tradewise.common.entity.TwLoginLog;
import com.damien.tradewise.common.mapper.TwLoginLogMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 登录日志Service
 */
@Service
public class TwLoginLogService {
    
    @Autowired
    private TwLoginLogMapper loginLogMapper;
    
    /**
     * 记录登录日志
     */
    public void recordLogin(String userType, Long userId, String username, 
                           String loginIp, String userAgent, 
                           boolean success, String failReason) {
        TwLoginLog log = new TwLoginLog();
        log.setUserType(userType);
        log.setUserId(userId);
        log.setUsername(username);
        log.setLoginIp(loginIp);
        log.setUserAgent(userAgent);
        log.setLoginStatus(success ? "SUCCESS" : "FAILED");
        log.setFailReason(failReason);
        
        loginLogMapper.insert(log);
    }
    
    /**
     * 获取用户登录日志
     */
    public List<TwLoginLog> getUserLoginLogs(String userType, Long userId, Integer limit) {
        return loginLogMapper.findByUserId(userType, userId, limit);
    }
    
    /**
     * 获取所有登录日志（管理员用）
     */
    public List<TwLoginLog> getAllLoginLogs(Integer limit) {
        return loginLogMapper.findAll(limit);
    }
}
