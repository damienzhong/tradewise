package com.example.tradewise.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class FeatureConfigService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 检查功能是否启用（总开关）
     */
    public boolean isFeatureEnabled(String featureKey) {
        String sql = "SELECT enabled FROM feature_config WHERE feature_key = ?";
        try {
            Integer enabled = jdbcTemplate.queryForObject(sql, Integer.class, featureKey);
            return enabled != null && enabled == 1;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 检查用户级别功能是否启用
     */
    public boolean isFeatureEnabledForUser(Long userId, String featureKey) {
        // 先检查总开关
        if (!isFeatureEnabled(featureKey)) {
            return false;
        }

        // 再检查用户级别开关
        String sql = "SELECT enabled FROM user_feature_config WHERE user_id = ? AND feature_key = ?";
        try {
            Integer enabled = jdbcTemplate.queryForObject(sql, Integer.class, userId, featureKey);
            return enabled != null && enabled == 1;
        } catch (Exception e) {
            // 如果用户没有配置，默认跟随总开关
            return true;
        }
    }

    /**
     * 设置功能开关
     */
    public void setFeatureEnabled(String featureKey, boolean enabled) {
        String sql = "UPDATE feature_config SET enabled = ? WHERE feature_key = ?";
        int rows = jdbcTemplate.update(sql, enabled ? 1 : 0, featureKey);
        if (rows == 0) {
            throw new RuntimeException("功能不存在: " + featureKey);
        }
    }

    /**
     * 设置用户级别功能开关
     */
    public void setUserFeatureEnabled(Long userId, String featureKey, boolean enabled) {
        String sql = "INSERT INTO user_feature_config (user_id, feature_key, enabled) VALUES (?, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE enabled = ?";
        jdbcTemplate.update(sql, userId, featureKey, enabled ? 1 : 0, enabled ? 1 : 0);
    }

    /**
     * 获取所有功能状态（包含详细信息）
     */
    public Map<String, Map<String, Object>> getAllFeatures() {
        String sql = "SELECT feature_key, feature_name, enabled, description FROM feature_config";
        Map<String, Map<String, Object>> features = new HashMap<>();
        jdbcTemplate.query(sql, rs -> {
            Map<String, Object> feature = new HashMap<>();
            feature.put("name", rs.getString("feature_name"));
            feature.put("enabled", rs.getInt("enabled") == 1);
            feature.put("description", rs.getString("description"));
            features.put(rs.getString("feature_key"), feature);
        });
        return features;
    }
}
