package com.damien.tradewise.user.service;

import com.damien.tradewise.common.entity.TwTraderOrder;
import com.damien.tradewise.common.mapper.TwTraderOrderMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TwUserOrderService {

    @Autowired
    private TwTraderOrderMapper orderMapper;

    public Map<String, Object> getOrdersByUserSubscriptions(Long userId, Long traderId, String symbol, String side, LocalDate date, int page, int size) {
        int offset = (page - 1) * size;
        List<TwTraderOrder> orders = orderMapper.selectByUserSubscriptions(userId, traderId, symbol, side, date, offset, size);
        int total = orderMapper.countByUserSubscriptions(userId, traderId, symbol, side, date);
        
        Map<String, Object> result = new HashMap<>();
        result.put("content", orders);
        result.put("totalElements", total);
        result.put("totalPages", (int) Math.ceil((double) total / size));
        result.put("size", size);
        result.put("number", page);
        result.put("last", page >= Math.ceil((double) total / size));
        return result;
    }

    public TwTraderOrder getOrderById(Long id, Long userId) {
        // 验证用户是否订阅了该订单的交易员
        TwTraderOrder order = orderMapper.selectById(id);
        if (order != null) {
            int count = orderMapper.countByUserSubscriptions(userId, order.getTraderId(), null, null, null);
            if (count > 0) {
                return order;
            }
        }
        return null;
    }
}
