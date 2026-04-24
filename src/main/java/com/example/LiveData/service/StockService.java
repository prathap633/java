package com.example.LiveData.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.LiveData.config.UpstoxConfig;
import com.example.LiveData.entity.StockData;
import com.example.LiveData.repository.StockRepository;
import com.example.LiveData.websocket.StockWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class StockService {

    @Autowired
    private UpstoxConfig upstoxConfig;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private StockWebSocketHandler webSocketHandler;

    @Autowired
    private TechnicalIndicatorService indicatorService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final ConcurrentHashMap<String, StockData> stockCache = new ConcurrentHashMap<>();

    // Track previous prices to determine UP/DOWN direction
    private final ConcurrentHashMap<String, Double> previousPrices = new ConcurrentHashMap<>();

    private static final String UPSTOX_QUOTE_URL = "https://api.upstox.com/v2/market-quote/quotes?symbol=";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

    /**
     * Core loop: Fetches data, calculates indicators, saves to DB, and broadcasts via WS.
     */
    @Scheduled(fixedRateString = "${upstox.refresh.interval}")
    public void fetchUpdateAndBroadcast() {
        try {
            List<StockData> stocks = fetchAllChunks();

            if (!stocks.isEmpty()) {
                stockRepository.saveAll(stocks);
                stocks.forEach(s -> stockCache.put(s.getInstrumentKey(), s));

                String json = objectMapper.writeValueAsString(new ArrayList<>(stockCache.values()));
                webSocketHandler.broadcastStockData(json);

                System.out.println("[SYNC] " + stocks.size() + " stocks updated at "
                        + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
            }
        } catch (Exception e) {
            System.err.println("[ERROR] Service: " + e.getMessage());
        }
    }

    private List<StockData> fetchAllChunks() {
        List<StockData> all = new ArrayList<>();
        List<List<String>> chunks = upstoxConfig.getInstrumentChunks();

        for (List<String> chunk : chunks) {
            try {
                String keys = String.join(",", chunk);
                all.addAll(fetchChunk(keys));
            } catch (Exception e) {
                System.err.println("[WARN] Chunk Error: " + e.getMessage());
            }
        }
        return all;
    }

    private List<StockData> fetchChunk(String instrumentKeys) throws Exception {
        String url = UPSTOX_QUOTE_URL + instrumentKeys;
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + upstoxConfig.getAccessToken());
        headers.set("Accept", "application/json");

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
            return parseResponse(response.getBody());
        } catch (HttpClientErrorException.Unauthorized e) {
            System.err.println("[AUTH] Token Expired: Update upstox.access.token in properties.");
            return Collections.emptyList();
        }
    }

    private List<StockData> parseResponse(String body) throws Exception {
        List<StockData> stocks = new ArrayList<>();
        JsonNode data = objectMapper.readTree(body).path("data");
        String now = LocalDateTime.now().format(FORMATTER);

        Iterator<Map.Entry<String, JsonNode>> fields = data.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            JsonNode node = entry.getValue();

            // ---- Extract all fields from Upstox API ----
            double ltp   = node.path("last_price").asDouble();
            double open  = node.path("ohlc").path("open").asDouble();
            double high  = node.path("ohlc").path("high").asDouble();
            double low   = node.path("ohlc").path("low").asDouble();
            long   volume = node.path("volume").asLong();
            String symbol = node.path("symbol").asText();
            String apiTimestamp = node.path("timestamp").asText(now);

            // Use net_change from API to derive previous close
            double netChangeApi = node.path("net_change").asDouble();
            double previousClose = (netChangeApi != 0) ? (ltp - netChangeApi) : node.path("ohlc").path("close").asDouble();
            if (previousClose == 0) previousClose = ltp;

            // Close price from OHLC (this is previous day close in Upstox full quotes)
            double ohlcClose = node.path("ohlc").path("close").asDouble();

            // Additional fields from full market quote
            double avgPrice = node.path("average_price").asDouble();
            double upperCircuit = node.path("upper_circuit_limit").asDouble();
            double lowerCircuit = node.path("lower_circuit_limit").asDouble();
            long totalBuy = node.path("total_buy_quantity").asLong();
            long totalSell = node.path("total_sell_quantity").asLong();

            // ---- Calculate change & percentage ----
            double netChange = netChangeApi;
            double pctChange = (previousClose != 0) ? (netChange / previousClose) * 100 : 0;

            // ---- Direction (UP / DOWN) based on price movement ----
            String instrumentKey = entry.getKey();
            Double prevPrice = previousPrices.get(instrumentKey);
            String direction;
            if (prevPrice != null) {
                if (ltp > prevPrice) direction = "UP";
                else if (ltp < prevPrice) direction = "DOWN";
                else direction = netChange >= 0 ? "UP" : "DOWN";
            } else {
                direction = netChange >= 0 ? "UP" : "DOWN";
            }
            previousPrices.put(instrumentKey, ltp);

            // ---- Store candle for indicator calculations ----
            indicatorService.storeCandle(instrumentKey, open, high, low, ltp, volume);

            // ---- Calculate technical indicators ----
            double atr = indicatorService.calculateATR(instrumentKey);
            double adx = indicatorService.calculateADX(instrumentKey);
            double roc = indicatorService.calculateROC(instrumentKey);

            // Fallback for ATR/ROC if not enough history
            if (atr == 0) {
                atr = indicatorService.calculateSingleCandleATR(high, low, previousClose);
            }
            if (roc == 0) {
                roc = indicatorService.calculateIntradayROC(open, ltp);
            }

            // ---- Build StockData with all 12 categories ----
            StockData s = new StockData();
            s.setInstrumentKey(instrumentKey);
            s.setSymbol(symbol);

            // 1) Current Price
            s.setCurrentPrice(ltp);

            // 2) Change Price
            s.setChange(Math.round(netChange * 100.0) / 100.0);

            // 3) Change Percentage
            s.setChangePercent(Math.round(pctChange * 100.0) / 100.0);

            // 4) ADX, ATR, ROC
            s.setAdx(adx);
            s.setAtr(atr);
            s.setRoc(roc);

            // 5) High Price
            s.setHighPrice(high);

            // 6) Open Price
            s.setOpenPrice(open);

            // 7) Close Price
            s.setClosePrice(ohlcClose > 0 ? ohlcClose : previousClose);

            // 8) Last Price
            s.setLastPrice(ltp);

            // 9) Previous Close Price
            s.setPreviousClosePrice(previousClose);

            // 10) Volume
            s.setVolume(volume);

            // 11) Timestamp
            s.setTimestamp(apiTimestamp.isEmpty() ? now : apiTimestamp);

            // 12) Direction (UP / DOWN)
            s.setDirection(direction);

            // Additional fields
            s.setLowPrice(low);
            s.setAveragePrice(avgPrice);
            s.setUpperCircuitLimit(upperCircuit);
            s.setLowerCircuitLimit(lowerCircuit);
            s.setTotalBuyQuantity(totalBuy);
            s.setTotalSellQuantity(totalSell);
            s.setStatus(direction); // keep status in sync with direction

            stocks.add(s);
        }
        return stocks;
    }

    // --- Helper Methods for Controllers ---

    public List<StockData> getLatestStocks() {
        return new ArrayList<>(stockCache.values());
    }

    public Optional<StockData> getByInstrumentKey(String key) {
        return Optional.ofNullable(stockCache.get(key));
    }

    public List<StockData> getTopGainers(int limit) {
        return stockCache.values().stream()
                .sorted((a, b) -> Double.compare(b.getChangePercent(), a.getChangePercent()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    public List<StockData> getTopLosers(int limit) {
        return stockCache.values().stream()
                .sorted(Comparator.comparingDouble(StockData::getChangePercent))
                .limit(limit)
                .collect(Collectors.toList());
    }

    public List<StockData> search(String query) {
        if (query == null || query.isEmpty()) return getLatestStocks();
        String q = query.toLowerCase();
        return stockCache.values().stream()
                .filter(s -> s.getSymbol().toLowerCase().contains(q)
                        || s.getInstrumentKey().toLowerCase().contains(q))
                .collect(Collectors.toList());
    }
}
