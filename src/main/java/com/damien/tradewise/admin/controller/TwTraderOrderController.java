package com.damien.tradewise.admin.controller;

import com.damien.tradewise.admin.service.TwTraderOrderService;
import com.damien.tradewise.common.entity.TwTraderOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/admin/orders")
public class TwTraderOrderController {

    @Autowired
    private TwTraderOrderService orderService;

    @GetMapping("/list")
    public Map<String, Object> list(@RequestParam(required = false) Long traderId,
                                     @RequestParam(required = false) String symbol,
                                     @RequestParam(required = false) String side,
                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                     @RequestParam(defaultValue = "1") int page,
                                     @RequestParam(defaultValue = "20") int size) {
        return orderService.getOrdersByPage(traderId, symbol, side, date, page, size);
    }

    @GetMapping("/detail/{id}")
    public TwTraderOrder detail(@PathVariable Long id) {
        return orderService.getOrderById(id);
    }

    @GetMapping("/statistics")
    public Map<String, Object> statistics() {
        return orderService.getStatistics();
    }
}
