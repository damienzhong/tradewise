package com.example.tradewise.controller;

import com.example.tradewise.service.TraderPerformanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 交易员表现分析控制器
 * 提供交易员绩效统计和分析的API
 */
@RestController
@RequestMapping("/api/trader-performance")
public class TraderPerformanceController {

    @Autowired
    private TraderPerformanceService traderPerformanceService;

    /**
     * 获取指定交易员的表现统计
     */
    @GetMapping("/stats/{traderId}")
    public ResponseEntity<TraderPerformanceService.TraderPerformanceStats> getTraderPerformance(
            @PathVariable String traderId) {
        TraderPerformanceService.TraderPerformanceStats stats = 
            traderPerformanceService.getTraderPerformanceStats(traderId);
        return ResponseEntity.ok(stats);
    }

    /**
     * 获取所有交易员的表现统计
     */
    @GetMapping("/all-stats")
    public ResponseEntity<List<TraderPerformanceService.TraderPerformanceStats>> getAllTradersPerformance() {
        List<TraderPerformanceService.TraderPerformanceStats> allStats = 
            traderPerformanceService.getAllTradersPerformanceStats();
        return ResponseEntity.ok(allStats);
    }

    /**
     * 获取高风险交易员列表
     */
    @GetMapping("/high-risk")
    public ResponseEntity<List<TraderPerformanceService.TraderPerformanceStats>> getHighRiskTraders() {
        List<TraderPerformanceService.TraderPerformanceStats> allStats = 
            traderPerformanceService.getAllTradersPerformanceStats();
        
        List<TraderPerformanceService.TraderPerformanceStats> highRiskTraders = allStats.stream()
            .filter(stats -> "HIGH".equals(stats.getRiskLevel()))
            .collect(java.util.stream.Collectors.toList());
        
        return ResponseEntity.ok(highRiskTraders);
    }

    /**
     * 获取低风险交易员列表
     */
    @GetMapping("/low-risk")
    public ResponseEntity<List<TraderPerformanceService.TraderPerformanceStats>> getLowRiskTraders() {
        List<TraderPerformanceService.TraderPerformanceStats> allStats = 
            traderPerformanceService.getAllTradersPerformanceStats();
        
        List<TraderPerformanceService.TraderPerformanceStats> lowRiskTraders = allStats.stream()
            .filter(stats -> "LOW".equals(stats.getRiskLevel()))
            .collect(java.util.stream.Collectors.toList());
        
        return ResponseEntity.ok(lowRiskTraders);
    }

    /**
     * 获取盈利能力最强的交易员
     */
    @GetMapping("/top-profitable")
    public ResponseEntity<List<TraderPerformanceService.TraderPerformanceStats>> getTopProfitableTraders(
            @RequestParam(defaultValue = "5") int limit) {
        List<TraderPerformanceService.TraderPerformanceStats> allStats = 
            traderPerformanceService.getAllTradersPerformanceStats();
        
        List<TraderPerformanceService.TraderPerformanceStats> topTraders = allStats.stream()
            .sorted((s1, s2) -> s2.getNetProfit().compareTo(s1.getNetProfit()))
            .limit(limit)
            .collect(java.util.stream.Collectors.toList());
        
        return ResponseEntity.ok(topTraders);
    }
}