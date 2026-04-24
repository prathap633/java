package com.example.LiveData.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "candle_history", indexes = {
    @Index(name = "idx_candle_instrument", columnList = "instrumentKey"),
    @Index(name = "idx_candle_ts", columnList = "timestamp")
})
public class CandleHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String instrumentKey;

    private double open;
    private double high;
    private double low;
    private double close;
    private long volume;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    public CandleHistory() {}

    public CandleHistory(String instrumentKey, double open, double high, double low, double close, long volume, LocalDateTime timestamp) {
        this.instrumentKey = instrumentKey;
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
        this.volume = volume;
        this.timestamp = timestamp;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getInstrumentKey() { return instrumentKey; }
    public void setInstrumentKey(String instrumentKey) { this.instrumentKey = instrumentKey; }

    public double getOpen() { return open; }
    public void setOpen(double open) { this.open = open; }

    public double getHigh() { return high; }
    public void setHigh(double high) { this.high = high; }

    public double getLow() { return low; }
    public void setLow(double low) { this.low = low; }

    public double getClose() { return close; }
    public void setClose(double close) { this.close = close; }

    public long getVolume() { return volume; }
    public void setVolume(long volume) { this.volume = volume; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
