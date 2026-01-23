package com.example.tradewise.mapper;

import com.example.tradewise.entity.TraderConfig;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface TraderConfigMapper {

    @Insert("INSERT INTO trader_config (trader_id, portfolio_id, name, enabled) VALUES (#{traderId}, #{portfolioId}, #{name}, #{enabled})")
    void insert(TraderConfig traderConfig);

    @Update("UPDATE trader_config SET portfolio_id = #{portfolioId}, name = #{name}, enabled = #{enabled} WHERE trader_id = #{traderId}")
    void update(TraderConfig traderConfig);

    @Select("SELECT * FROM trader_config WHERE enabled = 1")
    List<TraderConfig> findAllEnabled();

    @Select("SELECT * FROM trader_config WHERE trader_id = #{traderId}")
    TraderConfig findByTraderId(@Param("traderId") String traderId);

    @Delete("DELETE FROM trader_config WHERE trader_id = #{traderId}")
    void deleteByTraderId(@Param("traderId") String traderId);
}