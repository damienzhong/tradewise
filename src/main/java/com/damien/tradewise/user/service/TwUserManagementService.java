package com.damien.tradewise.user.service;

import com.damien.tradewise.user.entity.TwUser;
import com.damien.tradewise.user.mapper.TwUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TwUserManagementService {
    
    @Autowired
    private TwUserMapper userMapper;
    
    /**
     * 获取用户列表（分页）
     */
    public Map<String, Object> getUserList(int page, int pageSize, String keyword) {
        int offset = (page - 1) * pageSize;
        
        List<TwUser> users;
        int total;
        
        if (keyword != null && !keyword.trim().isEmpty()) {
            users = userMapper.searchUsers(keyword, offset, pageSize);
            total = userMapper.countSearchUsers(keyword);
        } else {
            users = userMapper.findAllWithPagination(offset, pageSize);
            total = userMapper.countAll();
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("users", users);
        result.put("total", total);
        result.put("page", page);
        result.put("pageSize", pageSize);
        result.put("totalPages", (int) Math.ceil((double) total / pageSize));
        
        return result;
    }
    
    /**
     * 获取用户详情
     */
    public TwUser getUserDetail(Long userId) {
        return userMapper.findById(userId);
    }
    
    /**
     * 启用/禁用用户
     */
    public void toggleUserStatus(Long userId, boolean enabled) {
        userMapper.updateEnabled(userId, enabled);
    }
    
    /**
     * 删除用户
     */
    public void deleteUser(Long userId) {
        userMapper.deleteById(userId);
    }
    
    /**
     * 获取用户统计
     */
    public Map<String, Object> getUserStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", userMapper.countAll());
        stats.put("enabledUsers", userMapper.countByEnabled(true));
        stats.put("disabledUsers", userMapper.countByEnabled(false));
        stats.put("verifiedUsers", userMapper.countByEmailVerified(true));
        stats.put("unverifiedUsers", userMapper.countByEmailVerified(false));
        return stats;
    }
}
