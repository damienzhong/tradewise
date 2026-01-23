package com.example.tradewise.entity;

import java.math.BigDecimal;

public class Order {
    private Long id;
    private String externalOrderId;
    private String traderId;
    private String traderName;
    private String symbol;
    private String baseAsset;
    private String quoteAsset;
    private String side;
    private String type;
    private String positionSide;
    private BigDecimal executedQty;
    private BigDecimal avgPrice;
    private BigDecimal totalPnl;
    private Long orderUpdateTime;
    private Long orderTime;

    // Constructors
    public Order() {}

    public Order(String externalOrderId, String traderId, String traderName, String symbol, String baseAsset, String quoteAsset,
                 String side, String type, String positionSide, BigDecimal executedQty, BigDecimal avgPrice,
                 BigDecimal totalPnl, Long orderUpdateTime, Long orderTime) {
        this.externalOrderId = externalOrderId;
        this.traderId = traderId;
        this.traderName = traderName;
        this.symbol = symbol;
        this.baseAsset = baseAsset;
        this.quoteAsset = quoteAsset;
        this.side = side;
        this.type = type;
        this.positionSide = positionSide;
        this.executedQty = executedQty;
        this.avgPrice = avgPrice;
        this.totalPnl = totalPnl;
        this.orderUpdateTime = orderUpdateTime;
        this.orderTime = orderTime;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getExternalOrderId() {
        return externalOrderId;
    }

    public void setExternalOrderId(String externalOrderId) {
        this.externalOrderId = externalOrderId;
    }

    public String getTraderId() {
        return traderId;
    }

    public void setTraderId(String traderId) {
        this.traderId = traderId;
    }

    public String getTraderName() {
        return traderName;
    }

    public void setTraderName(String traderName) {
        this.traderName = traderName;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getBaseAsset() {
        return baseAsset;
    }

    public void setBaseAsset(String baseAsset) {
        this.baseAsset = baseAsset;
    }

    public String getQuoteAsset() {
        return quoteAsset;
    }

    public void setQuoteAsset(String quoteAsset) {
        this.quoteAsset = quoteAsset;
    }

    public String getSide() {
        return side;
    }

    public void setSide(String side) {
        this.side = side;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getPositionSide() {
        return positionSide;
    }

    public void setPositionSide(String positionSide) {
        this.positionSide = positionSide;
    }

    public BigDecimal getExecutedQty() {
        return executedQty;
    }

    public void setExecutedQty(BigDecimal executedQty) {
        this.executedQty = executedQty;
    }

    public BigDecimal getAvgPrice() {
        return avgPrice;
    }

    public void setAvgPrice(BigDecimal avgPrice) {
        this.avgPrice = avgPrice;
    }

    public BigDecimal getTotalPnl() {
        return totalPnl;
    }

    public void setTotalPnl(BigDecimal totalPnl) {
        this.totalPnl = totalPnl;
    }

    public Long getOrderUpdateTime() {
        return orderUpdateTime;
    }

    public void setOrderUpdateTime(Long orderUpdateTime) {
        this.orderUpdateTime = orderUpdateTime;
    }

    public Long getOrderTime() {
        return orderTime;
    }

    public void setOrderTime(Long orderTime) {
        this.orderTime = orderTime;
    }
}