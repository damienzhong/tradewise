package com.damien.tradewise.admin.controller;

import com.damien.tradewise.admin.service.TwSystemEmailConfigService;
import com.damien.tradewise.admin.entity.TwSystemEmailConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/system-email")
public class TwSystemEmailConfigController {
    
    @Autowired
    private TwSystemEmailConfigService emailConfigService;
    
    @GetMapping("/list")
    public Map<String, Object> list(HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        
        String role = (String) session.getAttribute("tw_admin_role");
        if (!"SUPER_ADMIN".equals(role)) {
            response.put("success", false);
            response.put("message", "权限不足");
            return response;
        }
        
        List<TwSystemEmailConfig> configs = emailConfigService.getAllConfigs();
        response.put("success", true);
        response.put("data", configs);
        return response;
    }
    
    @PostMapping("/add")
    public Map<String, Object> add(@RequestBody TwSystemEmailConfig config, HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        
        String role = (String) session.getAttribute("tw_admin_role");
        if (!"SUPER_ADMIN".equals(role)) {
            response.put("success", false);
            response.put("message", "权限不足");
            return response;
        }
        
        try {
            emailConfigService.addConfig(config);
            response.put("success", true);
            response.put("message", "添加成功");
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        
        return response;
    }
    
    @PutMapping("/update/{id}")
    public Map<String, Object> update(@PathVariable Long id, @RequestBody TwSystemEmailConfig config, HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        
        String role = (String) session.getAttribute("tw_admin_role");
        if (!"SUPER_ADMIN".equals(role)) {
            response.put("success", false);
            response.put("message", "权限不足");
            return response;
        }
        
        try {
            config.setId(id);
            emailConfigService.updateConfig(config);
            response.put("success", true);
            response.put("message", "更新成功");
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        
        return response;
    }
    
    @DeleteMapping("/delete/{id}")
    public Map<String, Object> delete(@PathVariable Long id, HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        
        String role = (String) session.getAttribute("tw_admin_role");
        if (!"SUPER_ADMIN".equals(role)) {
            response.put("success", false);
            response.put("message", "权限不足");
            return response;
        }
        
        try {
            emailConfigService.deleteConfig(id);
            response.put("success", true);
            response.put("message", "删除成功");
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        
        return response;
    }
    
    @PostMapping("/set-default/{id}")
    public Map<String, Object> setDefault(@PathVariable Long id, HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        
        String role = (String) session.getAttribute("tw_admin_role");
        if (!"SUPER_ADMIN".equals(role)) {
            response.put("success", false);
            response.put("message", "权限不足");
            return response;
        }
        
        try {
            emailConfigService.setDefault(id);
            response.put("success", true);
            response.put("message", "设置成功");
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        
        return response;
    }
    
    @PostMapping("/test/{id}")
    public Map<String, Object> test(@PathVariable Long id, HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        
        String role = (String) session.getAttribute("tw_admin_role");
        if (!"SUPER_ADMIN".equals(role)) {
            response.put("success", false);
            response.put("message", "权限不足");
            return response;
        }
        
        boolean success = emailConfigService.testConnection(id);
        response.put("success", success);
        response.put("message", success ? "连接测试成功" : "连接测试失败");
        
        return response;
    }
}
