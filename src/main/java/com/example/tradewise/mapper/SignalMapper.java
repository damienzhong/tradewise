package com.example.tradewise.mapper;

import com.example.tradewise.entity.Signal;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
@Repository
public interface SignalMapper {

    @Insert("INSERT INTO signal_performance (symbol, signal_time, signal_type, indicator, price, " +
            "stop_loss, take_profit, score, confidence, reason, status) " +
            "VALUES (#{symbol}, #{signalTime}, #{signalType}, #{indicator}, #{price}, " +
            "#{stopLoss}, #{takeProfit}, #{score}, #{confidence}, #{reason}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Signal signal);

    @Select("SELECT * FROM signal_performance WHERE id = #{id}")
    Signal findById(@Param("id") Long id);

    @Select("<script>" +
            "SELECT * FROM signal_performance WHERE 1=1 " +
            "<if test='symbol != null'>AND symbol = #{symbol}</if> " +
            "<if test='signalType != null'>AND signal_type = #{signalType}</if> " +
            "<if test='status != null'>AND status = #{status}</if> " +
            "<if test='startTime != null'>AND signal_time &gt;= #{startTime}</if> " +
            "<if test='endTime != null'>AND signal_time &lt;= #{endTime}</if> " +
            "ORDER BY signal_time DESC " +
            "LIMIT #{offset}, #{limit}" +
            "</script>")
    List<Signal> findByConditions(@Param("symbol") String symbol,
                                   @Param("signalType") String signalType,
                                   @Param("status") String status,
                                   @Param("startTime") LocalDateTime startTime,
                                   @Param("endTime") LocalDateTime endTime,
                                   @Param("offset") int offset,
                                   @Param("limit") int limit);

    @Select("<script>" +
            "SELECT COUNT(*) FROM signal_performance WHERE 1=1 " +
            "<if test='symbol != null'>AND symbol = #{symbol}</if> " +
            "<if test='signalType != null'>AND signal_type = #{signalType}</if> " +
            "<if test='status != null'>AND status = #{status}</if> " +
            "<if test='startTime != null'>AND signal_time &gt;= #{startTime}</if> " +
            "<if test='endTime != null'>AND signal_time &lt;= #{endTime}</if>" +
            "</script>")
    int countByConditions(@Param("symbol") String symbol,
                          @Param("signalType") String signalType,
                          @Param("status") String status,
                          @Param("startTime") LocalDateTime startTime,
                          @Param("endTime") LocalDateTime endTime);

    @Select("SELECT * FROM signal_performance ORDER BY signal_time DESC LIMIT #{limit}")
    List<Signal> findRecent(@Param("limit") int limit);

    @Update("UPDATE signal_performance SET status = #{status}, outcome_time = #{outcomeTime}, " +
            "final_price = #{finalPrice}, pnl_percentage = #{pnlPercentage}, notes = #{notes} " +
            "WHERE id = #{id}")
    void updateOutcome(Signal signal);

    @Select("SELECT COUNT(*) as total, " +
            "SUM(CASE WHEN signal_type = 'BUY' THEN 1 ELSE 0 END) as buyCount, " +
            "SUM(CASE WHEN signal_type = 'SELL' THEN 1 ELSE 0 END) as sellCount, " +
            "SUM(CASE WHEN status = 'CLOSED' AND pnl_percentage > 0 THEN 1 ELSE 0 END) as winCount, " +
            "SUM(CASE WHEN status = 'CLOSED' THEN 1 ELSE 0 END) as closedCount, " +
            "AVG(CASE WHEN status = 'CLOSED' THEN pnl_percentage ELSE NULL END) as avgPnl " +
            "FROM signal_performance " +
            "WHERE signal_time >= #{startTime}")
    Map<String, Object> getStatistics(@Param("startTime") LocalDateTime startTime);

    @Select("SELECT symbol, COUNT(*) as count FROM signal_performance " +
            "WHERE signal_time >= #{startTime} " +
            "GROUP BY symbol ORDER BY count DESC LIMIT #{limit}")
    List<Map<String, Object>> getTopSymbols(@Param("startTime") LocalDateTime startTime, 
                                             @Param("limit") int limit);
}
