package com.example.tradewise.controller;

import com.example.tradewise.service.SignalMonitorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/diagnostic")
public class DiagnosticController {

    @Autowired
    private SignalMonitorService signalMonitorService;

    @GetMapping("/stats/summary")
    public Map<String, Object> getStatsSummary() {
        return signalMonitorService.getSummary();
    }

    @GetMapping("/stats/all")
    public Map<String, SignalMonitorService.SignalStats> getAllStats() {
        return signalMonitorService.getAllStats();
    }

    @GetMapping("/stats/{symbol}")
    public SignalMonitorService.SignalStats getSymbolStats(@PathVariable String symbol) {
        return signalMonitorService.getStats(symbol);
    }

    @GetMapping("/alerts")
    public Map<String, Object> checkAlerts() {
        Map<String, Object> result = new HashMap<>();
        List<String> alerts = signalMonitorService.checkAlerts();
        result.put("alertCount", alerts.size());
        result.put("alerts", alerts);
        result.put("hasAlerts", !alerts.isEmpty());
        return result;
    }

    @PostMapping("/stats/reset")
    public Map<String, Object> resetStats() {
        signalMonitorService.resetStats();
        Map<String, Object> result = new HashMap<>();
        result.put("status", "success");
        result.put("message", "统计数据已重置");
        return result;
    }
}
