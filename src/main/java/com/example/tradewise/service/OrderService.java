package com.example.tradewise.service;

import com.example.tradewise.dto.OrderHistoryResponse;
import com.example.tradewise.entity.Order;
import com.example.tradewise.mapper.OrderMapper;
import com.example.tradewise.service.SystemHealthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;

@Service
public class OrderService {

    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private EmailService emailService;

    @Autowired
    private ObjectMapper objectMapper;  // 注入ObjectMapper用于序列化响应对象

    @Autowired
    private OrderMapper orderMapper;  // 注入订单Mapper

    @Autowired
    private SystemHealthService systemHealthService;  // 注入系统健康服务

    @Autowired
    private com.example.tradewise.service.SystemAlertService systemAlertService; // 注入系统告警服务

    @Autowired
    private com.example.tradewise.config.TradeWiseProperties tradeWiseProperties; // 注入配置属性

    @Autowired
    private CopyTradeSignalEvaluator signalEvaluator; // 注入信号质量评估器

    public java.util.List<Order> fetchAndSaveOrders(String traderId, String traderName, String portfolioId) {
        return fetchAndSaveOrdersWithRetry(traderId, traderName, portfolioId, tradeWiseProperties.getRetryAttempts());
    }

    private java.util.List<Order> fetchAndSaveOrdersWithRetry(String traderId, String traderName, String portfolioId, int remainingRetries) {
        long methodStartTime = System.currentTimeMillis();
        try {
            // 计算时间范围（当前时间前3天到当前时间后8小时）
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            java.time.LocalDateTime startTimeLocal = now.minusDays(3);
            java.time.LocalDateTime endTimeLocal = now.plusHours(8);

            // 转换为时间戳
            long startTime = startTimeLocal.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            long endTime = endTimeLocal.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

            // 构建请求参数
            String requestBody = String.format(
                    "{\"portfolioId\":\"%s\",\"startTime\":%d,\"endTime\":%d,\"pageSize\":10}",
                    portfolioId, startTime, endTime
            );

            logger.info("开始调用API获取订单信息，交易员: {}, portfolioId: {}, 请求参数: {}", traderName, portfolioId, requestBody);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/110.0.0.0 Safari/537.36");
            headers.set("Accept", "application/json, text/plain, */*");
            headers.set("Accept-Encoding", "gzip, deflate, br");
            headers.set("Referer", "https://www.binance.com/en/copy-trade/lead/4777921357644221697");
            headers.set("Origin", "https://www.binance.com");
            headers.set("sec-ch-ua", "\"Google Chrome\";v=\"110\", \"Not A(Brand\";v=\"24\", \"Chromium\";v=\"110\"");
            headers.set("sec-ch-ua-mobile", "?0");
            headers.set("sec-ch-ua-platform", "\"Windows\"");
            headers.set("Sec-Fetch-Dest", "empty");
            headers.set("Sec-Fetch-Mode", "cors");
            headers.set("Sec-Fetch-Site", "same-site");
            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

            // 发送请求 - 使用正确的API端点
            ResponseEntity<byte[]> response;
            try {
                response = restTemplate.exchange(
                        "https://www.binance.com/bapi/futures/v1/friendly/future/copy-trade/lead-portfolio/order-history",
                        HttpMethod.POST,
                        entity,
                        byte[].class  // 获取原始字节数组响应
                );
            } catch (org.springframework.web.client.ResourceAccessException e) {
                logger.error("API请求失败，交易员: {}，原因: {}", traderName, e.getMessage());
                
                // 记录API失败告警
                systemAlertService.recordApiFailure("BinanceOrderHistory", 
                    String.format("交易员: %s, 错误: %s", traderName, e.getMessage()));

                // 如果还有重试次数，稍后重试
                if (remainingRetries > 0) {
                    logger.warn("剩余重试次数: {}，等待 {} ms 后重试", remainingRetries, tradeWiseProperties.getRetryDelayMs());
                    try {
                        TimeUnit.MILLISECONDS.sleep(tradeWiseProperties.getRetryDelayMs());
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        logger.error("重试等待被中断");
                        return new java.util.ArrayList<>();
                    }

                    // 递归重试
                    return fetchAndSaveOrdersWithRetry(traderId, traderName, portfolioId, remainingRetries - 1);
                }

                throw e; // 抛出异常以便上层处理
            }

            logger.info("API调用完成，交易员: {}, portfolioId: {}, 响应状态: {}", traderName, portfolioId, response.getStatusCode());

            // 记录成功的API调用
            long processingTime = System.currentTimeMillis() - methodStartTime;
            systemHealthService.recordSuccessfulApiCall(processingTime);
            
            // 重置API错误计数器
            systemAlertService.resetErrorCounter("API_BinanceOrderHistory");

            // 获取响应体字节数组
            byte[] rawResponseBody = response.getBody();
            if (rawResponseBody == null || rawResponseBody.length == 0) {
                logger.error("API返回空响应，交易员: {}, portfolioId: {}", traderName, portfolioId);
                return new java.util.ArrayList<>();
            }

            // 检查是否是GZIP压缩数据（通过检查魔数）
            byte[] decompressedData = rawResponseBody;
            if (rawResponseBody.length > 2 && rawResponseBody[0] == (byte) 0x1f && rawResponseBody[1] == (byte) 0x8b) {
                // 这是一个GZIP压缩的数据，需要解压
                try (GZIPInputStream gis = new GZIPInputStream(new java.io.ByteArrayInputStream(rawResponseBody))) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = gis.read(buffer)) > 0) {
                        baos.write(buffer, 0, len);
                    }
                    decompressedData = baos.toByteArray();
                    logger.debug("成功解压GZIP响应数据，原始大小: {}, 解压后大小: {}", rawResponseBody.length, decompressedData.length);
                } catch (IOException e) {
                    logger.error("解压响应数据失败", e);
                    return new java.util.ArrayList<>();
                }
            } else {
                logger.debug("响应数据未压缩，大小: {}", rawResponseBody.length);
            }

            // 将字节数组转换为字符串
            String responseBody = new String(decompressedData, StandardCharsets.UTF_8);
            logger.debug("API原始响应数据: {}", responseBody);

            // 尝试解析JSON
            OrderHistoryResponse orderResponse;
            try {
                orderResponse = objectMapper.readValue(responseBody, OrderHistoryResponse.class);
            } catch (Exception e) {
                logger.error("JSON解析失败，响应内容: {}", responseBody);
                throw e;
            }

            if (orderResponse != null && orderResponse.isSuccess()) {
                java.util.List<OrderHistoryResponse.OrderItemDTO> apiOrders = orderResponse.getData().getList();

                if (apiOrders == null || apiOrders.isEmpty()) {
                    logger.info("API返回空订单列表，交易员: {}, portfolioId: {}", traderName, portfolioId);
                    return new java.util.ArrayList<>();
                }

                logger.info("API返回订单数量: {}, 交易员: {}, portfolioId: {}", apiOrders.size(), traderName, portfolioId);

                List<Order> newOrders = new java.util.ArrayList<>();

                for (OrderHistoryResponse.OrderItemDTO apiOrder : apiOrders) {
                    // 使用orderTime作为外部订单ID（这应该是唯一的）
                    String externalOrderId = String.valueOf(apiOrder.getOrderTime());

                    // 检查订单是否已存在于数据库中
                    Order existingOrder = orderMapper.findByExternalOrderId(externalOrderId);
                    if (existingOrder == null) {
                        // 创建新订单实体
                        Order order = new Order(
                                externalOrderId,
                                traderId,
                                traderName,
                                apiOrder.getSymbol(),
                                apiOrder.getBaseAsset(),
                                apiOrder.getQuoteAsset(),
                                apiOrder.getSide(),
                                apiOrder.getType(),
                                apiOrder.getPositionSide(),
                                apiOrder.getExecutedQty() != null ? BigDecimal.valueOf(apiOrder.getExecutedQty()) : BigDecimal.ZERO,
                                apiOrder.getAvgPrice() != null ? BigDecimal.valueOf(apiOrder.getAvgPrice()) : BigDecimal.ZERO,
                                apiOrder.getTotalPnl() != null ? BigDecimal.valueOf(apiOrder.getTotalPnl()) : BigDecimal.ZERO,
                                apiOrder.getOrderUpdateTime(),
                                apiOrder.getOrderTime()
                        );

                        // 保存到数据库
                        orderMapper.insertOrUpdate(order);

                        newOrders.add(order);

                        logger.debug("新增订单: 交易对={}, 方向={}, 类型={}, 数量={}, 价格={}, 时间={}",
                                order.getSymbol(), order.getSide(), order.getType(),
                                order.getExecutedQty(), order.getAvgPrice(),
                                new java.util.Date(order.getOrderTime()));
                    } else {
                        logger.debug("已存在的订单，跳过: 交易对={}, 时间={}",
                                apiOrder.getSymbol(), new java.util.Date(apiOrder.getOrderTime()));
                    }
                }

                logger.info("找到 {} 个新订单，交易员: {}", newOrders.size(), traderName);

                // 记录处理的订单数量
                systemHealthService.recordOrdersProcessed(newOrders.size());

                // 如果有新订单，发送邮件通知
                if (!newOrders.isEmpty()) {
                    emailService.sendNewOrdersNotificationWithQualityAssessment(newOrders, traderName, signalEvaluator);
                }

                return newOrders;
            } else {
                logger.error("API调用失败，交易员: {}, portfolioId: {}, 响应: {}", traderName, portfolioId, orderResponse);
                return new java.util.ArrayList<>();
            }
        } catch (Exception e) {
            logger.error("获取订单时发生错误，交易员: " + traderName, e);
            
            // 记录系统异常
            systemAlertService.recordSystemException("OrderService", 
                String.format("获取订单失败: %s", traderName), e);

            // 记录失败的API调用
            long processingTime = System.currentTimeMillis() - methodStartTime;
            systemHealthService.recordFailedApiCall();

            // 检查是否是API速率限制错误（429），如果是则进行重试
            if (e instanceof org.springframework.web.client.HttpClientErrorException &&
                    ((org.springframework.web.client.HttpClientErrorException) e).getStatusCode().value() == 429 &&
                    remainingRetries > 0) {

                logger.warn("API速率限制错误 (429)，剩余重试次数: {}，等待 {} ms 后重试",
                        remainingRetries, tradeWiseProperties.getRetryDelayMs());

                try {
                    TimeUnit.MILLISECONDS.sleep(tradeWiseProperties.getRetryDelayMs());
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    logger.error("重试等待被中断");
                    return new java.util.ArrayList<>();
                }

                // 递归重试
                return fetchAndSaveOrdersWithRetry(traderId, traderName, portfolioId, remainingRetries - 1);
            }

            // 如果是其他网络连接错误，可能是网络配置问题
            if (e instanceof org.springframework.web.client.ResourceAccessException) {
                logger.error("网络错误 - 请检查您的互联网连接和防火墙设置。错误详情: {}", e.getMessage());
            }

            // 记录API错误统计
            systemHealthService.recordFailedApiCall();

            return new java.util.ArrayList<>();
        }
    }

    /**
     * 异步批量处理多个交易员的订单
     */
    public java.util.concurrent.CompletableFuture<Void> fetchOrdersForAllTraders(java.util.List<com.example.tradewise.entity.TraderConfig> traders) {
        // 记录监控的交易员数量
        systemHealthService.recordTradersMonitored(traders.size());

        java.util.List<java.util.concurrent.CompletableFuture<java.util.List<Order>>> futures = new java.util.ArrayList<>();
        for (com.example.tradewise.entity.TraderConfig trader : traders) {
            futures.add(java.util.concurrent.CompletableFuture.supplyAsync(() -> fetchAndSaveOrders(trader.getTraderId(), trader.getName(), trader.getPortfolioId())));
        }

        return java.util.concurrent.CompletableFuture.allOf(futures.toArray(new java.util.concurrent.CompletableFuture[futures.size()]));
    }
}
