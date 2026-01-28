package com.damien.tradewise.admin.controller;

import com.damien.tradewise.admin.entity.TwTraderConfig;
import com.damien.tradewise.admin.service.TwTraderConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/admin/traders")
public class TwTraderConfigController {
    
    @Autowired
    private TwTraderConfigService traderConfigService;
    
    @GetMapping("/list")
    public Map<String, Object> getTraderList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword) {
        
        Map<String, Object> response = new HashMap<>();
        try {
            Map<String, Object> data = traderConfigService.getTraderList(page, pageSize, keyword);
            response.put("success", true);
            response.put("data", data);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "获取交易员列表失败: " + e.getMessage());
        }
        return response;
    }
    
    @GetMapping("/detail/{id}")
    public Map<String, Object> getTraderDetail(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            TwTraderConfig trader = traderConfigService.getTraderDetail(id);
            if (trader != null) {
                response.put("success", true);
                response.put("data", trader);
            } else {
                response.put("success", false);
                response.put("message", "交易员不存在");
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "获取交易员详情失败: " + e.getMessage());
        }
        return response;
    }
    
    @PostMapping("/add")
    public Map<String, Object> addTrader(@RequestBody TwTraderConfig trader, HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        try {
            Long adminId = (Long) session.getAttribute("tw_admin_id");
            trader.setCreatedBy(adminId);
            
            // 设置默认值
            if (trader.getEnabled() == null) {
                trader.setEnabled(true);
            }
            if (trader.getMonitorInterval() == null) {
                trader.setMonitorInterval(60);
            }
            
            traderConfigService.addTrader(trader);
            response.put("success", true);
            response.put("message", "交易员添加成功");
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "添加失败: " + e.getMessage());
        }
        return response;
    }
    
    @PutMapping("/update/{id}")
    public Map<String, Object> updateTrader(@PathVariable Long id, @RequestBody TwTraderConfig trader) {
        Map<String, Object> response = new HashMap<>();
        try {
            trader.setId(id);
            traderConfigService.updateTrader(trader);
            response.put("success", true);
            response.put("message", "交易员更新成功");
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "更新失败: " + e.getMessage());
        }
        return response;
    }
    
    @PostMapping("/toggle-status/{id}")
    public Map<String, Object> toggleStatus(@PathVariable Long id, @RequestParam boolean enabled) {
        Map<String, Object> response = new HashMap<>();
        try {
            traderConfigService.toggleTraderStatus(id, enabled);
            response.put("success", true);
            response.put("message", enabled ? "交易员已启用" : "交易员已禁用");
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "操作失败: " + e.getMessage());
        }
        return response;
    }
    
    @DeleteMapping("/delete/{id}")
    public Map<String, Object> deleteTrader(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            traderConfigService.deleteTrader(id);
            response.put("success", true);
            response.put("message", "交易员已删除");
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "删除失败: " + e.getMessage());
        }
        return response;
    }
    
    @GetMapping("/statistics")
    public Map<String, Object> getStatistics() {
        Map<String, Object> response = new HashMap<>();
        try {
            Map<String, Object> stats = traderConfigService.getStatistics();
            response.put("success", true);
            response.put("data", stats);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "获取统计数据失败: " + e.getMessage());
        }
        return response;
    }
}
