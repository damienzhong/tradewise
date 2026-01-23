package com.example.tradewise.controller;

import com.example.tradewise.config.TradeWiseProperties;
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
    private TradeWiseProperties tradeWiseProperties;
    
    /**
     * 获取当前所有功能的状态
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Boolean>> getFeatureStatus() {
        Map<String, Boolean> status = new HashMap<>();
        status.put("marketAnalysis", tradeWiseProperties.getMarketAnalysis().isEnabled());
        status.put("copyTrading", tradeWiseProperties.getCopyTrading().isEnabled());
        
        return ResponseEntity.ok(status);
    }
    
    /**
     * 控制市场分析功能的开关
     */
    @PostMapping("/market-analysis/toggle")
    public ResponseEntity<Map<String, Object>> toggleMarketAnalysis(@RequestParam boolean enabled) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            tradeWiseProperties.getMarketAnalysis().setEnabled(enabled);
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
            tradeWiseProperties.getCopyTrading().setEnabled(enabled);
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
     * 获取市场分析功能状态
     */
    @GetMapping("/market-analysis/status")
    public ResponseEntity<Boolean> getMarketAnalysisStatus() {
        return ResponseEntity.ok(tradeWiseProperties.getMarketAnalysis().isEnabled());
    }
    
    /**
     * 获取交易员跟单功能状态
     */
    @GetMapping("/copy-trading/status")
    public ResponseEntity<Boolean> getCopyTradingStatus() {
        return ResponseEntity.ok(tradeWiseProperties.getCopyTrading().isEnabled());
    }
}