package com.example.tradewise.controller;

import com.example.tradewise.entity.TraderConfig;
import com.example.tradewise.mapper.TraderConfigMapper;
import com.example.tradewise.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    
    @Autowired
    private OrderService orderService;
    
    /**
     * 手动触发订单检查（用于测试）
     */
    @GetMapping("/check")
    public ResponseEntity<String> checkOrdersManually() {
        // 注意：这里需要获取配置的交易员信息，我们暂时返回提示信息
        return ResponseEntity.ok("手动检查功能需要在实际实现中获取配置的交易员信息");
    }
    
    /**
     * 获取API健康状态
     */
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Service is running");
    }
}