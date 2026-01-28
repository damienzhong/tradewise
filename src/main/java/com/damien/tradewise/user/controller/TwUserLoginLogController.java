package com.damien.tradewise.user.controller;

import com.damien.tradewise.common.entity.TwLoginLog;
import com.damien.tradewise.common.service.TwLoginLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户登录日志Controller
 */
@RestController
@RequestMapping("/user/login-logs")
public class TwUserLoginLogController {
    
    @Autowired
    private TwLoginLogService loginLogService;
    
    /**
     * 获取我的登录日志
     */
    @GetMapping("/my")
    public Map<String, Object> getMyLoginLogs(@RequestParam(defaultValue = "50") Integer limit, 
                                              HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        
        Long userId = (Long) session.getAttribute("tw_user_id");
        if (userId == null) {
            response.put("success", false);
            response.put("message", "未登录");
            return response;
        }
        
        List<TwLoginLog> logs = loginLogService.getUserLoginLogs("USER", userId, limit);
        response.put("success", true);
        response.put("data", logs);
        
        return response;
    }
}
