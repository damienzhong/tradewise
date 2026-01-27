package com.damien.tradewise.admin.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 管理端页面Controller
 */
@Controller
@RequestMapping("/admin")
public class TwAdminPageController {
    
    @GetMapping("/login")
    public String login() {
        return "admin/tw-admin-login";
    }
    
    @GetMapping("/dashboard")
    public String dashboard() {
        return "admin/tw-admin-dashboard";
    }
    
    @GetMapping("/admins")
    public String admins() {
        return "admin/tw-admin-management";
    }
    
    @GetMapping("/forgot-password")
    public String forgotPassword() {
        return "admin/tw-admin-forgot-password";
    }
    
    @GetMapping("/reset-password")
    public String resetPassword() {
        return "admin/tw-admin-reset-password";
    }
    
    @GetMapping("/change-password")
    public String changePassword() {
        return "admin/tw-admin-change-password";
    }
    
    @GetMapping("/profile")
    public String profile() {
        return "admin/tw-admin-profile";
    }
    
    @GetMapping("/login-logs")
    public String loginLogs() {
        return "admin/tw-admin-login-logs";
    }
    
    @GetMapping("/system-email")
    public String systemEmail() {
        return "admin/tw-admin-system-email";
    }
    
    @GetMapping("/users")
    public String users() {
        return "admin/tw-admin-users";
    }
}
