package com.example.tradewise.controller;

import com.example.tradewise.service.FeatureConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/feature-config")
public class FeatureConfigController {

    @Autowired
    private FeatureConfigService featureConfigService;

    @GetMapping("/status")
    public Map<String, Object> getStatus() {
        Map<String, Object> result = new HashMap<>();
        result.put("copyTradingEnabled", featureConfigService.isFeatureEnabled("copy_trading"));
        result.put("marketAnalysisEnabled", featureConfigService.isFeatureEnabled("market_analysis"));
        result.put("emailNotificationEnabled", featureConfigService.isFeatureEnabled("email_notification"));
        return result;
    }

    @PostMapping("/toggle/{featureKey}")
    public Map<String, Object> toggleFeature(@PathVariable String featureKey, @RequestParam boolean enabled) {
        featureConfigService.setFeatureEnabled(featureKey, enabled);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("featureKey", featureKey);
        result.put("enabled", enabled);
        return result;
    }

    @PostMapping("/user/{userId}/toggle/{featureKey}")
    public Map<String, Object> toggleUserFeature(
            @PathVariable Long userId,
            @PathVariable String featureKey,
            @RequestParam boolean enabled) {
        featureConfigService.setUserFeatureEnabled(userId, featureKey, enabled);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("userId", userId);
        result.put("featureKey", featureKey);
        result.put("enabled", enabled);
        return result;
    }
}
