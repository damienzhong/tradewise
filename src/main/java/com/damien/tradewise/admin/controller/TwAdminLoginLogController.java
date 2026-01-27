package com.damien.tradewise.admin.controller;

import com.damien.tradewise.common.entity.TwLoginLog;
import com.damien.tradewise.common.service.TwLoginLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理员登录日志Controller
 */
@RestController
@RequestMapping("/admin/login-logs")
public class TwAdminLoginLogController {
    
    @Autowired
    private TwLoginLogService loginLogService;
    
    /**
     * 获取我的登录日志
     */
    @GetMapping("/my")
    public Map<String, Object> getMyLoginLogs(@RequestParam(defaultValue = "50") Integer limit, 
                                              HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        
        Long adminId = (Long) session.getAttribute("tw_admin_id");
        if (adminId == null) {
            response.put("success", false);
            response.put("message", "未登录");
            return response;
        }
        
        List<TwLoginLog> logs = loginLogService.getUserLoginLogs("ADMIN", adminId, limit);
        response.put("success", true);
        response.put("data", logs);
        
        return response;
    }
    
    /**
     * 获取所有登录日志（仅超级管理员）
     */
    @GetMapping("/all")
    public Map<String, Object> getAllLoginLogs(@RequestParam(defaultValue = "100") Integer limit, 
                                               HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        
        String role = (String) session.getAttribute("tw_admin_role");
        if (!"SUPER_ADMIN".equals(role)) {
            response.put("success", false);
            response.put("message", "权限不足");
            return response;
        }
        
        List<TwLoginLog> logs = loginLogService.getAllLoginLogs(limit);
        response.put("success", true);
        response.put("data", logs);
        
        return response;
    }
}
