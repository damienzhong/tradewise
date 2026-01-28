package com.damien.tradewise.user.mapper;

import com.damien.tradewise.user.entity.TwUserTraderSubscription;
import org.apache.ibatis.annotations.*;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface TwUserSubscriptionMapper {
    
    /**
     * 查询订阅了指定交易员的所有用户邮箱（仅启用且邮箱已验证的用户）
     */
    @Select("SELECT u.email FROM tw_user u " +
            "INNER JOIN tw_user_trader_subscription s ON u.id = s.user_id " +
            "WHERE s.trader_id = #{traderId} " +
            "AND s.status = 'ACTIVE' " +
            "AND s.notify_email = TRUE " +
            "AND u.enabled = TRUE " +
            "AND u.email_verified = TRUE " +
            "AND u.email IS NOT NULL")
    List<String> findSubscribedUserEmails(@Param("traderId") Long traderId);
    
    /**
     * 查询用户订阅的所有交易员
     */
    @Select("SELECT * FROM tw_user_trader_subscription " +
            "WHERE user_id = #{userId} AND status = 'ACTIVE' " +
            "ORDER BY subscribed_at DESC")
    List<TwUserTraderSubscription> findByUserId(@Param("userId") Long userId);
    
    /**
     * 查询用户是否订阅了指定交易员
     */
    @Select("SELECT COUNT(*) FROM tw_user_trader_subscription " +
            "WHERE user_id = #{userId} AND trader_id = #{traderId}")
    int existsByUserIdAndTraderId(@Param("userId") Long userId, @Param("traderId") Long traderId);
    
    /**
     * 插入订阅
     */
    @Insert("INSERT INTO tw_user_trader_subscription (user_id, trader_id, notify_email, auto_copy_trade, status) " +
            "VALUES (#{userId}, #{traderId}, #{notifyEmail}, #{autoCopyTrade}, #{status})")
    void insert(TwUserTraderSubscription subscription);
    
    /**
     * 删除订阅
     */
    @Delete("DELETE FROM tw_user_trader_subscription WHERE user_id = #{userId} AND trader_id = #{traderId}")
    void deleteByUserIdAndTraderId(@Param("userId") Long userId, @Param("traderId") Long traderId);
    
    /**
     * 更新订阅配置
     */
    @Update("UPDATE tw_user_trader_subscription SET notify_email = #{notifyEmail}, status = #{status} " +
            "WHERE user_id = #{userId} AND trader_id = #{traderId}")
    void updateConfig(@Param("userId") Long userId, @Param("traderId") Long traderId, 
                     @Param("notifyEmail") Boolean notifyEmail, @Param("status") String status);
}
