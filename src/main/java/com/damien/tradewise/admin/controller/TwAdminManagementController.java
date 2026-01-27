package com.damien.tradewise.admin.controller;

import com.damien.tradewise.admin.service.TwAdminManagementService;
import com.damien.tradewise.admin.entity.TwAdmin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 管理员管理Controller（仅超级管理员可用）
 */
@RestController
@RequestMapping("/admin/management")
public class TwAdminManagementController {
    
    @Autowired
    private TwAdminManagementService adminManagementService;
    
    /**
     * 创建管理员
     */
    @PostMapping("/create")
    public Map<String, Object> createAdmin(@RequestBody Map<String, String> request, HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        
        // 检查是否为超级管理员
        String role = (String) session.getAttribute("tw_admin_role");
        if (!"SUPER_ADMIN".equals(role)) {
            response.put("success", false);
            response.put("message", "权限不足，仅超级管理员可创建管理员");
            return response;
        }
        
        try {
            String username = request.get("username");
            String password = request.get("password");
            String nickName = request.get("nickName");
            String email = request.get("email");
            String adminRole = request.getOrDefault("role", "ADMIN");
            Long createdBy = (Long) session.getAttribute("tw_admin_id");
            
            TwAdmin admin = adminManagementService.createAdmin(username, password, nickName, email, adminRole, createdBy);
            
            response.put("success", true);
            response.put("message", "管理员创建成功");
            response.put("data", buildAdminInfo(admin));
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        
        return response;
    }
    
    /**
     * 获取管理员列表
     */
    @GetMapping("/list")
    public Map<String, Object> getAdminList(HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        
        // 检查是否为超级管理员
        String role = (String) session.getAttribute("tw_admin_role");
        if (!"SUPER_ADMIN".equals(role)) {
            response.put("success", false);
            response.put("message", "权限不足");
            return response;
        }
        
        try {
            List<TwAdmin> admins = adminManagementService.getAllAdmins();
            List<Map<String, Object>> adminList = admins.stream()
                    .map(this::buildAdminInfo)
                    .collect(Collectors.toList());
            
            response.put("success", true);
            response.put("data", adminList);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        
        return response;
    }
    
    /**
     * 启用/禁用管理员
     */
    @PostMapping("/toggle-status/{adminId}")
    public Map<String, Object> toggleStatus(@PathVariable Long adminId, 
                                            @RequestParam Boolean enabled,
                                            HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        
        // 检查是否为超级管理员
        String role = (String) session.getAttribute("tw_admin_role");
        if (!"SUPER_ADMIN".equals(role)) {
            response.put("success", false);
            response.put("message", "权限不足");
            return response;
        }
        
        try {
            adminManagementService.toggleAdminStatus(adminId, enabled);
            response.put("success", true);
            response.put("message", enabled ? "管理员已启用" : "管理员已禁用");
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        
        return response;
    }
    
    /**
     * 删除管理员
     */
    @DeleteMapping("/delete/{adminId}")
    public Map<String, Object> deleteAdmin(@PathVariable Long adminId, HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        
        // 检查是否为超级管理员
        String role = (String) session.getAttribute("tw_admin_role");
        if (!"SUPER_ADMIN".equals(role)) {
            response.put("success", false);
            response.put("message", "权限不足");
            return response;
        }
        
        try {
            adminManagementService.deleteAdmin(adminId);
            response.put("success", true);
            response.put("message", "管理员已删除");
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        
        return response;
    }
    
    private Map<String, Object> buildAdminInfo(TwAdmin admin) {
        Map<String, Object> info = new HashMap<>();
        info.put("id", admin.getId());
        info.put("username", admin.getUsername());
        info.put("nickName", admin.getNickName());
        info.put("email", admin.getEmail());
        info.put("role", admin.getRole());
        info.put("enabled", admin.getEnabled());
        info.put("loginCount", admin.getLoginCount());
        info.put("lastLoginTime", admin.getLastLoginTime());
        info.put("createdAt", admin.getCreatedAt());
        return info;
    }
}
