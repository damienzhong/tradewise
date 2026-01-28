package com.damien.tradewise.common.mapper;

import com.damien.tradewise.common.entity.TwTraderOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Mapper
public interface TwTraderOrderMapper {
    
    List<TwTraderOrder> selectByPage(@Param("traderId") Long traderId,
                                      @Param("symbol") String symbol,
                                      @Param("side") String side,
                                      @Param("date") LocalDate date,
                                      @Param("offset") int offset,
                                      @Param("limit") int limit);
    
    int countByCondition(@Param("traderId") Long traderId,
                         @Param("symbol") String symbol,
                         @Param("side") String side,
                         @Param("date") LocalDate date);
    
    List<TwTraderOrder> selectByUserSubscriptions(@Param("userId") Long userId,
                                                   @Param("traderId") Long traderId,
                                                   @Param("symbol") String symbol,
                                                   @Param("side") String side,
                                                   @Param("date") LocalDate date,
                                                   @Param("offset") int offset,
                                                   @Param("limit") int limit);
    
    int countByUserSubscriptions(@Param("userId") Long userId,
                                  @Param("traderId") Long traderId,
                                  @Param("symbol") String symbol,
                                  @Param("side") String side,
                                  @Param("date") LocalDate date);
    
    TwTraderOrder selectById(@Param("id") Long id);
    
    Map<String, Object> selectStatistics();
    
    int insert(TwTraderOrder order);
    
    int existsByExchangeAndOrderId(@Param("exchange") String exchange, @Param("orderId") String orderId);
}
