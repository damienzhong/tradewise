package com.damien.tradewise.admin.mapper;

import com.damien.tradewise.admin.entity.TwTraderConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface TwTraderConfigMapper {
    
    List<TwTraderConfig> findAll();
    
    List<TwTraderConfig> findAllWithPagination(@Param("offset") int offset, @Param("limit") int limit);
    
    List<TwTraderConfig> searchTraders(@Param("keyword") String keyword, @Param("offset") int offset, @Param("limit") int limit);
    
    TwTraderConfig findById(@Param("id") Long id);
    
    TwTraderConfig findByPortfolioId(@Param("portfolioId") String portfolioId);
    
    int countAll();
    
    int countSearchTraders(@Param("keyword") String keyword);
    
    int countByEnabled(@Param("enabled") Boolean enabled);
    
    int sumTodayOrders();
    
    int insert(TwTraderConfig trader);
    
    int update(TwTraderConfig trader);
    
    int updateEnabled(@Param("id") Long id, @Param("enabled") Boolean enabled);
    
    int deleteById(@Param("id") Long id);
    
    List<TwTraderConfig> selectEnabledTraders();
    
    List<TwTraderConfig> selectEnabledTraders(@Param("offset") int offset, @Param("limit") int limit);
    
    int countEnabledTraders();
    
    int updateOrderStatistics(@Param("id") Long id, @Param("newOrderCount") int newOrderCount, @Param("lastOrderTime") LocalDateTime lastOrderTime);
    
    int updateLastCheckTime(@Param("id") Long id, @Param("lastCheckTime") LocalDateTime lastCheckTime);
}
