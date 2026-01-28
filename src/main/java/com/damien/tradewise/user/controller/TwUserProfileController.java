package com.damien.tradewise.user.controller;

import com.damien.tradewise.user.entity.TwUser;
import com.damien.tradewise.user.mapper.TwUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

/**
 * 用户个人信息Controller
 */
@RestController
@RequestMapping("/user/profile")
public class TwUserProfileController {
    
    @Autowired
    private TwUserMapper userMapper;
    
    /**
     * 获取个人信息
     */
    @GetMapping("/info")
    public Map<String, Object> getProfile(HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        
        Long userId = (Long) session.getAttribute("tw_user_id");
        if (userId == null) {
            response.put("success", false);
            response.put("message", "未登录");
            return response;
        }
        
        TwUser user = userMapper.findById(userId);
        if (user != null) {
            response.put("success", true);
            response.put("data", buildUserInfo(user));
        } else {
            response.put("success", false);
            response.put("message", "用户不存在");
        }
        
        return response;
    }
    
    /**
     * 更新个人信息
     */
    @PostMapping("/update")
    public Map<String, Object> updateProfile(@RequestBody Map<String, String> request, HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        
        Long userId = (Long) session.getAttribute("tw_user_id");
        if (userId == null) {
            response.put("success", false);
            response.put("message", "未登录");
            return response;
        }
        
        try {
            TwUser user = userMapper.findById(userId);
            if (user == null) {
                response.put("success", false);
                response.put("message", "用户不存在");
                return response;
            }
            
            // 更新信息
            if (request.containsKey("nickname")) {
                user.setNickname(request.get("nickname"));
            }
            if (request.containsKey("phone")) {
                user.setPhone(request.get("phone"));
            }
            if (request.containsKey("avatar")) {
                user.setAvatar(request.get("avatar"));
            }
            
            userMapper.update(user);
            
            response.put("success", true);
            response.put("message", "个人信息更新成功");
            response.put("data", buildUserInfo(user));
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
        info.put("phone", user.getPhone());
        info.put("avatar", user.getAvatar());
        info.put("enabled", user.getEnabled());
        info.put("loginCount", user.getLoginCount());
        info.put("lastLoginTime", user.getLastLoginTime());
        info.put("createdAt", user.getCreatedAt());
        return info;
    }
}
