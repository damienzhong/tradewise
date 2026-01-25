package com.example.tradewise.controller;

import com.example.tradewise.service.FeatureConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 功能开关控制器
 * 用于动态控制各项功能的开启/关闭
 */
@RestController
@RequestMapping("/api/features")
public class FeatureToggleController {
    
    @Autowired
    private FeatureConfigService featureConfigService;
    
    /**
     * 获取当前所有功能的状态
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Boolean>> getFeatureStatus() {
        Map<String, Boolean> status = new HashMap<>();
        status.put("marketAnalysisEnabled", featureConfigService.isFeatureEnabled("market_analysis"));
        status.put("copyTradingEnabled", featureConfigService.isFeatureEnabled("copy_trading"));
        status.put("emailNotificationEnabled", featureConfigService.isFeatureEnabled("email_notification"));
        
        return ResponseEntity.ok(status);
    }
    
    /**
     * 获取所有功能详细信息
     */
    @GetMapping("/all")
    public ResponseEntity<Map<String, Map<String, Object>>> getAllFeatures() {
        return ResponseEntity.ok(featureConfigService.getAllFeatures());
    }
    
    /**
     * 控制市场分析功能的开关
     */
    @PostMapping("/market-analysis/toggle")
    public ResponseEntity<Map<String, Object>> toggleMarketAnalysis(@RequestParam boolean enabled) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            featureConfigService.setFeatureEnabled("market_analysis", enabled);
            response.put("success", true);
            response.put("message", "市场分析功能已" + (enabled ? "启用" : "禁用"));
            response.put("enabled", enabled);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "切换市场分析功能失败: " + e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 控制交易员跟单功能的开关
     */
    @PostMapping("/copy-trading/toggle")
    public ResponseEntity<Map<String, Object>> toggleCopyTrading(@RequestParam boolean enabled) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            featureConfigService.setFeatureEnabled("copy_trading", enabled);
            response.put("success", true);
            response.put("message", "交易员跟单功能已" + (enabled ? "启用" : "禁用"));
            response.put("enabled", enabled);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "切换交易员跟单功能失败: " + e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 控制邮件通知功能的开关
     */
    @PostMapping("/email-notification/toggle")
    public ResponseEntity<Map<String, Object>> toggleEmailNotification(@RequestParam boolean enabled) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            featureConfigService.setFeatureEnabled("email_notification", enabled);
            response.put("success", true);
            response.put("message", "邮件通知功能已" + (enabled ? "启用" : "禁用"));
            response.put("enabled", enabled);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "切换邮件通知功能失败: " + e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 获取市场分析功能状态
     */
    @GetMapping("/market-analysis/status")
    public ResponseEntity<Boolean> getMarketAnalysisStatus() {
        return ResponseEntity.ok(featureConfigService.isFeatureEnabled("market_analysis"));
    }
    
    /**
     * 获取交易员跟单功能状态
     */
    @GetMapping("/copy-trading/status")
    public ResponseEntity<Boolean> getCopyTradingStatus() {
        return ResponseEntity.ok(featureConfigService.isFeatureEnabled("copy_trading"));
    }
}
