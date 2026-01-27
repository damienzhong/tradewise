package com.damien.tradewise.admin.controller;

import com.damien.tradewise.admin.entity.TwAdmin;
import com.damien.tradewise.common.service.TwAuthService;
import com.damien.tradewise.common.service.TwLoginLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

/**
 * 管理端认证Controller
 */
@RestController
@RequestMapping("/admin/auth")
public class TwAdminAuthController {
    
    @Autowired
    private TwAuthService authService;
    
    @Autowired
    private TwLoginLogService loginLogService;
    
    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> request,
                                     HttpServletRequest httpRequest,
                                     HttpSession session) {
        String username = request.get("username");
        String password = request.get("password");
        
        Map<String, Object> response = new HashMap<>();
        
        TwAdmin admin = authService.authenticateAdmin(username, password);
        
        String ip = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        
        if (admin != null) {
            session.setAttribute("tw_admin_id", admin.getId());
            session.setAttribute("tw_admin_username", admin.getUsername());
            session.setAttribute("tw_admin_role", admin.getRole());
            
            authService.updateAdminLoginInfo(admin.getId(), ip);
            
            // 记录登录成功日志
            loginLogService.recordLogin("ADMIN", admin.getId(), admin.getUsername(), ip, userAgent, true, null);
            
            response.put("success", true);
            response.put("message", "登录成功");
            response.put("data", buildAdminInfo(admin));
        } else {
            // 记录登录失败日志
            loginLogService.recordLogin("ADMIN", null, username, ip, userAgent, false, "用户名或密码错误");
            
            response.put("success", false);
            response.put("message", "用户名或密码错误");
        }
        
        return response;
    }
    
    @PostMapping("/logout")
    public Map<String, Object> logout(HttpSession session) {
        session.removeAttribute("tw_admin_id");
        session.removeAttribute("tw_admin_username");
        session.removeAttribute("tw_admin_role");
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "登出成功");
        
        return response;
    }
    
    @GetMapping("/current")
    public Map<String, Object> getCurrentAdmin(HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        
        Long adminId = (Long) session.getAttribute("tw_admin_id");
        if (adminId != null) {
            Map<String, Object> data = new HashMap<>();
            data.put("id", adminId);
            data.put("username", session.getAttribute("tw_admin_username"));
            data.put("role", session.getAttribute("tw_admin_role"));
            
            response.put("success", true);
            response.put("data", data);
        } else {
            response.put("success", false);
            response.put("message", "未登录");
        }
        
        return response;
    }
    
    @PostMapping("/forgot-password")
    public Map<String, Object> forgotPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            authService.createAdminPasswordResetToken(email);
            response.put("success", true);
            response.put("message", "密码重置链接已发送到您的邮箱，请注意查收");
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        
        return response;
    }
    
    @PostMapping("/reset-password")
    public Map<String, Object> resetPassword(@RequestBody Map<String, String> request) {
        String token = request.get("token");
        String newPassword = request.get("newPassword");
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            authService.resetAdminPassword(token, newPassword);
            response.put("success", true);
            response.put("message", "密码重置成功");
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        
        return response;
    }
    
    /**
     * 修改密码（登录后）
     */
    @PostMapping("/change-password")
    public Map<String, Object> changePassword(@RequestBody Map<String, String> request, HttpSession session) {
        String oldPassword = request.get("oldPassword");
        String newPassword = request.get("newPassword");
        
        Map<String, Object> response = new HashMap<>();
        
        Long adminId = (Long) session.getAttribute("tw_admin_id");
        if (adminId == null) {
            response.put("success", false);
            response.put("message", "未登录");
            return response;
        }
        
        try {
            authService.changeAdminPassword(adminId, oldPassword, newPassword);
            response.put("success", true);
            response.put("message", "密码修改成功");
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
        return info;
    }
    
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
