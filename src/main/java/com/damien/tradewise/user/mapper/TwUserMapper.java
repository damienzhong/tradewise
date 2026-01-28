package com.damien.tradewise.user.mapper;

import com.damien.tradewise.user.entity.TwUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 普通用户Mapper接口
 */
@Mapper
        public interface TwUserMapper {
    
    /**
     * 根据用户名查询用户
     */
    TwUser findByUsername(@Param("username") String username);
    
    /**
     * 根据邮箱查询用户
     */
    TwUser findByEmail(@Param("email") String email);
    
    /**
     * 根据ID查询用户
     */
    TwUser findById(@Param("id") Long id);
    
    /**
     * 插入用户
     */
    int insert(TwUser user);
    
    /**
     * 更新用户信息
     */
    int update(TwUser user);
    
    /**
     * 更新密码
     */
    int updatePassword(@Param("id") Long id, @Param("password") String password);
    
    /**
     * 更新邮箱验证状态
     */
    int updateEmailVerified(@Param("id") Long id, @Param("emailVerified") Boolean emailVerified);
    
    /**
     * 更新登录信息
     */
    int updateLoginInfo(@Param("id") Long id, 
                       @Param("loginIp") String loginIp);
    
    /**
     * 分页查询所有用户
     */
    List<TwUser> findAllWithPagination(@Param("offset") int offset, @Param("limit") int limit);
    
    /**
     * 搜索用户（按用户名、邮箱、昵称）
     */
    List<TwUser> searchUsers(@Param("keyword") String keyword, @Param("offset") int offset, @Param("limit") int limit);
    
    /**
     * 统计总用户数
     */
    int countAll();
    
    /**
     * 统计搜索结果数
     */
    int countSearchUsers(@Param("keyword") String keyword);
    
    /**
     * 统计启用/禁用用户数
     */
    int countByEnabled(@Param("enabled") Boolean enabled);
    
    /**
     * 统计邮箱验证用户数
     */
    int countByEmailVerified(@Param("emailVerified") Boolean emailVerified);
    
    /**
     * 更新用户启用状态
     */
    int updateEnabled(@Param("id") Long id, @Param("enabled") Boolean enabled);
    
    /**
     * 删除用户
     */
    int deleteById(@Param("id") Long id);
}
