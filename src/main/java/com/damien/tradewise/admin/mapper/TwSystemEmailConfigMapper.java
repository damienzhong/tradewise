package com.damien.tradewise.admin.mapper;

import com.damien.tradewise.admin.entity.TwSystemEmailConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TwSystemEmailConfigMapper {
    
    List<TwSystemEmailConfig> findAll();
    
    TwSystemEmailConfig findById(@Param("id") Long id);
    
    TwSystemEmailConfig findDefault();
    
    void insert(TwSystemEmailConfig config);
    
    void update(TwSystemEmailConfig config);
    
    void delete(@Param("id") Long id);
    
    void clearDefault();
    
    void updateTestStatus(@Param("id") Long id, @Param("status") String status);
}
