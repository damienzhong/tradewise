package com.damien.tradewise.user.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 用户端页面Controller
 */
@Controller
@RequestMapping("/user")
public class TwUserPageController {
    
    @GetMapping("/login")
    public String login() {
        return "user/tw-user-login";
    }
    
    @GetMapping("/register")
    public String register() {
        return "user/tw-user-register";
    }
    
    @GetMapping("/dashboard")
    public String dashboard() {
        return "user/tw-user-dashboard";
    }
    
    @GetMapping("/forgot-password")
    public String forgotPassword() {
        return "user/tw-user-forgot-password";
    }
    
    @GetMapping("/reset-password")
    public String resetPassword() {
        return "user/tw-user-reset-password";
    }
    
    @GetMapping("/change-password")
    public String changePassword() {
        return "user/tw-user-change-password";
    }
    
    @GetMapping("/profile")
    public String profile() {
        return "user/tw-user-profile";
    }
    
    @GetMapping("/login-logs")
    public String loginLogs() {
        return "user/tw-user-login-logs";
    }
    
    @GetMapping("/verify-email")
    public String verifyEmail() {
        return "user/tw-user-verify-email";
    }
}
