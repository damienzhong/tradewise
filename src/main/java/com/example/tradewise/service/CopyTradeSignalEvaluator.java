package com.example.tradewise.service;

import com.example.tradewise.entity.Order;
import com.example.tradewise.service.MarketAnalysisService.Candlestick;
import com.example.tradewise.service.MarketAnalysisService.TradingSignal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * 跟单信号质量评估服务
 * 在发送跟单邮件前评估信号质量
 */
@Service
public class CopyTradeSignalEvaluator {

    private static final Logger logger = LoggerFactory.getLogger(CopyTradeSignalEvaluator.class);

    @Autowired
    private MarketAnalysisService marketAnalysisService;

    @Autowired
    private MarketDataService marketDataService;

    /**
     * 评估跟单信号质量
     */
    public SignalQualityAssessment evaluateSignalQuality(Order order) {
        try {
            // 获取市场数据
            List<Candlestick> candlesticks = marketDataService.getKlines(order.getSymbol(), "1h", 100);
            if (candlesticks == null || candlesticks.isEmpty()) {
                return createLowQualityAssessment("无法获取市场数据");
            }

            // 生成当前市场信号
            List<TradingSignal> marketSignals = marketAnalysisService.generateSignalsForSymbol(order.getSymbol(), false);
            TradingSignal currentMarketSignal = marketSignals.isEmpty() ? null : marketSignals.get(0);

            // 计算各项评估指标
            double trendAlignment = calculateTrendAlignment(order, candlesticks);
            double marketSignalAlignment = calculateMarketSignalAlignment(order, currentMarketSignal);
            double volumeConfirmation = calculateVolumeConfirmation(order, candlesticks);
            double technicalSupport = calculateTechnicalSupport(order, candlesticks);
            double riskRewardRatio = calculateRiskRewardRatio(order, candlesticks);

            // 计算综合评分 (0-10分)
            double totalScore = (trendAlignment * 0.25 + marketSignalAlignment * 0.3 +
                    volumeConfirmation * 0.2 + technicalSupport * 0.15 +
                    riskRewardRatio * 0.1) * 10;

            // 确定质量等级
            String qualityLevel = determineQualityLevel(totalScore);
            String recommendation = generateRecommendation(totalScore, order);

            return new SignalQualityAssessment(
                    order.getSymbol(),
                    totalScore,
                    qualityLevel,
                    recommendation,
                    trendAlignment,
                    marketSignalAlignment,
                    volumeConfirmation,
                    technicalSupport,
                    riskRewardRatio,
                    currentMarketSignal != null ? currentMarketSignal.getSignalType().toString() : "无信号",
                    currentMarketSignal != null ? currentMarketSignal.getReason() : "当前无明确市场信号"
            );

        } catch (Exception e) {
            logger.error("评估信号质量时发生错误: {}", e.getMessage());
            return createLowQualityAssessment("评估过程中发生错误: " + e.getMessage());
        }
    }

    private double calculateTrendAlignment(Order order, List<Candlestick> candlesticks) {
        if (candlesticks.size() < 20) return 0.5;

        // 计算短期和长期EMA
        double[] prices = candlesticks.stream().mapToDouble(c -> c.getClose()).toArray();
        double shortEma = calculateEMA(prices, 12);
        double longEma = calculateEMA(prices, 26);

        boolean isUptrend = shortEma > longEma;
        boolean isBuyOrder = "BUY".equals(order.getSide());

        return (isUptrend == isBuyOrder) ? 0.8 : 0.2;
    }

    private double calculateMarketSignalAlignment(Order order, TradingSignal marketSignal) {
        if (marketSignal == null) return 0.5;

        boolean orderIsBuy = "BUY".equals(order.getSide());
        boolean signalIsBuy = "BUY".equals(marketSignal.getSignalType().toString());

        if (orderIsBuy == signalIsBuy) {
            return Math.min(1.0, marketSignal.getScore() / 10.0);
        } else {
            return 0.1;
        }
    }

    private double calculateVolumeConfirmation(Order order, List<Candlestick> candlesticks) {
        if (candlesticks.size() < 5) return 0.5;

        // 计算最近5根K线的平均成交量
        double avgVolume = candlesticks.subList(candlesticks.size() - 5, candlesticks.size())
                .stream().mapToDouble(c -> c.getVolume()).average().orElse(0);

        // 获取最新K线成交量
        double latestVolume = candlesticks.get(candlesticks.size() - 1).getVolume();

        double volumeRatio = avgVolume > 0 ? latestVolume / avgVolume : 1.0;
        return Math.min(1.0, volumeRatio / 2.0);
    }

    private double calculateTechnicalSupport(Order order, List<Candlestick> candlesticks) {
        if (candlesticks.size() < 20) return 0.5;

        double currentPrice = candlesticks.get(candlesticks.size() - 1).getClose();
        double[] prices = candlesticks.stream().mapToDouble(c -> c.getClose()).toArray();

        // 计算RSI
        double rsi = calculateRSI(prices, 14);

        // 计算布林带
        double[] bb = calculateBollingerBands(prices, 20, 2.0);
        double bbUpper = bb[0], bbMiddle = bb[1], bbLower = bb[2];

        double score = 0.5;

        // RSI评估
        if ("BUY".equals(order.getSide())) {
            if (rsi < 30) score += 0.3; // 超卖区买入
            else if (rsi > 70) score -= 0.2; // 超买区买入
        } else {
            if (rsi > 70) score += 0.3; // 超买区卖出
            else if (rsi < 30) score -= 0.2; // 超卖区卖出
        }

        // 布林带评估
        if ("BUY".equals(order.getSide()) && currentPrice < bbLower) {
            score += 0.2; // 价格在下轨附近买入
        } else if ("SELL".equals(order.getSide()) && currentPrice > bbUpper) {
            score += 0.2; // 价格在上轨附近卖出
        }

        return Math.max(0, Math.min(1.0, score));
    }

    private double calculateRiskRewardRatio(Order order, List<Candlestick> candlesticks) {
        if (candlesticks.size() < 20) return 0.5;

        double currentPrice = candlesticks.get(candlesticks.size() - 1).getClose();
        double[] prices = candlesticks.stream().mapToDouble(c -> c.getClose()).toArray();
        double[] lows = candlesticks.stream().mapToDouble(c -> c.getLow()).toArray();

        // 计算ATR作为风险度量
        double atr = calculateATR(candlesticks, 14);
        double riskReward = atr > 0 ? (atr * 2) / atr : 1.0; // 假设目标是2倍ATR

        return Math.min(1.0, riskReward / 3.0);
    }

    private String determineQualityLevel(double score) {
        if (score >= 8.0) return "优秀";
        if (score >= 6.5) return "良好";
        if (score >= 5.0) return "一般";
        if (score >= 3.5) return "较差";
        return "很差";
    }

    private String generateRecommendation(double score, Order order) {
        if (score >= 8.0) {
            return "强烈推荐跟单，信号质量优秀，多项指标确认";
        } else if (score >= 6.5) {
            return "推荐跟单，信号质量良好，风险可控";
        } else if (score >= 5.0) {
            return "谨慎跟单，信号质量一般，建议降低仓位";
        } else if (score >= 3.5) {
            return "不建议跟单，信号质量较差，风险较高";
        } else {
            return "强烈不建议跟单，信号质量很差，可能逆势操作";
        }
    }

    private SignalQualityAssessment createLowQualityAssessment(String reason) {
        return new SignalQualityAssessment(
                "未知", 2.0, "很差", "无法评估: " + reason,
                0.0, 0.0, 0.0, 0.0, 0.0, "无信号", reason
        );
    }

    // 技术指标计算方法
    private double calculateEMA(double[] prices, int period) {
        if (prices.length < period) return prices[prices.length - 1];

        double multiplier = 2.0 / (period + 1);
        double ema = prices[0];

        for (int i = 1; i < prices.length; i++) {
            ema = (prices[i] * multiplier) + (ema * (1 - multiplier));
        }

        return ema;
    }

    private double calculateRSI(double[] prices, int period) {
        if (prices.length < period + 1) return 50.0;

        double avgGain = 0, avgLoss = 0;

        for (int i = 1; i <= period; i++) {
            double change = prices[i] - prices[i - 1];
            if (change > 0) avgGain += change;
            else avgLoss += Math.abs(change);
        }

        avgGain /= period;
        avgLoss /= period;

        if (avgLoss == 0) return 100.0;

        double rs = avgGain / avgLoss;
        return 100 - (100 / (1 + rs));
    }

    private double[] calculateBollingerBands(double[] prices, int period, double stdMultiplier) {
        if (prices.length < period) {
            double price = prices[prices.length - 1];
            return new double[]{price, price, price};
        }

        double sum = 0;
        for (int i = prices.length - period; i < prices.length; i++) {
            sum += prices[i];
        }
        double sma = sum / period;

        double variance = 0;
        for (int i = prices.length - period; i < prices.length; i++) {
            variance += Math.pow(prices[i] - sma, 2);
        }
        double std = Math.sqrt(variance / period);

        return new double[]{
                sma + (std * stdMultiplier), // Upper band
                sma,                         // Middle band (SMA)
                sma - (std * stdMultiplier)  // Lower band
        };
    }

    private double calculateATR(List<Candlestick> candlesticks, int period) {
        if (candlesticks.size() < period + 1) return 0.0;

        double atr = 0;
        for (int i = candlesticks.size() - period; i < candlesticks.size(); i++) {
            Candlestick current = candlesticks.get(i);
            Candlestick previous = i > 0 ? candlesticks.get(i - 1) : current;

            double tr1 = current.getHigh() - current.getLow();
            double tr2 = Math.abs(current.getHigh() - previous.getClose());
            double tr3 = Math.abs(current.getLow() - previous.getClose());

            atr += Math.max(tr1, Math.max(tr2, tr3));
        }

        return atr / period;
    }

    /**
     * 信号质量评估结果
     */
    public static class SignalQualityAssessment {
        private final String symbol;
        private final double totalScore;
        private final String qualityLevel;
        private final String recommendation;
        private final double trendAlignment;
        private final double marketSignalAlignment;
        private final double volumeConfirmation;
        private final double technicalSupport;
        private final double riskRewardRatio;
        private final String currentMarketSignal;
        private final String marketSignalDescription;

        public SignalQualityAssessment(String symbol, double totalScore, String qualityLevel,
                                       String recommendation, double trendAlignment,
                                       double marketSignalAlignment, double volumeConfirmation,
                                       double technicalSupport, double riskRewardRatio,
                                       String currentMarketSignal, String marketSignalDescription) {
            this.symbol = symbol;
            this.totalScore = totalScore;
            this.qualityLevel = qualityLevel;
            this.recommendation = recommendation;
            this.trendAlignment = trendAlignment;
            this.marketSignalAlignment = marketSignalAlignment;
            this.volumeConfirmation = volumeConfirmation;
            this.technicalSupport = technicalSupport;
            this.riskRewardRatio = riskRewardRatio;
            this.currentMarketSignal = currentMarketSignal;
            this.marketSignalDescription = marketSignalDescription;
        }

        // Getters
        public String getSymbol() { return symbol; }
        public double getTotalScore() { return totalScore; }
        public String getQualityLevel() { return qualityLevel; }
        public String getRecommendation() { return recommendation; }
        public double getTrendAlignment() { return trendAlignment; }
        public double getMarketSignalAlignment() { return marketSignalAlignment; }
        public double getVolumeConfirmation() { return volumeConfirmation; }
        public double getTechnicalSupport() { return technicalSupport; }
        public double getRiskRewardRatio() { return riskRewardRatio; }
        public String getCurrentMarketSignal() { return currentMarketSignal; }
        public String getMarketSignalDescription() { return marketSignalDescription; }

        public String getFormattedScore() {
            return String.format("%.1f", totalScore);
        }

        public String getScoreColor() {
            if (totalScore >= 8.0) return "#28a745"; // 绿色
            if (totalScore >= 6.5) return "#17a2b8"; // 蓝色
            if (totalScore >= 5.0) return "#ffc107"; // 黄色
            if (totalScore >= 3.5) return "#fd7e14"; // 橙色
            return "#dc3545"; // 红色
        }
    }
}