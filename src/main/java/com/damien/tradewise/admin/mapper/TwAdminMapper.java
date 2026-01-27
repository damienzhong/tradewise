package com.damien.tradewise.admin.mapper;

import com.damien.tradewise.admin.entity.TwAdmin;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 管理员Mapper接口
 */
@Mapper
public interface TwAdminMapper {
    
    /**
     * 根据用户名查询管理员
     */
    TwAdmin findByUsername(@Param("username") String username);
    
    /**
     * 根据邮箱查询管理员
     */
    TwAdmin findByEmail(@Param("email") String email);
    
    /**
     * 根据ID查询管理员
     */
    TwAdmin findById(@Param("id") Long id);
    
    /**
     * 查询所有管理员
     */
    List<TwAdmin> findAll();
    
    /**
     * 插入管理员
     */
    int insert(TwAdmin admin);
    
    /**
     * 更新管理员信息
     */
    int update(TwAdmin admin);
    
    /**
     * 更新密码
     */
    int updatePassword(@Param("id") Long id, @Param("password") String password);
    
    /**
     * 更新登录信息
     */
    int updateLoginInfo(@Param("id") Long id, 
                       @Param("loginIp") String loginIp);
    
    /**
     * 删除管理员
     */
    int delete(@Param("id") Long id);
}
