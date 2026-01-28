package com.damien.tradewise.user.mapper;

import com.damien.tradewise.user.entity.TwPasswordResetToken;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TwPasswordResetTokenMapper {
    
    void insert(TwPasswordResetToken token);
    
    TwPasswordResetToken findByToken(@Param("token") String token);
    
    void markAsUsed(@Param("token") String token);
    
    void deleteExpiredTokens();
}
