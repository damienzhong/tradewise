package com.damien.tradewise.user.controller;

import com.damien.tradewise.common.entity.TwTraderOrder;
import com.damien.tradewise.user.service.TwUserOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/user/orders")
public class TwUserOrderController {

    @Autowired
    private TwUserOrderService orderService;

    @GetMapping("/list")
    public Map<String, Object> list(@RequestParam(required = false) Long traderId,
                                     @RequestParam(required = false) String symbol,
                                     @RequestParam(required = false) String side,
                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                     @RequestParam(defaultValue = "1") int page,
                                     @RequestParam(defaultValue = "10") int size) {
        // TODO: 从session获取当前登录用户ID
        Long userId = 1L; // 临时硬编码
        return orderService.getOrdersByUserSubscriptions(userId, traderId, symbol, side, date, page, size);
    }

    @GetMapping("/detail/{id}")
    public TwTraderOrder detail(@PathVariable Long id) {
        // TODO: 从session获取当前登录用户ID
        Long userId = 1L; // 临时硬编码
        return orderService.getOrderById(id, userId);
    }
}
