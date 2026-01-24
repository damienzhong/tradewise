package com.example.tradewise.service;

import com.example.tradewise.entity.SymbolConfig;
import com.example.tradewise.mapper.SymbolConfigMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SymbolConfigService {

    private static final Logger logger = LoggerFactory.getLogger(SymbolConfigService.class);

    @Autowired
    private SymbolConfigMapper symbolConfigMapper;

    public List<SymbolConfig> getAllEnabledSymbols() {
        return symbolConfigMapper.findAllEnabled();
    }

    public List<String> getEnabledSymbolNames() {
        return symbolConfigMapper.findAllEnabled().stream()
                .map(SymbolConfig::getSymbol)
                .collect(Collectors.toList());
    }

    public List<SymbolConfig> getAllSymbols() {
        return symbolConfigMapper.findAll();
    }

    public boolean addSymbol(SymbolConfig symbolConfig) {
        try {
            SymbolConfig existing = symbolConfigMapper.findBySymbol(symbolConfig.getSymbol());
            if (existing != null) {
                logger.warn("币对已存在: {}", symbolConfig.getSymbol());
                return false;
            }
            int result = symbolConfigMapper.insert(symbolConfig);
            logger.info("添加币对成功: {}", symbolConfig.getSymbol());
            return result > 0;
        } catch (Exception e) {
            logger.error("添加币对失败", e);
            return false;
        }
    }

    public boolean updateSymbol(SymbolConfig symbolConfig) {
        try {
            int result = symbolConfigMapper.update(symbolConfig);
            logger.info("更新币对成功: {}", symbolConfig.getSymbol());
            return result > 0;
        } catch (Exception e) {
            logger.error("更新币对失败", e);
            return false;
        }
    }

    public boolean deleteSymbol(Long id) {
        try {
            int result = symbolConfigMapper.deleteById(id);
            logger.info("删除币对成功，ID: {}", id);
            return result > 0;
        } catch (Exception e) {
            logger.error("删除币对失败", e);
            return false;
        }
    }
}
