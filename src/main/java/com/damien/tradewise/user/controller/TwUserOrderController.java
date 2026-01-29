package com.damien.tradewise.user.controller;

import com.damien.tradewise.common.entity.TwTraderOrder;
import com.damien.tradewise.user.service.TwUserOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.time.LocalDate;
import java.util.HashMap;
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
                                     @RequestParam(defaultValue = "10") int size,
                                     HttpSession session) {
        Long userId = (Long) session.getAttribute("tw_user_id");
        if (userId == null) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "请先登录");
            return result;
        }
        return orderService.getOrdersByUserSubscriptions(userId, traderId, symbol, side, date, page, size);
    }

    @GetMapping("/detail/{id}")
    public Map<String, Object> detail(@PathVariable Long id, HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        Long userId = (Long) session.getAttribute("tw_user_id");
        if (userId == null) {
            result.put("success", false);
            result.put("message", "请先登录");
            return result;
        }
        TwTraderOrder order = orderService.getOrderById(id, userId);
        if (order != null) {
            result.put("success", true);
            result.put("order", order);
        } else {
            result.put("success", false);
            result.put("message", "订单不存在或无权访问");
        }
        return result;
    }
}
