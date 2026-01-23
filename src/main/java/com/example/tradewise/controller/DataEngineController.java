package com.example.tradewise.controller;

import com.example.tradewise.service.DataEngine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 数据引擎控制器 - 管理数据引擎相关的API
 */
@RestController
@RequestMapping("/api/data-engine")
public class DataEngineController {

    @Autowired
    private DataEngine dataEngine;

    /**
     * 更新带单员持仓方向
     *
     * @param traderId 交易员ID
     * @param symbol 交易对
     * @param direction 持仓方向 (LONG/SHORT/NEUTRAL)
     * @return 响应结果
     */
    @PostMapping("/trader-position")
    public ResponseEntity<String> updateTraderPosition(
            @RequestParam String traderId,
            @RequestParam String symbol,
            @RequestParam String direction) {
        
        DataEngine.SignalDirection signalDirection;
        try {
            signalDirection = DataEngine.SignalDirection.valueOf(direction.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body("无效的方向参数: " + direction + ". 有效值: LONG, SHORT, NEUTRAL");
        }

        dataEngine.updateTraderPosition(traderId, symbol, signalDirection);
        
        return ResponseEntity.ok("成功更新交易员 " + traderId + " 在 " + symbol + " 的持仓方向为 " + direction);
    }

    /**
     * 手动执行数据引擎
     *
     * @return 响应结果
     */
    @PostMapping("/execute")
    public ResponseEntity<String> executeDataEngine() {
        dataEngine.execute();
        return ResponseEntity.ok("数据引擎执行完成");
    }

    /**
     * 清理过期缓存
     *
     * @return 响应结果
     */
    @PostMapping("/cleanup-cache")
    public ResponseEntity<String> cleanupCache() {
        dataEngine.cleanupExpiredCache();
        return ResponseEntity.ok("缓存清理完成");
    }
}