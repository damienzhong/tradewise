package com.damien.tradewise.admin.controller;

import com.damien.tradewise.admin.entity.TwAdmin;
import com.damien.tradewise.admin.mapper.TwAdminMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

/**
 * 管理员个人信息Controller
 */
@RestController
@RequestMapping("/admin/profile")
public class TwAdminProfileController {
    
    @Autowired
    private TwAdminMapper adminMapper;
    
    /**
     * 获取个人信息
     */
    @GetMapping("/info")
    public Map<String, Object> getProfile(HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        
        Long adminId = (Long) session.getAttribute("tw_admin_id");
        if (adminId == null) {
            response.put("success", false);
            response.put("message", "未登录");
            return response;
        }
        
        TwAdmin admin = adminMapper.findById(adminId);
        if (admin != null) {
            response.put("success", true);
            response.put("data", buildAdminInfo(admin));
        } else {
            response.put("success", false);
            response.put("message", "管理员不存在");
        }
        
        return response;
    }
    
    /**
     * 更新个人信息
     */
    @PostMapping("/update")
    public Map<String, Object> updateProfile(@RequestBody Map<String, String> request, HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        
        Long adminId = (Long) session.getAttribute("tw_admin_id");
        if (adminId == null) {
            response.put("success", false);
            response.put("message", "未登录");
            return response;
        }
        
        try {
            TwAdmin admin = adminMapper.findById(adminId);
            if (admin == null) {
                response.put("success", false);
                response.put("message", "管理员不存在");
                return response;
            }
            
            // 更新信息
            if (request.containsKey("nickName")) {
                admin.setNickName(request.get("nickName"));
            }
            if (request.containsKey("phone")) {
                admin.setPhone(request.get("phone"));
            }
            
            adminMapper.update(admin);
            
            response.put("success", true);
            response.put("message", "个人信息更新成功");
            response.put("data", buildAdminInfo(admin));
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
        info.put("phone", admin.getPhone());
        info.put("role", admin.getRole());
        info.put("enabled", admin.getEnabled());
        info.put("loginCount", admin.getLoginCount());
        info.put("lastLoginTime", admin.getLastLoginTime());
        info.put("createdAt", admin.getCreatedAt());
        return info;
    }
}
