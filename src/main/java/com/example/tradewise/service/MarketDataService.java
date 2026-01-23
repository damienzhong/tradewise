package com.example.tradewise.service;

import com.example.tradewise.service.MarketAnalysisService.Candlestick;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * 市场数据服务
 * 用于从交易所API获取真实的市场数据
 */
@Service
public class MarketDataService {
    
    private static final Logger logger = LoggerFactory.getLogger(MarketDataService.class);
    
    @Autowired
    private RestTemplate restTemplate;
    
    /**
     * 从币安API获取K线数据
     * 
     * @param symbol 交易对符号，如 "BTCUSDT"
     * @param interval K线周期，如 "1h", "15m", "1d" 等
     * @param limit 返回数据点的数量
     * @return K线数据列表
     */
    public List<Candlestick> getKlines(String symbol, String interval, int limit) {
        logger.info("从币安API获取{}的{} K线数据，数量: {}", symbol, interval, limit);
        
        try {
            // 构建API请求URL
            String apiUrl = String.format(
                "https://api.binance.com/api/v3/klines?symbol=%s&interval=%s&limit=%d", 
                symbol.toUpperCase(), interval, limit
            );
            
            logger.debug("请求URL: {}", apiUrl);
            
            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "TradeWise Market Analysis Bot");
            headers.set("Accept", "application/json");
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            // 发起请求
            ResponseEntity<String> response = restTemplate.exchange(
                apiUrl,
                HttpMethod.GET,
                entity,
                String.class
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                // 解析响应数据
                return parseKlineData(response.getBody(), symbol);
            } else {
                logger.error("API请求失败，状态码: {}", response.getStatusCode());
                return new ArrayList<>();
            }
            
        } catch (Exception e) {
            logger.error("获取K线数据时发生错误", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 解析K线数据
     */
    private List<Candlestick> parseKlineData(String responseBody, String symbol) {
        List<Candlestick> klines = new ArrayList<>();
        
        try {
            // 移除首尾的方括号
            String data = responseBody.trim();
            if (data.startsWith("[") && data.endsWith("]")) {
                data = data.substring(1, data.length() - 1);
            }
            
            // 按逗号分割数组元素，需要处理嵌套数组的情况
            List<String> klineStrings = extractJSONArray(data);
            
            for (String klineStr : klineStrings) {
                // 解析单个K线数据
                klineStr = klineStr.trim();
                if (klineStr.startsWith("[") && klineStr.endsWith("]")) {
                    klineStr = klineStr.substring(1, klineStr.length() - 1);
                    
                    String[] values = parseJSONArray(klineStr);
                    
                    if (values.length >= 6) {
                        // 解析数值，去除可能的引号
                        long openTime = parseLong(removeQuotes(values[0].trim()));
                        double open = parseDouble(removeQuotes(values[1].trim()));
                        double high = parseDouble(removeQuotes(values[2].trim()));
                        double low = parseDouble(removeQuotes(values[3].trim()));
                        double close = parseDouble(removeQuotes(values[4].trim()));
                        double volume = parseDouble(removeQuotes(values[5].trim()));
                        
                        Candlestick candle = new Candlestick(symbol, openTime, open, high, low, close, volume);
                        klines.add(candle);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("解析K线数据时发生错误", e);
        }
        
        logger.info("成功解析 {} 条K线数据", klines.size());
        return klines;
    }
    
    /**
     * 去除字符串两端的引号
     */
    private String removeQuotes(String str) {
        if (str.startsWith("\"") && str.endsWith("\"")) {
            return str.substring(1, str.length() - 1);
        }
        if (str.startsWith("'") && str.endsWith("'")) {
            return str.substring(1, str.length() - 1);
        }
        return str;
    }
    
    /**
     * 解析长整型
     */
    private long parseLong(String str) {
        return Long.parseLong(str);
    }
    
    /**
     * 解析双精度浮点型
     */
    private double parseDouble(String str) {
        return Double.parseDouble(str);
    }
    
    /**
     * 提取JSON数组元素
     */
    private List<String> extractJSONArray(String jsonArray) {
        List<String> elements = new ArrayList<>();
        int bracketCount = 0;
        int start = 0;
        boolean insideArray = false;
        boolean insideString = false;
        char quoteChar = '"';
        
        for (int i = 0; i < jsonArray.length(); i++) {
            char c = jsonArray.charAt(i);
            
            if (!insideString && c == '[') {
                if (bracketCount == 0) {
                    insideArray = true;
                    start = i;
                }
                bracketCount++;
            } else if (!insideString && c == ']') {
                bracketCount--;
                if (bracketCount == 0 && insideArray) {
                    elements.add(jsonArray.substring(start, i + 1));
                    insideArray = false;
                }
            } else if (c == '"' || c == '\'') {
                if (!insideString) {
                    insideString = true;
                    quoteChar = c;
                } else if (c == quoteChar) {
                    insideString = false;
                }
            }
        }
        
        return elements;
    }
    
    /**
     * 解析JSON数组字符串为字符串数组
     */
    private String[] parseJSONArray(String arrayStr) {
        List<String> result = new ArrayList<>();
        int bracketCount = 0;
        int start = 0;
        boolean insideString = false;
        char quoteChar = '"';
        
        for (int i = 0; i <= arrayStr.length(); i++) {
            char c = (i < arrayStr.length()) ? arrayStr.charAt(i) : ',';
            
            if (c == '[') {
                bracketCount++;
            } else if (c == ']') {
                bracketCount--;
            } else if (c == '"' || c == '\'') {
                if (!insideString) {
                    insideString = true;
                    quoteChar = c;
                } else if (c == quoteChar) {
                    insideString = false;
                }
            } else if (c == ',' && bracketCount == 0 && !insideString) {
                String value = arrayStr.substring(start, i).trim();
                result.add(value);
                start = i + 1;
            }
        }
        
        if (start < arrayStr.length()) {
            String value = arrayStr.substring(start).trim();
            result.add(value);
        }
        
        return result.toArray(new String[0]);
    }
    
    /**
     * 获取最新的价格信息
     */
    public double getCurrentPrice(String symbol) {
        logger.info("获取{}的当前价格");
        
        try {
            String apiUrl = String.format("https://api.binance.com/api/v3/ticker/price?symbol=%s", symbol.toUpperCase());
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "TradeWise Market Analysis Bot");
            headers.set("Accept", "application/json");
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                apiUrl,
                HttpMethod.GET,
                entity,
                String.class
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                // 简单解析价格
                String body = response.getBody();
                int priceStart = body.indexOf("\"price\"");
                if (priceStart != -1) {
                    priceStart = body.indexOf(':', priceStart) + 1;
                    int priceEnd = body.indexOf(',', priceStart);
                    if (priceEnd == -1) {
                        priceEnd = body.indexOf('}', priceStart);
                    }
                    String priceStr = body.substring(priceStart, priceEnd).trim();
                    if (priceStr.startsWith("\"")) {
                        priceStr = priceStr.substring(1, priceStr.length() - 1);
                    }
                    return Double.parseDouble(priceStr);
                }
            }
        } catch (Exception e) {
            logger.error("获取当前价格时发生错误", e);
        }
        
        // 如果API调用失败，返回默认值
        return 0.0;
    }
}