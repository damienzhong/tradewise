package com.example.tradewise.service;

import com.example.tradewise.service.MarketAnalysisService.Candlestick;
import com.example.tradewise.service.MarketAnalysisService.TechnicalIndicators;
import com.example.tradewise.service.SignalFusionEngine.TradingSignal;
import com.example.tradewise.service.SignalFusionEngine.SignalModel;
import com.example.tradewise.service.SignalFusionEngine.Decision;

import java.util.*;

/**
 * 相关性套利模型（对冲版）
 * 实现DeepSeek方案中的模型6：相关性套利模型
 */
public class CorrelationArbitrageModel {
    
    /**
     * 检测相关性套利信号
     */
    public Optional<TradingSignal> detect(String symbol, Map<String, List<Candlestick>> multiTimeframeData, 
                                       Map<String, List<Candlestick>> allSymbolsData) {
        // 获取主要交易对的数据
        List<Candlestick> symbolData = multiTimeframeData.get("4h");
        if (symbolData == null || symbolData.size() < 20) {
            return Optional.empty();
        }
        
        // 寻找相关的交易对进行套利分析
        // 这里我们使用模拟的相关交易对数据
        Map<String, List<Candlestick>> relatedSymbols = getRelatedSymbolsData(symbol, allSymbolsData);
        
        if (relatedSymbols.isEmpty()) {
            return Optional.empty();
        }
        
        // 策略类型：比价交易、板块轮动、期现套利
        List<ArbitrageOpportunity> opportunities = new ArrayList<>();
        
        // 类型A：比价交易
        opportunities.addAll(findPricingArbitrage(symbol, symbolData, relatedSymbols));
        
        // 类型B：板块轮动
        opportunities.addAll(findSectorRotationArbitrage(symbol, symbolData, allSymbolsData));
        
        // 类型C：期现套利（简化实现）
        opportunities.addAll(findSpotFuturesArbitrage(symbol, symbolData, relatedSymbols));
        
        if (opportunities.isEmpty()) {
            return Optional.empty();
        }
        
        // 选择最佳机会
        ArbitrageOpportunity bestOpportunity = opportunities.stream()
                .max(Comparator.comparingDouble(ArbitrageOpportunity::getScore))
                .orElse(null);
        
        if (bestOpportunity == null) {
            return Optional.empty();
        }
        
        // 确定方向和强度
        Decision direction = bestOpportunity.getLongSymbol().equals(symbol) ? Decision.LONG : Decision.SHORT;
        double strength = Math.min(8.0, bestOpportunity.getScore()); // 限制强度
        String reason = bestOpportunity.getDescription();
        
        // 计算置信度
        double confidence = calculateConfidence(bestOpportunity, symbolData);
        
        // 生成元数据
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("arbitrage_type", bestOpportunity.getType());
        metadata.put("opportunity_score", bestOpportunity.getScore());
        metadata.put("long_symbol", bestOpportunity.getLongSymbol());
        metadata.put("short_symbol", bestOpportunity.getShortSymbol());
        metadata.put("spread_deviation", bestOpportunity.getSpreadDeviation());
        metadata.put("analysis_details", bestOpportunity.getDetails());
        
        TradingSignal signal = new TradingSignal(
            SignalModel.CORRELATION_ARBITRAGE,
            direction,
            strength,
            reason,
            confidence,
            metadata
        );
        
        return Optional.of(signal);
    }
    
    /**
     * 检测比价交易机会
     */
    private List<ArbitrageOpportunity> findPricingArbitrage(String baseSymbol, List<Candlestick> baseData, 
                                                          Map<String, List<Candlestick>> relatedSymbols) {
        List<ArbitrageOpportunity> opportunities = new ArrayList<>();
        
        for (Map.Entry<String, List<Candlestick>> entry : relatedSymbols.entrySet()) {
            String relatedSymbol = entry.getKey();
            List<Candlestick> relatedData = entry.getValue();
            
            if (relatedData == null || relatedData.size() < 20) {
                continue;
            }
            
            // 计算比价（例如 ETH/BTC 汇率）
            double[] ratios = calculateRatioSeries(baseData, relatedData);
            if (ratios.length == 0) {
                continue;
            }
            
            // 计算历史均值和标准差
            double[] stats = calculateMeanAndStd(ratios);
            double meanRatio = stats[0];
            double stdRatio = stats[1];
            
            // 获取当前比价
            double currentRatio = ratios[ratios.length - 1];
            
            // 检查是否偏离历史均值2个标准差
            if (Math.abs(currentRatio - meanRatio) > 2.0 * stdRatio) {
                String description;
                String longSymbol, shortSymbol;
                
                if (currentRatio > meanRatio + 2.0 * stdRatio) {
                    // 比价过高，做空强势品种，做多弱势品种
                    description = String.format("比价交易机会：当前 %s/%s 比价 (%.4f) 远高于历史均值 (%.4f)，偏离%.2f个标准差，建议做空%s，做多%s",
                                             baseSymbol, relatedSymbol, currentRatio, meanRatio, 
                                             (currentRatio - meanRatio) / stdRatio, baseSymbol, relatedSymbol);
                    longSymbol = relatedSymbol;
                    shortSymbol = baseSymbol;
                } else {
                    // 比价过低，做多强势品种，做空弱势品种
                    description = String.format("比价交易机会：当前 %s/%s 比价 (%.4f) 远低于历史均值 (%.4f)，偏离%.2f个标准差，建议做多%s，做空%s",
                                             baseSymbol, relatedSymbol, currentRatio, meanRatio, 
                                             (meanRatio - currentRatio) / stdRatio, baseSymbol, relatedSymbol);
                    longSymbol = baseSymbol;
                    shortSymbol = relatedSymbol;
                }
                
                // 计算机会评分（偏离程度越大，评分越高）
                double score = Math.abs((currentRatio - meanRatio) / stdRatio);
                double spreadDeviation = Math.abs(currentRatio - meanRatio);
                
                opportunities.add(new ArbitrageOpportunity(
                    ArbitrageType.PRICING, score, description, longSymbol, shortSymbol, spreadDeviation,
                    String.format("比价偏离: %.4f vs %.4f (均值), 标准差: %.4f", currentRatio, meanRatio, stdRatio)
                ));
            }
        }
        
        return opportunities;
    }
    
    /**
     * 检测板块轮动机会
     */
    private List<ArbitrageOpportunity> findSectorRotationArbitrage(String baseSymbol, List<Candlestick> baseData,
                                                                Map<String, List<Candlestick>> allSymbolsData) {
        List<ArbitrageOpportunity> opportunities = new ArrayList<>();
        
        // 这里我们假设已知一些板块信息，实际应用中需要更复杂的板块分类
        Map<String, List<String>> sectors = groupSymbolsIntoSectors(allSymbolsData.keySet());
        
        for (Map.Entry<String, List<String>> sectorEntry : sectors.entrySet()) {
            List<String> symbolsInSector = sectorEntry.getValue();
            if (symbolsInSector.size() < 2) {
                continue; // 需要至少2个品种才能进行对比
            }
            
            // 计算板块内各品种的相对强度
            Map<String, Double> relativeStrengths = calculateRelativeStrengths(symbolsInSector, allSymbolsData);
            
            if (!relativeStrengths.containsKey(baseSymbol)) {
                continue;
            }
            
            double baseStrength = relativeStrengths.get(baseSymbol);
            
            // 找到板块中最强势和最弱势的品种
            String strongest = null;
            String weakest = null;
            double maxStrength = Double.MIN_VALUE;
            double minStrength = Double.MAX_VALUE;
            
            for (Map.Entry<String, Double> strengthEntry : relativeStrengths.entrySet()) {
                if (!symbolsInSector.contains(strengthEntry.getKey())) {
                    continue;
                }
                
                double strength = strengthEntry.getValue();
                if (strength > maxStrength) {
                    maxStrength = strength;
                    strongest = strengthEntry.getKey();
                }
                if (strength < minStrength) {
                    minStrength = strength;
                    weakest = strengthEntry.getKey();
                }
            }
            
            if (strongest == null || weakest == null) {
                continue;
            }
            
            // 如果本品种是最强势的，而有明显较弱的品种，则考虑做多弱品种做空强品种
            // 如果本品种是最弱势的，而有明显较强的品种，则考虑做多强品种做空弱品种
            if (baseSymbol.equals(strongest) && !baseSymbol.equals(weakest) && maxStrength - minStrength > 0.05) {
                // 本品种最强，做多弱品种，做空本品种
                String description = String.format("板块轮动机会：在%s板块中，%s相对强度最高，%s相对强度最低，强度差%.4f，建议做多%s，做空%s",
                                                 sectorEntry.getKey(), strongest, weakest, maxStrength - minStrength,
                                                 weakest, baseSymbol);
                
                double score = (maxStrength - minStrength) * 10; // 标准化评分
                double spreadDeviation = maxStrength - minStrength;
                
                opportunities.add(new ArbitrageOpportunity(
                    ArbitrageType.SECTOR_ROTATION, score, description, weakest, baseSymbol, spreadDeviation,
                    String.format("板块轮动：强品%s(%.4f) vs 弱品%s(%.4f)", strongest, maxStrength, weakest, minStrength)
                ));
            } else if (baseSymbol.equals(weakest) && !baseSymbol.equals(strongest) && maxStrength - minStrength > 0.05) {
                // 本品种最弱，做多本品种，做空强品种
                String description = String.format("板块轮动机会：在%s板块中，%s相对强度最低，%s相对强度最高，强度差%.4f，建议做多%s，做空%s",
                                                 sectorEntry.getKey(), weakest, strongest, maxStrength - minStrength,
                                                 baseSymbol, strongest);
                
                double score = (maxStrength - minStrength) * 10; // 标准化评分
                double spreadDeviation = maxStrength - minStrength;
                
                opportunities.add(new ArbitrageOpportunity(
                    ArbitrageType.SECTOR_ROTATION, score, description, baseSymbol, strongest, spreadDeviation,
                    String.format("板块轮动：弱品%s(%.4f) vs 强品%s(%.4f)", weakest, minStrength, strongest, maxStrength)
                ));
            }
        }
        
        return opportunities;
    }
    
    /**
     * 检测期现套利机会（简化实现）
     */
    private List<ArbitrageOpportunity> findSpotFuturesArbitrage(String baseSymbol, List<Candlestick> spotData,
                                                              Map<String, List<Candlestick>> relatedSymbols) {
        List<ArbitrageOpportunity> opportunities = new ArrayList<>();
        
        // 检查是否有对应的期货/永续合约数据
        String futuresSymbol = baseSymbol.replace("USDT", "USD"); // 简化假设
        if (relatedSymbols.containsKey(futuresSymbol)) {
            List<Candlestick> futuresData = relatedSymbols.get(futuresSymbol);
            if (futuresData != null && futuresData.size() >= spotData.size()) {
                // 计算基差（现货价格 - 期货价格）
                List<Candlestick> matchedSpotData = spotData;
                List<Candlestick> matchedFuturesData = futuresData.subList(0, spotData.size());
                
                double[] basisSeries = new double[matchedSpotData.size()];
                for (int i = 0; i < matchedSpotData.size(); i++) {
                    basisSeries[i] = matchedSpotData.get(i).getClose() - matchedFuturesData.get(i).getClose();
                }
                
                // 计算历史基差均值和标准差
                double[] basisStats = calculateMeanAndStd(basisSeries);
                double meanBasis = basisStats[0];
                double stdBasis = basisStats[1];
                
                // 获取当前基差
                double currentBasis = basisSeries[basisSeries.length - 1];
                
                // 检查基差是否偏离正常范围
                if (Math.abs(currentBasis - meanBasis) > 2.0 * stdBasis) {
                    String description;
                    String longSymbol, shortSymbol;
                    
                    if (currentBasis > meanBasis + 2.0 * stdBasis) {
                        // 现货溢价过高，做空现货，做多期货
                        description = String.format("期现套利机会：当前%s基差(%.4f)远高于历史均值(%.4f)，偏离%.2f个标准差，建议做空现货，做多期货",
                                                 baseSymbol, currentBasis, meanBasis,
                                                 (currentBasis - meanBasis) / stdBasis);
                        longSymbol = futuresSymbol;
                        shortSymbol = baseSymbol;
                    } else {
                        // 期货溢价过高，做多现货，做空期货
                        description = String.format("期现套利机会：当前%s基差(%.4f)远低于历史均值(%.4f)，偏离%.2f个标准差，建议做多现货，做空期货",
                                                 baseSymbol, currentBasis, meanBasis,
                                                 (meanBasis - currentBasis) / stdBasis);
                        longSymbol = baseSymbol;
                        shortSymbol = futuresSymbol;
                    }
                    
                    double score = Math.abs((currentBasis - meanBasis) / stdBasis);
                    double spreadDeviation = Math.abs(currentBasis - meanBasis);
                    
                    opportunities.add(new ArbitrageOpportunity(
                        ArbitrageType.SPOT_FUTURES, score, description, longSymbol, shortSymbol, spreadDeviation,
                        String.format("期现基差：当前%.4f vs 均值%.4f，标准差%.4f", currentBasis, meanBasis, stdBasis)
                    ));
                }
            }
        }
        
        return opportunities;
    }
    
    /**
     * 计算两个序列的比价
     */
    private double[] calculateRatioSeries(List<Candlestick> numeratorData, List<Candlestick> denominatorData) {
        int minLength = Math.min(numeratorData.size(), denominatorData.size());
        double[] ratios = new double[minLength];
        
        for (int i = 0; i < minLength; i++) {
            double numPrice = numeratorData.get(numeratorData.size() - minLength + i).getClose();
            double denPrice = denominatorData.get(denominatorData.size() - minLength + i).getClose();
            
            if (denPrice != 0) {
                ratios[i] = numPrice / denPrice;
            } else {
                ratios[i] = 0; // 避免除零
            }
        }
        
        return ratios;
    }
    
    /**
     * 计算均值和标准差
     */
    private double[] calculateMeanAndStd(double[] values) {
        if (values.length == 0) {
            return new double[]{0, 0};
        }
        
        // 计算均值
        double sum = 0;
        for (double value : values) {
            sum += value;
        }
        double mean = sum / values.length;
        
        // 计算方差和标准差
        double varianceSum = 0;
        for (double value : values) {
            varianceSum += Math.pow(value - mean, 2);
        }
        double variance = varianceSum / values.length;
        double std = Math.sqrt(variance);
        
        return new double[]{mean, std};
    }
    
    /**
     * 获取相关交易对数据
     */
    private Map<String, List<Candlestick>> getRelatedSymbolsData(String baseSymbol, 
                                                               Map<String, List<Candlestick>> allSymbolsData) {
        Map<String, List<Candlestick>> related = new HashMap<>();
        
        // 根据基础交易对推断可能的相关交易对
        // 例如，如果基础是 BTCUSDT，则相关可能是 ETHUSDT, XRPUSDT 等
        String quoteCurrency = extractQuoteCurrency(baseSymbol);
        
        for (Map.Entry<String, List<Candlestick>> entry : allSymbolsData.entrySet()) {
            String symbol = entry.getKey();
            if (symbol.equals(baseSymbol)) {
                continue; // 跳过自己
            }
            
            // 检查是否使用相同的报价货币
            if (extractQuoteCurrency(symbol).equals(quoteCurrency) && entry.getValue() != null && !entry.getValue().isEmpty()) {
                related.put(symbol, entry.getValue());
            }
        }
        
        return related;
    }
    
    /**
     * 提取报价货币
     */
    private String extractQuoteCurrency(String symbol) {
        // 假设符号格式为 BASEQUOTE (如 BTCUSDT)
        if (symbol.toUpperCase().endsWith("USDT")) {
            return "USDT";
        } else if (symbol.toUpperCase().endsWith("USD")) {
            return "USD";
        } else if (symbol.toUpperCase().endsWith("EUR")) {
            return "EUR";
        } else if (symbol.toUpperCase().endsWith("GBP")) {
            return "GBP";
        } else if (symbol.toUpperCase().endsWith("BTC")) {
            return "BTC";
        } else if (symbol.toUpperCase().endsWith("ETH")) {
            return "ETH";
        } else {
            // 简化处理，取最后3-4个字符作为报价货币
            int len = symbol.length();
            if (len >= 4) {
                return symbol.substring(len - 4);
            } else if (len >= 3) {
                return symbol.substring(len - 3);
            } else {
                return symbol;
            }
        }
    }
    
    /**
     * 将交易对分组到板块
     */
    private Map<String, List<String>> groupSymbolsIntoSectors(Set<String> allSymbols) {
        Map<String, List<String>> sectors = new HashMap<>();
        
        // 简化的板块分类
        List<String> majorCoins = Arrays.asList("BTC", "ETH", "BNB", "XRP");
        List<String> defiCoins = Arrays.asList("UNI", "AAVE", "COMP", "MKR", "SNX");
        List<String> layer1Coins = Arrays.asList("ADA", "DOT", "SOL", "AVAX", "MATIC");
        
        List<String> majorSymbols = new ArrayList<>();
        List<String> defiSymbols = new ArrayList<>();
        List<String> layer1Symbols = new ArrayList<>();
        List<String> otherSymbols = new ArrayList<>();
        
        for (String symbol : allSymbols) {
            String baseCurrency = extractBaseCurrency(symbol);
            
            if (majorCoins.contains(baseCurrency.toUpperCase())) {
                majorSymbols.add(symbol);
            } else if (defiCoins.contains(baseCurrency.toUpperCase())) {
                defiSymbols.add(symbol);
            } else if (layer1Coins.contains(baseCurrency.toUpperCase())) {
                layer1Symbols.add(symbol);
            } else {
                otherSymbols.add(symbol);
            }
        }
        
        if (!majorSymbols.isEmpty()) sectors.put("Major Coins", majorSymbols);
        if (!defiSymbols.isEmpty()) sectors.put("DeFi Tokens", defiSymbols);
        if (!layer1Symbols.isEmpty()) sectors.put("Layer 1", layer1Symbols);
        if (!otherSymbols.isEmpty()) sectors.put("Others", otherSymbols);
        
        return sectors;
    }
    
    /**
     * 提取基础货币
     */
    private String extractBaseCurrency(String symbol) {
        String quoteCurrency = extractQuoteCurrency(symbol);
        return symbol.substring(0, symbol.length() - quoteCurrency.length());
    }
    
    /**
     * 计算相对强度
     */
    private Map<String, Double> calculateRelativeStrengths(List<String> symbols, 
                                                        Map<String, List<Candlestick>> allSymbolsData) {
        Map<String, Double> strengths = new HashMap<>();
        
        for (String symbol : symbols) {
            List<Candlestick> data = allSymbolsData.get(symbol);
            if (data != null && data.size() >= 20) {
                // 使用价格变化率作为相对强度的指标
                double firstPrice = data.get(0).getClose();
                double lastPrice = data.get(data.size() - 1).getClose();
                double strength = (lastPrice - firstPrice) / firstPrice;
                
                // 也可以考虑加入波动率调整
                List<Double> atrList = TechnicalIndicators.calculateATR(data, 14);
                double avgAtr = atrList.stream()
                        .filter(Objects::nonNull)
                        .mapToDouble(Double::doubleValue)
                        .average()
                        .orElse(0);
                
                if (avgAtr > 0) {
                    // 调整强度：高收益低波动的品种强度更高
                    double volatilityAdjustedStrength = strength / (avgAtr / firstPrice);
                    strengths.put(symbol, volatilityAdjustedStrength);
                } else {
                    strengths.put(symbol, strength);
                }
            }
        }
        
        return strengths;
    }
    
    /**
     * 计算置信度
     */
    private double calculateConfidence(ArbitrageOpportunity opportunity, List<Candlestick> symbolData) {
        double baseConfidence = 0.7; // 基础置信度
        
        // 根据机会评分调整
        double scoreBasedAdjustment = Math.min(0.3, opportunity.getScore() / 50.0); // 评分越高，置信度越高，但有上限
        baseConfidence += scoreBasedAdjustment;
        
        // 根据数据量调整
        if (symbolData.size() < 30) {
            baseConfidence *= 0.7; // 数据不足降低置信度
        } else if (symbolData.size() >= 100) {
            baseConfidence *= 1.1; // 数据充分提高置信度
        }
        
        // 根据市场波动性调整
        List<Double> atrList = TechnicalIndicators.calculateATR(symbolData, 14);
        if (!atrList.isEmpty() && atrList.get(atrList.size() - 1) != null) {
            double currentAtr = atrList.get(atrList.size() - 1);
            double currentPrice = symbolData.get(symbolData.size() - 1).getClose();
            double atrPercentage = (currentAtr / currentPrice) * 100;
            
            // 极高或极低波动都可能影响套利机会的有效性
            if (atrPercentage > 5.0) {
                baseConfidence *= 0.7; // 过高波动降低置信度
            } else if (atrPercentage < 0.5) {
                baseConfidence *= 0.9; // 过低波动稍微降低置信度
            }
        }
        
        // 根据相关性稳定性调整（简化实现）
        // 在实际应用中，应该检查两个资产之间的相关性是否稳定
        baseConfidence *= 0.95; // 由于相关性可能会变化，稍微降低置信度
        
        // 确保置信度在合理范围内
        return Math.max(0.1, Math.min(1.0, baseConfidence));
    }
    
    /**
     * 套利机会类型
     */
    enum ArbitrageType {
        PRICING,        // 比价交易
        SECTOR_ROTATION, // 板块轮动
        SPOT_FUTURES   // 期现套利
    }
    
    /**
     * 套利机会类
     */
    static class ArbitrageOpportunity {
        private final ArbitrageType type;
        private final double score;
        private final String description;
        private final String longSymbol;
        private final String shortSymbol;
        private final double spreadDeviation;
        private final String details;
        
        public ArbitrageOpportunity(ArbitrageType type, double score, String description, 
                                  String longSymbol, String shortSymbol, double spreadDeviation, String details) {
            this.type = type;
            this.score = score;
            this.description = description;
            this.longSymbol = longSymbol;
            this.shortSymbol = shortSymbol;
            this.spreadDeviation = spreadDeviation;
            this.details = details;
        }
        
        public ArbitrageType getType() { return type; }
        public double getScore() { return score; }
        public String getDescription() { return description; }
        public String getLongSymbol() { return longSymbol; }
        public String getShortSymbol() { return shortSymbol; }
        public double getSpreadDeviation() { return spreadDeviation; }
        public String getDetails() { return details; }
    }
}