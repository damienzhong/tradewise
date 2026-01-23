package com.example.tradewise.service;

import com.example.tradewise.entity.TraderConfig;
import com.example.tradewise.mapper.TraderConfigMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TraderConfigService {

    @Autowired
    private TraderConfigMapper traderConfigMapper;

    public List<TraderConfig> getAllEnabledTraderConfigs() {
        return traderConfigMapper.findAllEnabled();
    }

    public TraderConfig addTraderConfig(TraderConfig traderConfig) {
        // 检查交易员ID是否已存在
        TraderConfig existingConfig = traderConfigMapper.findByTraderId(traderConfig.getTraderId());
        if (existingConfig != null) {
            throw new RuntimeException("交易员ID已存在: " + traderConfig.getTraderId());
        }
        
        if (traderConfig.getEnabled() == null) {
            traderConfig.setEnabled(true);
        }
        
        traderConfigMapper.insert(traderConfig);
        return traderConfig;
    }

    public void updateTraderConfig(TraderConfig traderConfig) {
        traderConfigMapper.update(traderConfig);
    }

    public void deleteTraderConfigByTraderId(String traderId) {
        traderConfigMapper.deleteByTraderId(traderId);
    }

    public TraderConfig findByTraderId(String traderId) {
        return traderConfigMapper.findByTraderId(traderId);
    }
}