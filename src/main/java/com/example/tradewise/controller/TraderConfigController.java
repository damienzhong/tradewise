package com.example.tradewise.controller;

import com.example.tradewise.entity.TraderConfig;
import com.example.tradewise.service.TraderConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/trader-config")
public class TraderConfigController {

    @Autowired
    private TraderConfigService traderConfigService;

    @GetMapping("/list")
    public ResponseEntity<List<TraderConfig>> getAllTraderConfigs() {
        List<TraderConfig> traderConfigs = traderConfigService.getAllEnabledTraderConfigs();
        return ResponseEntity.ok(traderConfigs);
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<Map<String, Object>> updateTraderConfig(@PathVariable Integer id, @RequestBody TraderConfig traderConfig) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 获取现有配置并更新
            TraderConfig existingConfig = traderConfigService.findByTraderId(traderConfig.getTraderId());
            if (existingConfig == null || !existingConfig.getId().equals(id)) {
                response.put("success", false);
                response.put("message", "交易员配置不存在");
                return ResponseEntity.badRequest().body(response);
            }
            
            existingConfig.setName(traderConfig.getName());
            existingConfig.setPortfolioId(traderConfig.getPortfolioId());
            existingConfig.setEnabled(traderConfig.getEnabled());
            
            traderConfigService.updateTraderConfig(existingConfig);
            response.put("success", true);
            response.put("message", "交易员配置更新成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "更新交易员配置失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @DeleteMapping("/delete/{traderId}")
    public ResponseEntity<Map<String, Object>> deleteTraderConfig(@PathVariable String traderId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            traderConfigService.deleteTraderConfigByTraderId(traderId);
            response.put("success", true);
            response.put("message", "交易员配置删除成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "删除交易员配置失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}