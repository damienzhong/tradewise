package com.example.tradewise.controller;

import com.example.tradewise.entity.EmailConfig;
import com.example.tradewise.mapper.EmailConfigMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/email-config")
public class EmailConfigController {

    @Autowired
    private EmailConfigMapper emailConfigMapper;

    @GetMapping("/list")
    public ResponseEntity<List<EmailConfig>> getAllEmailConfigs() {
        List<EmailConfig> emailConfigs = emailConfigMapper.findAllEnabled();
        return ResponseEntity.ok(emailConfigs);
    }

    @PostMapping("/add")
    public ResponseEntity<Map<String, Object>> addEmailConfig(@RequestBody EmailConfig emailConfig) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 检查邮箱地址是否已经存在
            EmailConfig existingConfig = emailConfigMapper.findByEmailAddress(emailConfig.getEmailAddress());
            if (existingConfig != null) {
                response.put("success", false);
                response.put("message", "邮箱地址已存在");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 设置默认启用状态
            if (emailConfig.getEnabled() == null) {
                emailConfig.setEnabled(true);
            }
            
            emailConfigMapper.insert(emailConfig);
            response.put("success", true);
            response.put("message", "邮箱地址添加成功");
            response.put("data", emailConfig);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "添加邮箱地址失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<Map<String, Object>> updateEmailConfig(@PathVariable Integer id, @RequestBody EmailConfig emailConfig) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            EmailConfig existingConfig = new EmailConfig();
            existingConfig.setId(id);
            existingConfig.setEmailAddress(emailConfig.getEmailAddress());
            existingConfig.setEnabled(emailConfig.getEnabled());
            
            emailConfigMapper.update(existingConfig);
            response.put("success", true);
            response.put("message", "邮箱配置更新成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "更新邮箱配置失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Map<String, Object>> deleteEmailConfig(@PathVariable Integer id) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            emailConfigMapper.deleteById(id);
            response.put("success", true);
            response.put("message", "邮箱地址删除成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "删除邮箱地址失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @DeleteMapping("/delete-by-email/{email}")
    public ResponseEntity<Map<String, Object>> deleteEmailConfigByEmail(@PathVariable String email) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            emailConfigMapper.deleteByEmailAddress(email);
            response.put("success", true);
            response.put("message", "邮箱地址删除成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "删除邮箱地址失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}