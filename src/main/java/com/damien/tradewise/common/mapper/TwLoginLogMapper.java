package com.damien.tradewise.common.mapper;

import com.damien.tradewise.common.entity.TwLoginLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TwLoginLogMapper {
    
    void insert(TwLoginLog log);
    
    List<TwLoginLog> findByUserId(@Param("userType") String userType, 
                                   @Param("userId") Long userId, 
                                   @Param("limit") Integer limit);
    
    List<TwLoginLog> findAll(@Param("limit") Integer limit);
}
