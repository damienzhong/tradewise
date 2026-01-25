package com.example.tradewise.controller;

import com.example.tradewise.entity.User;
import com.example.tradewise.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

/**
 * 认证控制器
 * 提供登录、登出等认证功能
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(
            @RequestBody LoginRequest request,
            HttpSession session) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            User user = userService.authenticate(request.getUsername(), request.getPassword());
            
            if (user != null) {
                // 登录成功，将用户信息存入session
                session.setAttribute("user", user);
                session.setAttribute("userId", user.getId());
                session.setAttribute("username", user.getUsername());
                session.setAttribute("role", user.getRole());
                
                response.put("success", true);
                response.put("message", "登录成功");
                
                Map<String, Object> userInfo = new HashMap<>();
                userInfo.put("id", user.getId());
                userInfo.put("username", user.getUsername());
                userInfo.put("role", user.getRole());
                response.put("user", userInfo);
                
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "用户名或密码错误");
                return ResponseEntity.status(401).body(response);
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "登录失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 用户登出
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        
        session.invalidate();
        
        response.put("success", true);
        response.put("message", "登出成功");
        
        return ResponseEntity.ok(response);
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/current-user")
    public ResponseEntity<Map<String, Object>> getCurrentUser(HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        
        User user = (User) session.getAttribute("user");
        
        if (user != null) {
            response.put("success", true);
            
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("id", user.getId());
            userInfo.put("username", user.getUsername());
            userInfo.put("role", user.getRole());
            response.put("user", userInfo);
        } else {
            response.put("success", false);
            response.put("message", "未登录");
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * 登录请求对象
     */
    public static class LoginRequest {
        private String username;
        private String password;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }
}
