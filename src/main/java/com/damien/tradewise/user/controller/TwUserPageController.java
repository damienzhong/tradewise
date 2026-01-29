package com.damien.tradewise.user.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

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
    
    @GetMapping("/forgot-password")
    public String forgotPassword() {
        return "user/tw-user-forgot-password";
    }
    
    @GetMapping("/reset-password")
    public String resetPassword() {
        return "user/tw-user-reset-password";
    }
    
    @GetMapping("/dashboard")
    public String dashboard() {
        return "user/tw-user-dashboard";
    }
    
    @GetMapping("/orders")
    public String orders() {
        return "user/tw-user-orders";
    }
    
    @GetMapping("/copy-trading")
    public String copyTrading() {
        return "user/tw-user-copy-trading";
    }
    
    @GetMapping("/traders")
    public String traders() {
        return "user/tw-user-traders";
    }
    
    @GetMapping("/subscriptions")
    public String subscriptions() {
        return "user/tw-user-subscriptions";
    }
}
