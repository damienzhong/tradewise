package com.example.tradewise.mapper;

import com.example.tradewise.entity.Order;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface OrderMapper {

    @Insert("INSERT INTO orders (external_order_id, trader_id, trader_name, symbol, base_asset, quote_asset, " +
            "side, type, position_side, executed_qty, avg_price, total_pnl, order_update_time, order_time) " +
            "VALUES (#{externalOrderId}, #{traderId}, #{traderName}, #{symbol}, #{baseAsset}, #{quoteAsset}, " +
            "#{side}, #{type}, #{positionSide}, #{executedQty}, #{avgPrice}, #{totalPnl}, #{orderUpdateTime}, #{orderTime}) " +
            "ON DUPLICATE KEY UPDATE " +
            "trader_name = VALUES(trader_name), " +
            "symbol = VALUES(symbol), " +
            "base_asset = VALUES(base_asset), " +
            "quote_asset = VALUES(quote_asset), " +
            "side = VALUES(side), " +
            "type = VALUES(type), " +
            "position_side = VALUES(position_side), " +
            "executed_qty = VALUES(executed_qty), " +
            "avg_price = VALUES(avg_price), " +
            "total_pnl = VALUES(total_pnl), " +
            "order_update_time = VALUES(order_update_time)")
    void insertOrUpdate(Order order);

    @Select("SELECT * FROM orders WHERE trader_id = #{traderId} AND order_time >= #{sinceTime} ORDER BY order_time DESC")
    List<Order> findByTraderIdSince(@Param("traderId") String traderId, @Param("sinceTime") Long sinceTime);

    @Select("SELECT * FROM orders WHERE external_order_id = #{externalOrderId}")
    Order findByExternalOrderId(@Param("externalOrderId") String externalOrderId);

    @Select("SELECT * FROM orders ORDER BY order_time DESC LIMIT #{limit}")
    List<Order> findRecentOrders(@Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM orders")
    int countAll();

    @Delete("DELETE FROM orders WHERE order_time < #{beforeTime}")
    int deleteOldOrders(@Param("beforeTime") Long beforeTime);

    @Select("SELECT * FROM orders WHERE trader_id = #{traderId} ORDER BY order_time ASC")
    List<Order> findByTraderId(@Param("traderId") String traderId);

    @Select("SELECT * FROM orders ORDER BY order_time ASC")
    List<Order> findAll();
}