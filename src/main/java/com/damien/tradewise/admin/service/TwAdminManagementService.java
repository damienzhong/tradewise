package com.damien.tradewise.admin.service;

import com.damien.tradewise.admin.entity.TwAdmin;
import com.damien.tradewise.admin.mapper.TwAdminMapper;
import com.damien.tradewise.common.service.TwPasswordEncoder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 管理员管理Service
 */
@Service
public class TwAdminManagementService {
    
    @Autowired
    private TwAdminMapper adminMapper;
    
    @Autowired
    private TwPasswordEncoder passwordEncoder;
    
    /**
     * 创建管理员（仅超级管理员可用）
     */
    public TwAdmin createAdmin(String username, String password, String nickName, String email, String role, Long createdBy) {
        // 检查用户名是否已存在
        if (adminMapper.findByUsername(username) != null) {
            throw new RuntimeException("用户名已存在");
        }
        
        // 检查邮箱是否已存在
        if (email != null && !email.isEmpty() && adminMapper.findByEmail(email) != null) {
            throw new RuntimeException("邮箱已被使用");
        }
        
        TwAdmin admin = new TwAdmin();
        admin.setUsername(username);
        admin.setPassword(passwordEncoder.encode(password));
        admin.setNickName(nickName);
        admin.setEmail(email);
        admin.setRole(role);
        admin.setEnabled(true);
        admin.setCreatedBy(createdBy);
        
        adminMapper.insert(admin);
        return admin;
    }
    
    /**
     * 获取所有管理员列表
     */
    public List<TwAdmin> getAllAdmins() {
        return adminMapper.findAll();
    }
    
    /**
     * 启用/禁用管理员
     */
    public void toggleAdminStatus(Long adminId, Boolean enabled) {
        TwAdmin admin = adminMapper.findById(adminId);
        if (admin == null) {
            throw new RuntimeException("管理员不存在");
        }
        
        if ("SUPER_ADMIN".equals(admin.getRole())) {
            throw new RuntimeException("不能禁用超级管理员");
        }
        
        admin.setEnabled(enabled);
        adminMapper.update(admin);
    }
    
    /**
     * 删除管理员
     */
    public void deleteAdmin(Long adminId) {
        TwAdmin admin = adminMapper.findById(adminId);
        if (admin == null) {
            throw new RuntimeException("管理员不存在");
        }
        
        if ("SUPER_ADMIN".equals(admin.getRole())) {
            throw new RuntimeException("不能删除超级管理员");
        }
        
        adminMapper.delete(adminId);
    }
}
