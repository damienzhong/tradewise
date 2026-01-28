package com.damien.tradewise.admin.service;

import com.damien.tradewise.admin.entity.TwTraderConfig;
import com.damien.tradewise.admin.mapper.TwTraderConfigMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TwTraderConfigService {
    
    @Autowired
    private TwTraderConfigMapper traderConfigMapper;
    
    public Map<String, Object> getTraderList(int page, int pageSize, String keyword) {
        int offset = (page - 1) * pageSize;
        
        List<TwTraderConfig> traders;
        int total;
        
        if (keyword != null && !keyword.trim().isEmpty()) {
            traders = traderConfigMapper.searchTraders(keyword, offset, pageSize);
            total = traderConfigMapper.countSearchTraders(keyword);
        } else {
            traders = traderConfigMapper.findAllWithPagination(offset, pageSize);
            total = traderConfigMapper.countAll();
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("traders", traders);
        result.put("total", total);
        result.put("page", page);
        result.put("pageSize", pageSize);
        result.put("totalPages", (int) Math.ceil((double) total / pageSize));
        
        return result;
    }
    
    public TwTraderConfig getTraderDetail(Long id) {
        return traderConfigMapper.findById(id);
    }
    
    public void addTrader(TwTraderConfig trader) {
        // 检查Portfolio ID是否已存在
        if (traderConfigMapper.findByPortfolioId(trader.getPortfolioId()) != null) {
            throw new RuntimeException("该Portfolio ID已存在");
        }
        traderConfigMapper.insert(trader);
    }
    
    public void updateTrader(TwTraderConfig trader) {
        TwTraderConfig existing = traderConfigMapper.findById(trader.getId());
        if (existing == null) {
            throw new RuntimeException("交易员不存在");
        }
        
        // 如果修改了Portfolio ID，检查新ID是否已被使用
        if (!existing.getPortfolioId().equals(trader.getPortfolioId())) {
            if (traderConfigMapper.findByPortfolioId(trader.getPortfolioId()) != null) {
                throw new RuntimeException("该Portfolio ID已被使用");
            }
        }
        
        traderConfigMapper.update(trader);
    }
    
    public void toggleTraderStatus(Long id, boolean enabled) {
        traderConfigMapper.updateEnabled(id, enabled);
    }
    
    public void deleteTrader(Long id) {
        traderConfigMapper.deleteById(id);
    }
    
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalTraders", traderConfigMapper.countAll());
        stats.put("enabledTraders", traderConfigMapper.countByEnabled(true));
        stats.put("todayOrders", traderConfigMapper.sumTodayOrders());
        return stats;
    }
}
