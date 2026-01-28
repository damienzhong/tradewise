package com.damien.tradewise.admin.service;

import com.damien.tradewise.common.entity.TwTraderOrder;
import com.damien.tradewise.common.mapper.TwTraderOrderMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TwTraderOrderService {

    @Autowired
    private TwTraderOrderMapper orderMapper;

    public Map<String, Object> getOrdersByPage(Long traderId, String symbol, String side, LocalDate date, int page, int size) {
        int offset = (page - 1) * size;
        List<TwTraderOrder> orders = orderMapper.selectByPage(traderId, symbol, side, date, offset, size);
        int total = orderMapper.countByCondition(traderId, symbol, side, date);
        
        Map<String, Object> result = new HashMap<>();
        result.put("content", orders);
        result.put("totalElements", total);
        result.put("totalPages", (int) Math.ceil((double) total / size));
        result.put("size", size);
        result.put("number", page);
        result.put("last", page >= Math.ceil((double) total / size));
        return result;
    }

    public TwTraderOrder getOrderById(Long id) {
        return orderMapper.selectById(id);
    }

    public Map<String, Object> getStatistics() {
        return orderMapper.selectStatistics();
    }
}
