package com.damien.tradewise.common.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
public class BinanceApiService {
    
    private static final Logger logger = LoggerFactory.getLogger(BinanceApiService.class);
    
    @Value("${tradewise.api.base-url}")
    private String baseUrl;
    
    @Value("${tradewise.api.order-history-endpoint}")
    private String orderHistoryEndpoint;
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    public BinanceApiService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * 获取交易员的历史订单
     */
    public List<JsonNode> getTraderOrders(String portfolioId) {
        try {
            // 计算时间范围（当前时间前12小时到当前时间后12小时）
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime startTimeLocal = now.minusHours(12);
            LocalDateTime endTimeLocal = now.plusHours(12);
            
            long startTime = startTimeLocal.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            long endTime = endTimeLocal.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            
            // 构建请求参数
            String requestBody = String.format(
                "{\"portfolioId\":\"%s\",\"startTime\":%d,\"endTime\":%d,\"pageSize\":10}",
                portfolioId, startTime, endTime
            );
            
            logger.info("调用币安API获取交易员开单记录: portfolioId={}, 请求参数={}", portfolioId, requestBody);
            
            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            headers.set("Accept", "application/json, text/plain, */*");
            headers.set("Referer", "https://www.binance.com/en/copy-trade/lead/" + portfolioId);
            headers.set("Origin", "https://www.binance.com");
            
            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
            
            // 发送请求
            String apiUrl = baseUrl + orderHistoryEndpoint;
            ResponseEntity<String> response = restTemplate.exchange(
                apiUrl,
                HttpMethod.POST,
                entity,
                String.class
            );
            
            String responseBody = response.getBody();
            logger.info("API响应: {}", responseBody);
            
            // 解析响应
            JsonNode root = objectMapper.readTree(responseBody);
            List<JsonNode> orders = new ArrayList<>();
            
            if (root.has("success") && root.get("success").asBoolean()) {
                JsonNode dataNode = root.get("data");
                if (dataNode != null && dataNode.has("list")) {
                    JsonNode listNode = dataNode.get("list");
                    if (listNode.isArray()) {
                        listNode.forEach(orders::add);
                    }
                }
            }
            
            logger.info("获取到 {} 条订单数据", orders.size());
            return orders;
            
        } catch (Exception e) {
            logger.error("调用币安API失败: portfolioId={}, error={}", portfolioId, e.getMessage(), e);
            return new ArrayList<>();
        }
    }
}
