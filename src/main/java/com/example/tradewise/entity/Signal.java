package com.example.tradewise.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Signal {
    private Long id;
    private String symbol;
    private LocalDateTime signalTime;
    private String signalType; // BUY/SELL
    private String indicator;
    private BigDecimal price;
    private BigDecimal stopLoss;
    private BigDecimal takeProfit;
    private Integer score;
    private String confidence;
    private String reason;
    private String status; // PENDING/ACTIVE/CLOSED/EXPIRED
    private LocalDateTime outcomeTime;
    private BigDecimal finalPrice;
    private BigDecimal pnlPercentage;
    private String notes;
    private LocalDateTime createdAt;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public LocalDateTime getSignalTime() { return signalTime; }
    public void setSignalTime(LocalDateTime signalTime) { this.signalTime = signalTime; }

    public String getSignalType() { return signalType; }
    public void setSignalType(String signalType) { this.signalType = signalType; }

    public String getIndicator() { return indicator; }
    public void setIndicator(String indicator) { this.indicator = indicator; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public BigDecimal getStopLoss() { return stopLoss; }
    public void setStopLoss(BigDecimal stopLoss) { this.stopLoss = stopLoss; }

    public BigDecimal getTakeProfit() { return takeProfit; }
    public void setTakeProfit(BigDecimal takeProfit) { this.takeProfit = takeProfit; }

    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }

    public String getConfidence() { return confidence; }
    public void setConfidence(String confidence) { this.confidence = confidence; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getOutcomeTime() { return outcomeTime; }
    public void setOutcomeTime(LocalDateTime outcomeTime) { this.outcomeTime = outcomeTime; }

    public BigDecimal getFinalPrice() { return finalPrice; }
    public void setFinalPrice(BigDecimal finalPrice) { this.finalPrice = finalPrice; }

    public BigDecimal getPnlPercentage() { return pnlPercentage; }
    public void setPnlPercentage(BigDecimal pnlPercentage) { this.pnlPercentage = pnlPercentage; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
