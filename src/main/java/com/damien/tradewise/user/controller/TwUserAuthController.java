package com.damien.tradewise.user.controller;

import com.damien.tradewise.user.entity.TwUser;
import com.damien.tradewise.common.service.TwAuthService;
import com.damien.tradewise.common.service.TwLoginLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

/**
 * 用户端认证Controller
 */
@RestController
@RequestMapping("/user/auth")
public class TwUserAuthController {
    
    @Autowired
    private TwAuthService authService;
    
    @Autowired
    private TwLoginLogService loginLogService;
    
    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String password = request.get("password");
        String email = request.get("email");
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            TwUser user = authService.registerUser(username, password, email);
            response.put("success", true);
            response.put("message", "注册成功");
            response.put("data", buildUserInfo(user));
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        
        return response;
    }
    
    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> request,
                                     HttpServletRequest httpRequest,
                                     HttpSession session) {
        String username = request.get("username");
        String password = request.get("password");
        
        Map<String, Object> response = new HashMap<>();
        
        TwUser user = authService.authenticateUser(username, password);
        
        String ip = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        
        if (user != null) {
            session.setAttribute("tw_user_id", user.getId());
            session.setAttribute("tw_user_username", user.getUsername());
            
            authService.updateUserLoginInfo(user.getId(), ip);
            
            // 记录登录成功日志
            loginLogService.recordLogin("USER", user.getId(), user.getUsername(), ip, userAgent, true, null);
            
            response.put("success", true);
            response.put("message", "登录成功");
            response.put("data", buildUserInfo(user));
        } else {
            // 记录登录失败日志
            loginLogService.recordLogin("USER", null, username, ip, userAgent, false, "用户名或密码错误");
            
            response.put("success", false);
            response.put("message", "用户名或密码错误");
        }
        
        return response;
    }
    
    @PostMapping("/logout")
    public Map<String, Object> logout(HttpSession session) {
        session.removeAttribute("tw_user_id");
        session.removeAttribute("tw_user_username");
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "登出成功");
        
        return response;
    }
    
    @GetMapping("/current")
    public Map<String, Object> getCurrentUser(HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        
        Long userId = (Long) session.getAttribute("tw_user_id");
        if (userId != null) {
            Map<String, Object> data = new HashMap<>();
            data.put("id", userId);
            data.put("username", session.getAttribute("tw_user_username"));
            
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
            authService.createPasswordResetToken(email);
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
            authService.resetPassword(token, newPassword);
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
        
        Long userId = (Long) session.getAttribute("tw_user_id");
        if (userId == null) {
            response.put("success", false);
            response.put("message", "未登录");
            return response;
        }
        
        try {
            authService.changeUserPassword(userId, oldPassword, newPassword);
            response.put("success", true);
            response.put("message", "密码修改成功");
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        
        return response;
    }
    
    /**
     * 验证邮箱
     */
    @PostMapping("/verify-email")
    public Map<String, Object> verifyEmail(@RequestBody Map<String, String> request) {
        String token = request.get("token");
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            authService.verifyEmail(token);
            response.put("success", true);
            response.put("message", "邮箱验证成功");
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        
        return response;
    }
    
    /**
     * 重新发送验证邮件
     */
    @PostMapping("/resend-verification")
    public Map<String, Object> resendVerification(HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        
        Long userId = (Long) session.getAttribute("tw_user_id");
        if (userId == null) {
            response.put("success", false);
            response.put("message", "未登录");
            return response;
        }
        
        try {
            TwUser user = authService.getUserById(userId);
            if (user == null) {
                response.put("success", false);
                response.put("message", "用户不存在");
                return response;
            }
            
            if (user.getEmailVerified()) {
                response.put("success", false);
                response.put("message", "邮箱已验证，无需重复验证");
                return response;
            }
            
            authService.sendEmailVerification(user);
            response.put("success", true);
            response.put("message", "验证邮件已发送");
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        
        return response;
    }
    
    private Map<String, Object> buildUserInfo(TwUser user) {
        Map<String, Object> info = new HashMap<>();
        info.put("id", user.getId());
        info.put("username", user.getUsername());
        info.put("nickname", user.getNickname());
        info.put("email", user.getEmail());
        info.put("avatar", user.getAvatar());
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
