package com.example.tradewise.controller;

import com.example.tradewise.entity.Order;
import com.example.tradewise.service.CopyTradeSignalEvaluator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * 跟单信号质量评估测试控制器
 */
@RestController
@RequestMapping("/api/copy-trade-evaluation")
public class CopyTradeEvaluationController {

    @Autowired
    private CopyTradeSignalEvaluator signalEvaluator;

    /**
     * 测试信号质量评估
     */
    @PostMapping("/test/{symbol}")
    public CopyTradeSignalEvaluator.SignalQualityAssessment testSignalEvaluation(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "BUY") String side,
            @RequestParam(defaultValue = "LIMIT") String type,
            @RequestParam(defaultValue = "LONG") String positionSide) {

        // 创建测试订单
        Order testOrder = new Order(
                "test-" + System.currentTimeMillis(),
                "test-trader",
                "测试交易员",
                symbol,
                symbol.substring(0, symbol.length() - 4), // baseAsset
                symbol.substring(symbol.length() - 4),    // quoteAsset
                side,
                type,
                positionSide,
                BigDecimal.valueOf(1.0),
                BigDecimal.valueOf(50000.0),
                BigDecimal.ZERO,
                System.currentTimeMillis(),
                System.currentTimeMillis()
        );

        return signalEvaluator.evaluateSignalQuality(testOrder);
    }

    /**
     * 获取服务健康状态
     */
    @GetMapping("/health")
    public String health() {
        return "跟单信号质量评估服务运行正常";
    }
}