package com.example.tradewise.service;

import com.example.tradewise.service.DataEngine.MarketRegime;
import com.example.tradewise.service.DataEngine.SignalState;
import com.example.tradewise.service.MarketAnalysisService.Candlestick;
import com.example.tradewise.service.MarketAnalysisService.TechnicalIndicators;
import com.example.tradewise.service.SignalDetector.MarketContext;
import com.example.tradewise.service.SignalDetector.SignalDetectionResult;
import com.example.tradewise.service.SignalDetector.SignalType;

import java.util.*;

/**
 * BOS (Break of Structure) 结构突破检测器
 * 检测价格突破关键摆动高低点的信号
 */
public class BosBreakoutDetector implements SignalDetector {
    
    @Override
    public SignalType getType() {
        return SignalType.BOS_BREAKOUT;
    }
    
    @Override
    public boolean allowedIn(MarketRegime regime) {
        // 在强趋势和弱趋势市场中允许BOS信号
        return regime == MarketRegime.STRONG_TREND || regime == MarketRegime.WEAK_TREND;
    }
    
    @Override
    public Optional<SignalDetectionResult> detect(MarketContext ctx) {
        String symbol = ctx.getSymbol();
        Map<String, List<Candlestick>> multiTimeframeData = ctx.getMultiTimeframeData();
        
        // 主要在15分钟图上检测BOS
        List<Candlestick> fifteenMinData = multiTimeframeData.get("15m");
        if (fifteenMinData == null || fifteenMinData.size() < 10) {
            return Optional.empty();
        }
        
        Candlestick currentCandle = fifteenMinData.get(fifteenMinData.size() - 1);
        double currentPrice = currentCandle.getClose();
        
        // 检测BOS
        boolean isBosDetected = detectBOS(fifteenMinData, currentPrice);
        
        if (isBosDetected) {
            // 确定信号方向
            String direction = getBosDirection(fifteenMinData, currentPrice);
            
            // 计算信号强度
            double strength = calculateBosStrength(fifteenMinData, currentPrice);
            
            // 生成信号原因
            String reason = String.format("%s BOS信号: 价格%.4f突破关键摆动点", 
                                        direction.equals("BUY") ? "看涨" : "看跌", currentPrice);
            
            // 生成操作建议
            String suggestion = direction.equals("BUY") ? 
                "建议开多单，注意确认突破有效性" : 
                "建议开空单，注意确认突破有效性";
            
            // 生成信号解释
            Map<String, Object> explanation = new HashMap<>();
            explanation.put("type", "BOS_BREAKOUT");
            explanation.put("timeframe", "15m");
            explanation.put("direction", direction);
            explanation.put("swing_points", detectSwingPoints(fifteenMinData));
            explanation.put("volume_confirmation", checkVolumeConfirmation(fifteenMinData));
            
            SignalDetectionResult result = new SignalDetectionResult(
                SignalType.BOS_BREAKOUT,
                direction.equals("BUY") ? "BOS-BREAKOUT-UP" : "BOS-BREAKOUT-DOWN",
                strength,
                reason,
                suggestion,
                currentPrice,
                explanation
            );
            
            return Optional.of(result);
        }
        
        return Optional.empty();
    }
    
    /**
     * 检测BOS (结构突破)
     */
    private boolean detectBOS(List<Candlestick> fifteenMinData, double currentPrice) {
        if (fifteenMinData.size() < 10) {
            return false;
        }

        // 查找最近的摆动高点和低点
        List<Double> swingHighs = new ArrayList<>();
        List<Double> swingLows = new ArrayList<>();

        for (int i = 2; i < fifteenMinData.size() - 2; i++) {
            Candlestick current = fifteenMinData.get(i);
            Candlestick prev1 = fifteenMinData.get(i - 1);
            Candlestick prev2 = fifteenMinData.get(i - 2);
            Candlestick next1 = fifteenMinData.get(i + 1);
            Candlestick next2 = fifteenMinData.get(i + 2);

            // 检测摆动高点（当前高点比相邻两个高点都高）
            if (current.getHigh() > prev1.getHigh() && current.getHigh() > prev2.getHigh() &&
                current.getHigh() > next1.getHigh() && current.getHigh() > next2.getHigh()) {
                swingHighs.add(current.getHigh());
            }

            // 检测摆动低点（当前低点比相邻两个低点都低）
            if (current.getLow() < prev1.getLow() && current.getLow() < prev2.getLow() &&
                current.getLow() < next1.getLow() && current.getLow() < next2.getLow()) {
                swingLows.add(current.getLow());
            }
        }

        // 检查当前价格是否突破了最近的摆动点
        if (!swingHighs.isEmpty() && currentPrice > swingHighs.get(swingHighs.size() - 1)) {
            return true; // 向上突破结构
        }
        if (!swingLows.isEmpty() && currentPrice < swingLows.get(swingLows.size() - 1)) {
            return true; // 向下突破结构
        }

        return false;
    }
    
    /**
     * 获取BOS方向
     */
    private String getBosDirection(List<Candlestick> fifteenMinData, double currentPrice) {
        if (fifteenMinData.size() < 10) {
            return "NEUTRAL";
        }

        // 查找最近的摆动高点和低点
        List<Double> swingHighs = new ArrayList<>();
        List<Double> swingLows = new ArrayList<>();

        for (int i = 2; i < fifteenMinData.size() - 2; i++) {
            Candlestick current = fifteenMinData.get(i);
            Candlestick prev1 = fifteenMinData.get(i - 1);
            Candlestick prev2 = fifteenMinData.get(i - 2);
            Candlestick next1 = fifteenMinData.get(i + 1);
            Candlestick next2 = fifteenMinData.get(i + 2);

            // 检测摆动高点
            if (current.getHigh() > prev1.getHigh() && current.getHigh() > prev2.getHigh() &&
                current.getHigh() > next1.getHigh() && current.getHigh() > next2.getHigh()) {
                swingHighs.add(current.getHigh());
            }

            // 检测摆动低点
            if (current.getLow() < prev1.getLow() && current.getLow() < prev2.getLow() &&
                current.getLow() < next1.getLow() && current.getLow() < next2.getLow()) {
                swingLows.add(current.getLow());
            }
        }

        // 检查突破方向
        if (!swingHighs.isEmpty() && currentPrice > swingHighs.get(swingHighs.size() - 1)) {
            return "BUY"; // 向上突破
        }
        if (!swingLows.isEmpty() && currentPrice < swingLows.get(swingLows.size() - 1)) {
            return "SELL"; // 向下突破
        }

        return "NEUTRAL";
    }
    
    /**
     * 计算BOS信号强度
     */
    private double calculateBosStrength(List<Candlestick> fifteenMinData, double currentPrice) {
        if (fifteenMinData.size() < 10) {
            return 0.0;
        }

        // 基础强度
        double baseStrength = 3.0; // BOS信号基础强度为3分
        
        // 检查成交量确认
        if (checkVolumeConfirmation(fifteenMinData)) {
            baseStrength += 1.0; // 成交量确认加1分
        }
        
        // 检查突破幅度
        double breakthroughMagnitude = calculateBreakthroughMagnitude(fifteenMinData, currentPrice);
        if (breakthroughMagnitude > 0.01) { // 如果突破幅度大于1%
            baseStrength += 1.0; // 幅度确认加1分
        }
        
        // 限制最大强度为5分
        return Math.min(baseStrength, 5.0);
    }
    
    /**
     * 检查成交量确认
     */
    private boolean checkVolumeConfirmation(List<Candlestick> fifteenMinData) {
        if (fifteenMinData.size() < 20) {
            return false;
        }

        // 计算最近20根K线的平均成交量
        double volumeSum = 0;
        for (int i = Math.max(0, fifteenMinData.size() - 20); i < fifteenMinData.size(); i++) {
            volumeSum += fifteenMinData.get(i).getVolume();
        }
        double avgVolume = volumeSum / Math.min(20, fifteenMinData.size());

        // 检查当前成交量是否超过均量
        double currentVolume = fifteenMinData.get(fifteenMinData.size() - 1).getVolume();
        return currentVolume > avgVolume * 1.2; // 当前成交量超过均量20%
    }
    
    /**
     * 计算突破幅度
     */
    private double calculateBreakthroughMagnitude(List<Candlestick> fifteenMinData, double currentPrice) {
        if (fifteenMinData.size() < 10) {
            return 0.0;
        }

        // 查找最近的摆动高点和低点
        List<Double> swingHighs = new ArrayList<>();
        List<Double> swingLows = new ArrayList<>();

        for (int i = 2; i < fifteenMinData.size() - 2; i++) {
            Candlestick current = fifteenMinData.get(i);
            Candlestick prev1 = fifteenMinData.get(i - 1);
            Candlestick prev2 = fifteenMinData.get(i - 2);
            Candlestick next1 = fifteenMinData.get(i + 1);
            Candlestick next2 = fifteenMinData.get(i + 2);

            // 检测摆动高点
            if (current.getHigh() > prev1.getHigh() && current.getHigh() > prev2.getHigh() &&
                current.getHigh() > next1.getHigh() && current.getHigh() > next2.getHigh()) {
                swingHighs.add(current.getHigh());
            }

            // 检测摆动低点
            if (current.getLow() < prev1.getLow() && current.getLow() < prev2.getLow() &&
                current.getLow() < next1.getLow() && current.getLow() < next2.getLow()) {
                swingLows.add(current.getLow());
            }
        }

        // 计算突破幅度
        if (!swingHighs.isEmpty() && currentPrice > swingHighs.get(swingHighs.size() - 1)) {
            // 向上突破
            double lastSwingHigh = swingHighs.get(swingHighs.size() - 1);
            return (currentPrice - lastSwingHigh) / lastSwingHigh;
        } else if (!swingLows.isEmpty() && currentPrice < swingLows.get(swingLows.size() - 1)) {
            // 向下突破
            double lastSwingLow = swingLows.get(swingLows.size() - 1);
            return (lastSwingLow - currentPrice) / lastSwingLow;
        }

        return 0.0;
    }
    
    /**
     * 检测摆动点
     */
    private List<Map<String, Object>> detectSwingPoints(List<Candlestick> fifteenMinData) {
        List<Map<String, Object>> swingPoints = new ArrayList<>();
        
        for (int i = 2; i < fifteenMinData.size() - 2; i++) {
            Candlestick current = fifteenMinData.get(i);
            Candlestick prev1 = fifteenMinData.get(i - 1);
            Candlestick prev2 = fifteenMinData.get(i - 2);
            Candlestick next1 = fifteenMinData.get(i + 1);
            Candlestick next2 = fifteenMinData.get(i + 2);

            // 检测摆动高点
            if (current.getHigh() > prev1.getHigh() && current.getHigh() > prev2.getHigh() &&
                current.getHigh() > next1.getHigh() && current.getHigh() > next2.getHigh()) {
                Map<String, Object> point = new HashMap<>();
                point.put("type", "HIGH");
                point.put("price", current.getHigh());
                point.put("time", current.getOpenTime());
                swingPoints.add(point);
            }

            // 检测摆动低点
            if (current.getLow() < prev1.getLow() && current.getLow() < prev2.getLow() &&
                current.getLow() < next1.getLow() && current.getLow() < next2.getLow()) {
                Map<String, Object> point = new HashMap<>();
                point.put("type", "LOW");
                point.put("price", current.getLow());
                point.put("time", current.getOpenTime());
                swingPoints.add(point);
            }
        }
        
        return swingPoints;
    }
}