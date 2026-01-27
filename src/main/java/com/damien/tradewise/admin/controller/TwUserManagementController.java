package com.damien.tradewise.admin.controller;

import com.damien.tradewise.user.entity.TwUser;
import com.damien.tradewise.user.service.TwUserManagementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/admin/users")
public class TwUserManagementController {
    
    @Autowired
    private TwUserManagementService userManagementService;
    
    /**
     * 获取用户列表
     */
    @GetMapping("/list")
    public Map<String, Object> getUserList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword) {
        
        Map<String, Object> response = new HashMap<>();
        try {
            Map<String, Object> data = userManagementService.getUserList(page, pageSize, keyword);
            response.put("success", true);
            response.put("data", data);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "获取用户列表失败: " + e.getMessage());
        }
        return response;
    }
    
    /**
     * 获取用户详情
     */
    @GetMapping("/detail/{id}")
    public Map<String, Object> getUserDetail(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            TwUser user = userManagementService.getUserDetail(id);
            if (user != null) {
                // 不返回密码
                user.setPassword(null);
                response.put("success", true);
                response.put("data", user);
            } else {
                response.put("success", false);
                response.put("message", "用户不存在");
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "获取用户详情失败: " + e.getMessage());
        }
        return response;
    }
    
    /**
     * 启用/禁用用户
     */
    @PostMapping("/toggle-status/{id}")
    public Map<String, Object> toggleUserStatus(
            @PathVariable Long id,
            @RequestParam boolean enabled) {
        
        Map<String, Object> response = new HashMap<>();
        try {
            userManagementService.toggleUserStatus(id, enabled);
            response.put("success", true);
            response.put("message", enabled ? "用户已启用" : "用户已禁用");
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "操作失败: " + e.getMessage());
        }
        return response;
    }
    
    /**
     * 删除用户
     */
    @DeleteMapping("/delete/{id}")
    public Map<String, Object> deleteUser(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            userManagementService.deleteUser(id);
            response.put("success", true);
            response.put("message", "用户已删除");
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "删除失败: " + e.getMessage());
        }
        return response;
    }
    
    /**
     * 获取用户统计
     */
    @GetMapping("/statistics")
    public Map<String, Object> getUserStatistics() {
        Map<String, Object> response = new HashMap<>();
        try {
            Map<String, Object> stats = userManagementService.getUserStatistics();
            response.put("success", true);
            response.put("data", stats);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "获取统计数据失败: " + e.getMessage());
        }
        return response;
    }
}
