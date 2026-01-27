package com.damien.tradewise.admin.mapper;

import com.damien.tradewise.admin.entity.TwAdminPasswordResetToken;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TwAdminPasswordResetTokenMapper {
    
    void insert(TwAdminPasswordResetToken token);
    
    TwAdminPasswordResetToken findByToken(@Param("token") String token);
    
    void markAsUsed(@Param("token") String token);
}
