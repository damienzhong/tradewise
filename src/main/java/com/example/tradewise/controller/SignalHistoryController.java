package com.example.tradewise.controller;

import com.example.tradewise.entity.Signal;
import com.example.tradewise.mapper.SignalMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/signals")
public class SignalHistoryController {

    @Autowired
    private SignalMapper signalMapper;

    /**
     * 查询历史信号（支持分页和筛选）
     */
    @GetMapping("/history")
    public ResponseEntity<Map<String, Object>> getSignalHistory(
            @RequestParam(required = false) String symbol,
            @RequestParam(required = false) String signalType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        
        int offset = (page - 1) * pageSize;
        
        List<Signal> signals = signalMapper.findByConditions(
            symbol, signalType, status, startTime, endTime, offset, pageSize);
        
        int total = signalMapper.countByConditions(
            symbol, signalType, status, startTime, endTime);
        
        Map<String, Object> response = new HashMap<>();
        response.put("data", signals);
        response.put("total", total);
        response.put("page", page);
        response.put("pageSize", pageSize);
        response.put("totalPages", (int) Math.ceil((double) total / pageSize));
        
        return ResponseEntity.ok(response);
    }

    /**
     * 获取信号详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<Signal> getSignalDetail(@PathVariable Long id) {
        Signal signal = signalMapper.findById(id);
        if (signal == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(signal);
    }

    /**
     * 获取信号统计数据
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics(
            @RequestParam(defaultValue = "30") int days) {
        
        LocalDateTime startTime = LocalDateTime.now().minusDays(days);
        Map<String, Object> stats = signalMapper.getStatistics(startTime);
        
        // 计算胜率
        Object closedCountObj = stats.get("closedCount");
        Object winCountObj = stats.get("winCount");
        
        if (closedCountObj != null && winCountObj != null) {
            long closedCount = ((Number) closedCountObj).longValue();
            long winCount = ((Number) winCountObj).longValue();
            
            double winRate = closedCount > 0 ? (double) winCount / closedCount * 100 : 0.0;
            stats.put("winRate", String.format("%.2f%%", winRate));
        } else {
            stats.put("winRate", "0.00%");
        }
        
        stats.put("period", days + " days");
        
        return ResponseEntity.ok(stats);
    }

    /**
     * 手动关闭信号
     */
    @PostMapping("/{id}/close")
    public ResponseEntity<Map<String, Object>> closeSignal(
            @PathVariable Long id,
            @RequestParam BigDecimal finalPrice,
            @RequestParam(required = false) String notes) {
        
        Signal signal = signalMapper.findById(id);
        if (signal == null) {
            return ResponseEntity.notFound().build();
        }
        
        // 计算盈亏百分比
        BigDecimal pnlPercentage = calculatePnl(signal.getPrice(), finalPrice, signal.getSignalType());
        
        signal.setStatus("CLOSED");
        signal.setOutcomeTime(LocalDateTime.now());
        signal.setFinalPrice(finalPrice);
        signal.setPnlPercentage(pnlPercentage);
        signal.setNotes(notes);
        
        signalMapper.updateOutcome(signal);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "信号已关闭");
        response.put("pnlPercentage", pnlPercentage);
        
        return ResponseEntity.ok(response);
    }

    /**
     * 获取最近的信号
     */
    @GetMapping("/recent")
    public ResponseEntity<List<Signal>> getRecentSignals(
            @RequestParam(defaultValue = "10") int limit) {
        List<Signal> signals = signalMapper.findRecent(limit);
        return ResponseEntity.ok(signals);
    }

    /**
     * 按交易对获取信号
     */
    @GetMapping("/by-symbol/{symbol}")
    public ResponseEntity<List<Signal>> getSignalsBySymbol(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "20") int limit) {
        
        List<Signal> signals = signalMapper.findByConditions(
            symbol, null, null, null, null, 0, limit);
        
        return ResponseEntity.ok(signals);
    }

    /**
     * 计算盈亏百分比
     */
    private BigDecimal calculatePnl(BigDecimal entryPrice, BigDecimal exitPrice, String signalType) {
        if (entryPrice == null || exitPrice == null || entryPrice.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal priceDiff = exitPrice.subtract(entryPrice);
        BigDecimal pnl = priceDiff.divide(entryPrice, 4, BigDecimal.ROUND_HALF_UP)
                                   .multiply(new BigDecimal("100"));
        
        // 如果是卖出信号，盈亏方向相反
        if ("SELL".equals(signalType)) {
            pnl = pnl.negate();
        }
        
        return pnl;
    }
}
