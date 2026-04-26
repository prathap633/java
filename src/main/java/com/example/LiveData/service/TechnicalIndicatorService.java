package com.example.LiveData.service;

import com.example.LiveData.entity.CandleHistory;
import com.example.LiveData.repository.CandleHistoryRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Calculates technical indicators: ADX, ATR, ROC
 * using proper Wilder's smoothing methods with historical candle data.
 */
@Service
public class TechnicalIndicatorService {

    private static final int ATR_PERIOD = 14;
    private static final int ADX_PERIOD = 14;
    private static final int ROC_PERIOD = 12;

    private final CandleHistoryRepository candleRepo;

    public TechnicalIndicatorService(CandleHistoryRepository candleRepo) {
        this.candleRepo = candleRepo;
    }

    public void storeCandle(String instrumentKey, double open, double high, double low, double close, long volume) {
        CandleHistory candle = new CandleHistory(instrumentKey, open, high, low, close, volume, LocalDateTime.now());
        candleRepo.save(candle);
    }

    /**
     * ATR (Average True Range) — Wilder's smoothing over 14 periods.
     * TR = max(High-Low, |High-PrevClose|, |Low-PrevClose|)
     */
    public double calculateATR(String instrumentKey) {
        List<CandleHistory> candles = getChronological(instrumentKey, ATR_PERIOD + 1);
        if (candles.size() < 2) return 0;

        List<Double> trList = new ArrayList<>();
        for (int i = 1; i < candles.size(); i++) {
            CandleHistory cur = candles.get(i);
            CandleHistory prev = candles.get(i - 1);
            double tr = Math.max(cur.getHigh() - cur.getLow(),
                        Math.max(Math.abs(cur.getHigh() - prev.getClose()),
                                 Math.abs(cur.getLow() - prev.getClose())));
            trList.add(tr);
        }

        return round(wilderSmooth(trList, ATR_PERIOD));
    }

    /**
     * ADX (Average Directional Index) — measures trend strength 0-100.
     * Uses +DM, -DM, smoothed DI+, DI-, then DX -> ADX.
     */
    public double calculateADX(String instrumentKey) {
        int needed = ADX_PERIOD * 2 + 1;
        List<CandleHistory> candles = getChronological(instrumentKey, needed);
        if (candles.size() < ADX_PERIOD + 2) return 0;

        List<Double> plusDM = new ArrayList<>();
        List<Double> minusDM = new ArrayList<>();
        List<Double> trList = new ArrayList<>();

        for (int i = 1; i < candles.size(); i++) {
            CandleHistory cur = candles.get(i);
            CandleHistory prev = candles.get(i - 1);

            double upMove = cur.getHigh() - prev.getHigh();
            double downMove = prev.getLow() - cur.getLow();

            plusDM.add((upMove > downMove && upMove > 0) ? upMove : 0);
            minusDM.add((downMove > upMove && downMove > 0) ? downMove : 0);

            double tr = Math.max(cur.getHigh() - cur.getLow(),
                        Math.max(Math.abs(cur.getHigh() - prev.getClose()),
                                 Math.abs(cur.getLow() - prev.getClose())));
            trList.add(tr);
        }

        if (trList.size() < ADX_PERIOD) return 0;

        double smoothedPlusDM = wilderSmooth(plusDM, ADX_PERIOD);
        double smoothedMinusDM = wilderSmooth(minusDM, ADX_PERIOD);
        double smoothedTR = wilderSmooth(trList, ADX_PERIOD);

        if (smoothedTR == 0) return 0;

        double plusDI = (smoothedPlusDM / smoothedTR) * 100;
        double minusDI = (smoothedMinusDM / smoothedTR) * 100;
        double diSum = plusDI + minusDI;

        if (diSum == 0) return 0;

        double dx = (Math.abs(plusDI - minusDI) / diSum) * 100;
        return round(dx);
    }

    /**
     * ROC (Rate of Change) — momentum as percentage change over N periods.
     * ROC = ((CurrentClose - CloseNAgo) / CloseNAgo) * 100
     */
    public double calculateROC(String instrumentKey) {
        List<CandleHistory> candles = getChronological(instrumentKey, ROC_PERIOD + 1);
        if (candles.size() <= ROC_PERIOD) return 0;

        double currentClose = candles.get(candles.size() - 1).getClose();
        double pastClose = candles.get(candles.size() - 1 - ROC_PERIOD).getClose();

        if (pastClose == 0) return 0;
        return round(((currentClose - pastClose) / pastClose) * 100);
    }

    /**
     * Fallback: calculate ATR from a single candle's True Range
     * when not enough history is available.
     */
    public double calculateSingleCandleATR(double high, double low, double prevClose) {
        double tr = Math.max(high - low,
                   Math.max(Math.abs(high - prevClose),
                            Math.abs(low - prevClose)));
        return round(tr);
    }

    /**
     * Fallback: calculate ROC from open to current price (intraday momentum).
     */
    public double calculateIntradayROC(double open, double current) {
        if (open == 0) return 0;
        return round(((current - open) / open) * 100);
    }

    private List<CandleHistory> getChronological(String instrumentKey, int count) {
        List<CandleHistory> candles = candleRepo.findLatestCandles(instrumentKey, count);
        List<CandleHistory> chronological = new ArrayList<>(candles);
        Collections.reverse(chronological);
        return chronological;
    }

    private double wilderSmooth(List<Double> values, int period) {
        if (values.size() < period) {
            return values.stream().mapToDouble(d -> d).average().orElse(0);
        }
        double sum = 0;
        for (int i = 0; i < period; i++) {
            sum += values.get(i);
        }
        double smoothed = sum / period;
        for (int i = period; i < values.size(); i++) {
            smoothed = (smoothed * (period - 1) + values.get(i)) / period;
        }
        return smoothed;
    }

    private double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
