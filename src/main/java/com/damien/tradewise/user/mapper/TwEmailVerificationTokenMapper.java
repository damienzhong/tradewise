package com.damien.tradewise.user.mapper;

import com.damien.tradewise.user.entity.TwEmailVerificationToken;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TwEmailVerificationTokenMapper {
    
    void insert(TwEmailVerificationToken token);
    
    TwEmailVerificationToken findByToken(@Param("token") String token);
    
    void markAsUsed(@Param("token") String token);
}
