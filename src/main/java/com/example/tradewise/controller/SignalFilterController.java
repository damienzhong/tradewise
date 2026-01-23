package com.example.tradewise.controller;

import com.example.tradewise.service.SignalFilterService;
import com.example.tradewise.service.DailySummaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 信号过滤控制器 - 提供信号过滤管理API
 */
@RestController
@RequestMapping("/api/signal-filter")
public class SignalFilterController {

    @Autowired
    private SignalFilterService signalFilterService;

    @Autowired
    private DailySummaryService dailySummaryService;

    /**
     * 获取信号过滤统计信息
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        Map<String, Object> stats = signalFilterService.getStatistics();
        return ResponseEntity.ok(stats);
    }

    /**
     * 手动触发每日摘要发送
     */
    @PostMapping("/send-summary")
    public ResponseEntity<Map<String, Object>> sendDailySummary() {
        Map<String, Object> response = new HashMap<>();
        try {
            dailySummaryService.sendDailySummary();
            response.put("success", true);
            response.put("message", "每日摘要发送成功");
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "每日摘要发送失败: " + e.getMessage());
        }
        return ResponseEntity.ok(response);
    }

    /**
     * 清空低优先级信号缓存
     */
    @PostMapping("/clear-cache")
    public ResponseEntity<Map<String, Object>> clearCache() {
        Map<String, Object> response = new HashMap<>();
        signalFilterService.clearLowPrioritySignals();
        response.put("success", true);
        response.put("message", "低优先级信号缓存已清空");
        return ResponseEntity.ok(response);
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "healthy");
        response.put("service", "SignalFilterService");
        response.put("statistics", signalFilterService.getStatistics());
        return ResponseEntity.ok(response);
    }
}