package com.example.tradewise.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class OrderHistoryResponse {
    
    private String code;
    private Object message;
    @JsonProperty("messageDetail")
    private Object messageDetail;
    private DataDTO data;
    private boolean success;

    // Getters and Setters
    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Object getMessage() {
        return message;
    }

    public void setMessage(Object message) {
        this.message = message;
    }

    public Object getMessageDetail() {
        return messageDetail;
    }

    public void setMessageDetail(Object messageDetail) {
        this.messageDetail = messageDetail;
    }

    public DataDTO getData() {
        return data;
    }

    public void setData(DataDTO data) {
        this.data = data;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public static class DataDTO {
        private String indexValue;
        private Integer total;
        private List<OrderItemDTO> list;

        // Getters and Setters
        public String getIndexValue() {
            return indexValue;
        }

        public void setIndexValue(String indexValue) {
            this.indexValue = indexValue;
        }

        public Integer getTotal() {
            return total;
        }

        public void setTotal(Integer total) {
            this.total = total;
        }

        public List<OrderItemDTO> getList() {
            return list;
        }

        public void setList(List<OrderItemDTO> list) {
            this.list = list;
        }
    }

    public static class OrderItemDTO {
        private String symbol;
        private String baseAsset;
        private String quoteAsset;
        private String side;
        private String type;
        private String positionSide;
        private Double executedQty;
        private Double avgPrice;
        private Double totalPnl;
        private Long orderUpdateTime;
        private Long orderTime;

        // Getters and Setters
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

        public Double getExecutedQty() {
            return executedQty;
        }

        public void setExecutedQty(Double executedQty) {
            this.executedQty = executedQty;
        }

        public Double getAvgPrice() {
            return avgPrice;
        }

        public void setAvgPrice(Double avgPrice) {
            this.avgPrice = avgPrice;
        }

        public Double getTotalPnl() {
            return totalPnl;
        }

        public void setTotalPnl(Double totalPnl) {
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
}