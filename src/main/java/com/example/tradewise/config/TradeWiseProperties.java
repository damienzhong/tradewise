package com.example.tradewise.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "tradewise")
public class TradeWiseProperties {
    
    private String baseUrl = "https://www.binance.com";
    private String orderHistoryEndpoint = "/bapi/futures/v1/friendly/future/copy-trade/lead-portfolio/order-history";
    private int scanInterval = 10; // 扫描间隔（秒）
    private int maxConcurrentRequests = 5; // 最大并发请求数
    private int requestTimeoutSeconds = 30; // 请求超时时间（秒）
    private int retryAttempts = 3; // 重试次数
    private long retryDelayMs = 1000; // 重试延迟（毫秒）
    private MarketAnalysis marketAnalysis = new MarketAnalysis();
    private Email email = new Email();
    // 交易员配置现在完全从数据库获取，不再使用配置文件中的列表
    
    // Getters and Setters
    public String getBaseUrl() {
        return baseUrl;
    }
    
    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
    
    public String getOrderHistoryEndpoint() {
        return orderHistoryEndpoint;
    }
    
    public void setOrderHistoryEndpoint(String orderHistoryEndpoint) {
        this.orderHistoryEndpoint = orderHistoryEndpoint;
    }
    
    public int getScanInterval() {
        return scanInterval;
    }
    
    public void setScanInterval(int scanInterval) {
        this.scanInterval = scanInterval;
    }
    
    public int getMaxConcurrentRequests() {
        return maxConcurrentRequests;
    }
    
    public void setMaxConcurrentRequests(int maxConcurrentRequests) {
        this.maxConcurrentRequests = maxConcurrentRequests;
    }
    
    public int getRequestTimeoutSeconds() {
        return requestTimeoutSeconds;
    }
    
    public void setRequestTimeoutSeconds(int requestTimeoutSeconds) {
        this.requestTimeoutSeconds = requestTimeoutSeconds;
    }
    
    public int getRetryAttempts() {
        return retryAttempts;
    }
    
    public void setRetryAttempts(int retryAttempts) {
        this.retryAttempts = retryAttempts;
    }
    
    public long getRetryDelayMs() {
        return retryDelayMs;
    }
    
    public void setRetryDelayMs(long retryDelayMs) {
        this.retryDelayMs = retryDelayMs;
    }
    
    public MarketAnalysis getMarketAnalysis() {
        return marketAnalysis;
    }
    
    public void setMarketAnalysis(MarketAnalysis marketAnalysis) {
        this.marketAnalysis = marketAnalysis;
    }
    
    public Email getEmail() {
        return email;
    }
    
    public void setEmail(Email email) {
        this.email = email;
    }
    
    
    public static class Email {
        private String from;
        private boolean enabled = true; // 是否启用邮件发送
        
        // Getters and Setters
        public String getFrom() {
            return from;
        }
        
        public void setFrom(String from) {
            this.from = from;
        }
        
        public boolean isEnabled() {
            return enabled;
        }
        
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
    
    public static class MarketAnalysis {
        private boolean enabled = true; // 是否启用市场分析功能
        private String[] symbolsToMonitor = {"BTCUSDT", "ETHUSDT", "BNBUSDT", "SOLUSDT", "XRPUSDT"}; // 要监控的交易对
        private String interval = "1h"; // K线周期
        private int limit = 100; // 获取K线的数量
        private int analysisIntervalMinutes = 1; // 分析间隔（分钟）
        
        // 技术指标参数
        private int emaShortPeriod = 12; // 短期EMA周期
        private int emaLongPeriod = 26;  // 长期EMA周期
        private int rsiPeriod = 14;      // RSI周期
        private int bbPeriod = 20;       // 布林带周期
        private double bbStdMultiplier = 2.0; // 布林带标准差倍数
        private int atrPeriod = 14;      // ATR周期
        private double minVolumeRatio = 1.2; // 最小成交量比率
        private int trendStrengthPeriod = 50; // 趋势强度计算周期
        
        private int signalConfirmationThreshold = 2; // 信号确认阈值，至少需要多少个指标确认
        
        // 多时间框架分析参数
        private boolean fastAnalysisEnabled = false; // 是否启用快速扫描
        private int fastAnalysisIntervalMinutes = 5; // 快速扫描间隔（分钟）
        private boolean deepAnalysisEnabled = true; // 是否启用深度分析
        private int deepAnalysisIntervalHours = 1; // 深度分析间隔（小时）
        
        // 信号频率控制参数
        private SignalFrequencyControl signalFrequencyControl = new SignalFrequencyControl();
        private boolean dataEngineEnabled = true; // 是否启用数据引擎
        private MultiTimeframeAnalysis multiTimeframeAnalysis = new MultiTimeframeAnalysis();
        private RiskManagement riskManagement = new RiskManagement();
        
        // Getters and Setters
        public boolean isEnabled() {
            return enabled;
        }
        
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
        
        public boolean isDataEngineEnabled() {
            return dataEngineEnabled;
        }
        
        public void setDataEngineEnabled(boolean dataEngineEnabled) {
            this.dataEngineEnabled = dataEngineEnabled;
        }
        
        public MultiTimeframeAnalysis getMultiTimeframeAnalysis() {
            return multiTimeframeAnalysis;
        }
        
        public void setMultiTimeframeAnalysis(MultiTimeframeAnalysis multiTimeframeAnalysis) {
            this.multiTimeframeAnalysis = multiTimeframeAnalysis;
        }
        
        public RiskManagement getRiskManagement() {
            return riskManagement;
        }
        
        public void setRiskManagement(RiskManagement riskManagement) {
            this.riskManagement = riskManagement;
        }
        
        public SignalFrequencyControl getSignalFrequencyControl() {
            return signalFrequencyControl;
        }
        
        public void setSignalFrequencyControl(SignalFrequencyControl signalFrequencyControl) {
            this.signalFrequencyControl = signalFrequencyControl;
        }
        
        public String getInterval() {
            return interval;
        }
        
        public void setInterval(String interval) {
            this.interval = interval;
        }
        
        public int getLimit() {
            return limit;
        }
        
        public void setLimit(int limit) {
            this.limit = limit;
        }
        
        public int getAnalysisIntervalMinutes() {
            return analysisIntervalMinutes;
        }
        
        public void setAnalysisIntervalMinutes(int analysisIntervalMinutes) {
            this.analysisIntervalMinutes = analysisIntervalMinutes;
        }
        
        public int getEmaShortPeriod() {
            return emaShortPeriod;
        }
        
        public void setEmaShortPeriod(int emaShortPeriod) {
            this.emaShortPeriod = emaShortPeriod;
        }
        
        public int getEmaLongPeriod() {
            return emaLongPeriod;
        }
        
        public void setEmaLongPeriod(int emaLongPeriod) {
            this.emaLongPeriod = emaLongPeriod;
        }
        
        public int getRsiPeriod() {
            return rsiPeriod;
        }
        
        public void setRsiPeriod(int rsiPeriod) {
            this.rsiPeriod = rsiPeriod;
        }
        
        public int getBbPeriod() {
            return bbPeriod;
        }
        
        public void setBbPeriod(int bbPeriod) {
            this.bbPeriod = bbPeriod;
        }
        
        public double getBbStdMultiplier() {
            return bbStdMultiplier;
        }
        
        public void setBbStdMultiplier(double bbStdMultiplier) {
            this.bbStdMultiplier = bbStdMultiplier;
        }
        
        public int getAtrPeriod() {
            return atrPeriod;
        }
        
        public void setAtrPeriod(int atrPeriod) {
            this.atrPeriod = atrPeriod;
        }
        
        public double getMinVolumeRatio() {
            return minVolumeRatio;
        }
        
        public void setMinVolumeRatio(double minVolumeRatio) {
            this.minVolumeRatio = minVolumeRatio;
        }
        
        public int getTrendStrengthPeriod() {
            return trendStrengthPeriod;
        }
        
        public void setTrendStrengthPeriod(int trendStrengthPeriod) {
            this.trendStrengthPeriod = trendStrengthPeriod;
        }
        
        public int getSignalConfirmationThreshold() {
            return signalConfirmationThreshold;
        }
        
        public void setSignalConfirmationThreshold(int signalConfirmationThreshold) {
            this.signalConfirmationThreshold = signalConfirmationThreshold;
        }
        
        public boolean isFastAnalysisEnabled() {
            return fastAnalysisEnabled;
        }
        
        public void setFastAnalysisEnabled(boolean fastAnalysisEnabled) {
            this.fastAnalysisEnabled = fastAnalysisEnabled;
        }
        
        public int getFastAnalysisIntervalMinutes() {
            return fastAnalysisIntervalMinutes;
        }
        
        public void setFastAnalysisIntervalMinutes(int fastAnalysisIntervalMinutes) {
            this.fastAnalysisIntervalMinutes = fastAnalysisIntervalMinutes;
        }
        
        public boolean isDeepAnalysisEnabled() {
            return deepAnalysisEnabled;
        }
        
        public void setDeepAnalysisEnabled(boolean deepAnalysisEnabled) {
            this.deepAnalysisEnabled = deepAnalysisEnabled;
        }
        
        public int getDeepAnalysisIntervalHours() {
            return deepAnalysisIntervalHours;
        }
        
        public void setDeepAnalysisIntervalHours(int deepAnalysisIntervalHours) {
            this.deepAnalysisIntervalHours = deepAnalysisIntervalHours;
        }
    }
    
    public static class SignalFrequencyControl {
        private int level1Threshold = 8; // LEVEL_1信号最低评分阈值
        private int level2Threshold = 6; // LEVEL_2信号最低评分阈值
        private int level3Threshold = 4; // LEVEL_3信号最低评分阈值
        
        // Getters and Setters
        public int getLevel1Threshold() {
            return level1Threshold;
        }
        
        public void setLevel1Threshold(int level1Threshold) {
            this.level1Threshold = level1Threshold;
        }
        
        public int getLevel2Threshold() {
            return level2Threshold;
        }
        
        public void setLevel2Threshold(int level2Threshold) {
            this.level2Threshold = level2Threshold;
        }
        
        public int getLevel3Threshold() {
            return level3Threshold;
        }
        
        public void setLevel3Threshold(int level3Threshold) {
            this.level3Threshold = level3Threshold;
        }
    }
    
    public static class MultiTimeframeAnalysis {
        private boolean enabled = true; // 是否启用多时间框架分析
        private String[] timeframes = {"15m", "1h", "4h"}; // 支持的时间框架
        
        // Getters and Setters
        public boolean isEnabled() {
            return enabled;
        }
        
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
        
        public String[] getTimeframes() {
            return timeframes;
        }
        
        public void setTimeframes(String[] timeframes) {
            this.timeframes = timeframes;
        }
    }
    
    public static class RiskManagement {
        private double minRiskRewardRatio = 1.5; // 最小风险回报比
        private double maxPositionRiskPercent = 2.0; // 最大单笔风险百分比
        private int cooldownHours = 1; // 同币种信号冷却时间（小时）
        
        // Getters and Setters
        public double getMinRiskRewardRatio() {
            return minRiskRewardRatio;
        }
        
        public void setMinRiskRewardRatio(double minRiskRewardRatio) {
            this.minRiskRewardRatio = minRiskRewardRatio;
        }
        
        public double getMaxPositionRiskPercent() {
            return maxPositionRiskPercent;
        }
        
        public void setMaxPositionRiskPercent(double maxPositionRiskPercent) {
            this.maxPositionRiskPercent = maxPositionRiskPercent;
        }
        
        public int getCooldownHours() {
            return cooldownHours;
        }
        
        public void setCooldownHours(int cooldownHours) {
            this.cooldownHours = cooldownHours;
        }
    }
    
    private CopyTrading copyTrading = new CopyTrading();
    
    public CopyTrading getCopyTrading() {
        return copyTrading;
    }
    
    public void setCopyTrading(CopyTrading copyTrading) {
        this.copyTrading = copyTrading;
    }
    
    public static class CopyTrading {
        private boolean enabled = true; // 是否启用交易员跟单功能
        
        // Getters and Setters
        public boolean isEnabled() {
            return enabled;
        }
        
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
    
    public static class Trader {
        private String id;
        private String portfolioId;
        private String name;
        
        // Getters and Setters
        public String getId() {
            return id;
        }
        
        public void setId(String id) {
            this.id = id;
        }
        
        public String getPortfolioId() {
            return portfolioId;
        }
        
        public void setPortfolioId(String portfolioId) {
            this.portfolioId = portfolioId;
        }
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
    }
}
