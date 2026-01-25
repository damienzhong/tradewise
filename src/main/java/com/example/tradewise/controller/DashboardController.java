package com.example.tradewise.controller;

import com.example.tradewise.mapper.OrderMapper;
import com.example.tradewise.mapper.SignalMapper;
import com.example.tradewise.mapper.TraderConfigMapper;
import com.example.tradewise.entity.Signal;
import com.example.tradewise.service.SystemHealthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Autowired
    private SignalMapper signalMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private TraderConfigMapper traderConfigMapper;

    @Autowired
    private SystemHealthService systemHealthService;

    /**
     * 获取今日统计数据
     */
    @GetMapping("/today-stats")
    public ResponseEntity<Map<String, Object>> getTodayStats() {
        LocalDateTime todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        
        Map<String, Object> stats = new HashMap<>();
        
        // 信号统计
        Map<String, Object> signalStats = signalMapper.getStatistics(todayStart);
        stats.put("todaySignals", signalStats.get("total"));
        stats.put("buySignals", signalStats.get("buyCount"));
        stats.put("sellSignals", signalStats.get("sellCount"));
        stats.put("winRate", calculateWinRate(signalStats));
        
        // 订单统计
        stats.put("todayOrders", orderMapper.countAll());
        
        // 交易员统计
        stats.put("activeTraders", traderConfigMapper.findAllEnabled().size());
        
        // 系统健康状态
        SystemHealthService.SystemHealthStatus health = systemHealthService.getSystemHealthStatus();
        stats.put("systemHealth", health.getOverallHealth());
        stats.put("successRate", health.getSuccessRate());
        
        return ResponseEntity.ok(stats);
    }

    /**
     * 获取趋势数据（近7天或30天）
     */
    @GetMapping("/trend")
    public ResponseEntity<Map<String, Object>> getTrend(@RequestParam(defaultValue = "7") int days) {
        LocalDateTime startTime = LocalDateTime.now().minusDays(days);
        
        Map<String, Object> trend = new HashMap<>();
        List<Map<String, Object>> dailyData = new ArrayList<>();
        
        for (int i = days - 1; i >= 0; i--) {
            LocalDateTime dayStart = LocalDateTime.now().minusDays(i).withHour(0).withMinute(0).withSecond(0);
            LocalDateTime dayEnd = dayStart.plusDays(1);
            
            int signalCount = signalMapper.countByConditions(null, null, null, dayStart, dayEnd);
            
            Map<String, Object> dayData = new HashMap<>();
            dayData.put("date", dayStart.toLocalDate().toString());
            dayData.put("signals", signalCount);
            dailyData.add(dayData);
        }
        
        trend.put("dailyData", dailyData);
        trend.put("period", days + " days");
        
        return ResponseEntity.ok(trend);
    }

    /**
     * 获取TOP交易对
     */
    @GetMapping("/top-symbols")
    public ResponseEntity<List<Map<String, Object>>> getTopSymbols(@RequestParam(defaultValue = "5") int limit) {
        LocalDateTime last7Days = LocalDateTime.now().minusDays(7);
        List<Map<String, Object>> topSymbols = signalMapper.getTopSymbols(last7Days, limit);
        return ResponseEntity.ok(topSymbols);
    }

    /**
     * 获取最新信号列表
     */
    @GetMapping("/recent-signals")
    public ResponseEntity<List<Signal>> getRecentSignals(@RequestParam(defaultValue = "10") int limit) {
        List<Signal> recentSignals = signalMapper.findRecent(limit);
        return ResponseEntity.ok(recentSignals);
    }

    /**
     * 获取综合概览数据
     */
    @GetMapping("/overview")
    public ResponseEntity<Map<String, Object>> getOverview() {
        Map<String, Object> overview = new HashMap<>();
        
        // 今日统计
        LocalDateTime todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        Map<String, Object> todaySignalStats = signalMapper.getStatistics(todayStart);
        
        // 本周统计
        LocalDateTime weekStart = LocalDateTime.now().minusDays(7);
        Map<String, Object> weekSignalStats = signalMapper.getStatistics(weekStart);
        
        // 本月统计
        LocalDateTime monthStart = LocalDateTime.now().minusDays(30);
        Map<String, Object> monthSignalStats = signalMapper.getStatistics(monthStart);
        
        overview.put("today", todaySignalStats);
        overview.put("week", weekSignalStats);
        overview.put("month", monthSignalStats);
        
        // 系统状态
        SystemHealthService.SystemHealthStatus health = systemHealthService.getSystemHealthStatus();
        Map<String, Object> systemStatus = new HashMap<>();
        systemStatus.put("health", health.getOverallHealth());
        systemStatus.put("uptime", health.getUptimeMinutes());
        systemStatus.put("successRate", health.getSuccessRate());
        overview.put("system", systemStatus);
        
        return ResponseEntity.ok(overview);
    }

    private double calculateWinRate(Map<String, Object> stats) {
        Object closedCountObj = stats.get("closedCount");
        Object winCountObj = stats.get("winCount");
        
        if (closedCountObj == null || winCountObj == null) {
            return 0.0;
        }
        
        long closedCount = ((Number) closedCountObj).longValue();
        long winCount = ((Number) winCountObj).longValue();
        
        if (closedCount == 0) {
            return 0.0;
        }
        
        return (double) winCount / closedCount * 100;
    }
}
