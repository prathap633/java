package com.example.LiveData.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@Entity
@Table(name = "stocks")
@JsonPropertyOrder({
    "symbol", "instrumentKey",
    "currentPrice", "change", "changePercent",
    "adx", "atr", "roc",
    "highPrice", "openPrice", "closePrice",
    "lastPrice", "previousClosePrice",
    "volume", "timestamp", "direction"
})
public class StockData {

    @Id
    private String instrumentKey;
    private String symbol;

    // 1) Current Price
    private double currentPrice;

    // 2) Change Price
    private double change;

    // 3) Change Percentage
    private double changePercent;

    // 4) Technical Indicators
    private double adx;
    private double atr;
    private double roc;

    // 5) High Price
    private double highPrice;

    // 6) Open Price
    private double openPrice;

    // 7) Close Price (today's close / last traded)
    private double closePrice;

    // 8) Last Price (last traded price)
    private double lastPrice;

    // 9) Previous Close Price
    private double previousClosePrice;

    // 10) Volume
    private long volume;

    // 11) Timestamp
    private String timestamp;

    // 12) Direction (UP / DOWN / NEUTRAL)
    private String direction;

    // Additional fields
    private double lowPrice;
    private double averagePrice;
    private double upperCircuitLimit;
    private double lowerCircuitLimit;
    private long totalBuyQuantity;
    private long totalSellQuantity;

    // Legacy field kept for backward compatibility
    private String status;

    public StockData() {
    }

    // -------- Getters & Setters --------

    public String getInstrumentKey() { return instrumentKey; }
    public void setInstrumentKey(String instrumentKey) { this.instrumentKey = instrumentKey; }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public double getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(double currentPrice) { this.currentPrice = currentPrice; }

    public double getChange() { return change; }
    public void setChange(double change) { this.change = change; }

    public double getChangePercent() { return changePercent; }
    public void setChangePercent(double changePercent) { this.changePercent = changePercent; }

    public double getAdx() { return adx; }
    public void setAdx(double adx) { this.adx = adx; }

    public double getAtr() { return atr; }
    public void setAtr(double atr) { this.atr = atr; }

    public double getRoc() { return roc; }
    public void setRoc(double roc) { this.roc = roc; }

    public double getHighPrice() { return highPrice; }
    public void setHighPrice(double highPrice) { this.highPrice = highPrice; }

    public double getOpenPrice() { return openPrice; }
    public void setOpenPrice(double openPrice) { this.openPrice = openPrice; }

    public double getClosePrice() { return closePrice; }
    public void setClosePrice(double closePrice) { this.closePrice = closePrice; }

    public double getLastPrice() { return lastPrice; }
    public void setLastPrice(double lastPrice) { this.lastPrice = lastPrice; }

    public double getPreviousClosePrice() { return previousClosePrice; }
    public void setPreviousClosePrice(double previousClosePrice) { this.previousClosePrice = previousClosePrice; }

    public long getVolume() { return volume; }
    public void setVolume(long volume) { this.volume = volume; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public String getDirection() { return direction; }
    public void setDirection(String direction) { this.direction = direction; }

    public double getLowPrice() { return lowPrice; }
    public void setLowPrice(double lowPrice) { this.lowPrice = lowPrice; }

    public double getAveragePrice() { return averagePrice; }
    public void setAveragePrice(double averagePrice) { this.averagePrice = averagePrice; }

    public double getUpperCircuitLimit() { return upperCircuitLimit; }
    public void setUpperCircuitLimit(double upperCircuitLimit) { this.upperCircuitLimit = upperCircuitLimit; }

    public double getLowerCircuitLimit() { return lowerCircuitLimit; }
    public void setLowerCircuitLimit(double lowerCircuitLimit) { this.lowerCircuitLimit = lowerCircuitLimit; }

    public long getTotalBuyQuantity() { return totalBuyQuantity; }
    public void setTotalBuyQuantity(long totalBuyQuantity) { this.totalBuyQuantity = totalBuyQuantity; }

    public long getTotalSellQuantity() { return totalSellQuantity; }
    public void setTotalSellQuantity(long totalSellQuantity) { this.totalSellQuantity = totalSellQuantity; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return "StockData [Symbol=" + symbol + ", Price=" + currentPrice +
                ", Change=" + change + ", Change%=" + changePercent +
                ", ADX=" + adx + ", ATR=" + atr + ", ROC=" + roc +
                "%, Volume=" + volume + ", Direction=" + direction +
                ", Time=" + timestamp + "]";
    }
}
