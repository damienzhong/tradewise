package com.example.tradewise.controller;

import com.example.tradewise.service.SystemHealthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 系统健康检查控制器
 * 提供系统运行状态和统计信息的API
 */
@RestController
@RequestMapping("/api/system-health")
public class SystemHealthController {

    @Autowired
    private SystemHealthService systemHealthService;

    @Autowired
    private com.example.tradewise.service.SystemAlertService systemAlertService;

    /**
     * 获取系统健康状态
     */
    @GetMapping("/status")
    public ResponseEntity<SystemHealthService.SystemHealthStatus> getHealthStatus() {
        SystemHealthService.SystemHealthStatus status = systemHealthService.getSystemHealthStatus();
        return ResponseEntity.ok(status);
    }

    /**
     * 获取简化的健康状态（用于负载均衡器等）
     */
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        SystemHealthService.SystemHealthStatus status = systemHealthService.getSystemHealthStatus();
        String healthStatus = status.getOverallHealth();
        
        if ("HEALTHY".equals(healthStatus)) {
            return ResponseEntity.ok("UP");
        } else if ("WARNING".equals(healthStatus)) {
            return ResponseEntity.status(299).body("WARNING"); // 299 is a custom warning status
        } else {
            return ResponseEntity.status(503).body("DOWN");
        }
    }

    /**
     * 重置统计信息
     */
    @PostMapping("/reset-stats")
    public ResponseEntity<String> resetStatistics() {
        systemHealthService.resetStatistics();
        return ResponseEntity.ok("Statistics reset successfully");
    }

    /**
     * 获取API调用统计
     */
    @GetMapping("/api-stats")
    public ResponseEntity<java.util.Map<String, Object>> getApiStats() {
        SystemHealthService.SystemHealthStatus status = systemHealthService.getSystemHealthStatus();
        
        // 创建一个Map作为响应对象
        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("successfulApiCalls", status.getSuccessfulApiCalls());
        stats.put("failedApiCalls", status.getFailedApiCalls());
        stats.put("averageProcessingTimeMs", status.getAverageProcessingTimeMs());
        stats.put("successRate", status.getSuccessRate());
        stats.put("overallHealth", status.getOverallHealth());
        
        return ResponseEntity.ok(stats);
    }

    /**
     * 获取业务统计
     */
    @GetMapping("/business-stats")
    public ResponseEntity<java.util.Map<String, Object>> getBusinessStats() {
        SystemHealthService.SystemHealthStatus status = systemHealthService.getSystemHealthStatus();
        
        // 创建一个Map作为响应对象
        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("ordersProcessed", status.getOrdersProcessed());
        stats.put("tradersMonitored", status.getTradersMonitored());
        stats.put("marketAnalysesPerformed", status.getMarketAnalysesPerformed());
        stats.put("tradingSignalsGenerated", status.getTradingSignalsGenerated());
        stats.put("uptimeMinutes", status.getUptimeMinutes());
        
        return ResponseEntity.ok(stats);
    }

    /**
     * 获取错误统计信息
     */
    @GetMapping("/errors")
    public ResponseEntity<java.util.Map<String, Object>> getErrorStatistics() {
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        response.put("errorCounters", systemAlertService.getErrorStatistics());
        response.put("timestamp", java.time.LocalDateTime.now());
        return ResponseEntity.ok(response);
    }
}
