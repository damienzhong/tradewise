package com.example.tradewise.controller;

import com.example.tradewise.service.MarketAnalysisService;
import com.example.tradewise.service.MarketAnalysisService.TradingSignal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 市场分析控制器
 * 提供市场分析和交易信号相关的API端点
 */
@RestController
@RequestMapping("/api/market-analysis")
public class MarketAnalysisController {
    
    @Autowired
    private MarketAnalysisService marketAnalysisService;
    
    /**
     * 手动触发特定交易对的市场分析
     */
    @PostMapping("/analyze/{symbol}")
    public ResponseEntity<String> analyzeSymbol(@PathVariable String symbol) {
        try {
            marketAnalysisService.analyzeSymbol(symbol.toUpperCase());
            return ResponseEntity.ok("已启动对 " + symbol.toUpperCase() + " 的市场分析");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("分析失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取健康状态
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Market Analysis Service is running");
    }
}