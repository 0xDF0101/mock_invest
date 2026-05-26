package com.mockinvest.infrastructure.stockprice.yahoo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.mockinvest.infrastructure.stockprice.CandleDto;
import com.mockinvest.infrastructure.stockprice.StockPriceDto;
import com.mockinvest.infrastructure.stockprice.StockPriceProvider;
import com.mockinvest.infrastructure.stockprice.TickSizeUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Component
public class YahooFinanceProvider implements StockPriceProvider {

    private final RestClient yahooRestClient;
    private final ObjectMapper objectMapper;

    // 현재가: 30초 TTL (실시간성 유지하면서 중복 호출 차단)
    private final Cache<String, StockPriceDto> priceCache = Caffeine.newBuilder()
            .expireAfterWrite(30, TimeUnit.SECONDS)
            .maximumSize(500)
            .build();

    // 차트 데이터: 5분 TTL (자주 바뀌지 않음)
    private final Cache<String, List<CandleDto>> historyCache = Caffeine.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .maximumSize(300)
            .build();

    public YahooFinanceProvider(@Qualifier("yahooRestClient") RestClient yahooRestClient,
                                ObjectMapper objectMapper) {
        this.yahooRestClient = yahooRestClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public StockPriceDto getPrice(String ticker) {
        return priceCache.get(ticker, this::fetchPrice);
    }

    @Override
    public Map<String, StockPriceDto> getPrices(List<String> tickers) {
        // 병렬로 각 종목 가격 조회 (캐시 히트 시 즉시 반환)
        return tickers.parallelStream()
                .collect(Collectors.toMap(t -> t, this::getPrice));
    }

    @Override
    public List<CandleDto> getHistory(String ticker, String period) {
        return historyCache.get(ticker + ":" + period, k -> fetchHistory(ticker, period));
    }

    @Override
    public BigDecimal getDividendToday(String ticker) {
        try {
            String json = yahooRestClient.get()
                    .uri("/v8/finance/chart/{ticker}?interval=1d&range=7d&events=div", ticker)
                    .retrieve()
                    .body(String.class);

            JsonNode dividends = objectMapper.readTree(json)
                    .path("chart").path("result").get(0)
                    .path("events").path("dividends");

            if (dividends.isMissingNode() || dividends.isEmpty()) return BigDecimal.ZERO;

            LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
            for (JsonNode div : dividends) {
                LocalDate divDate = Instant.ofEpochSecond(div.path("date").longValue())
                        .atZone(ZoneId.of("Asia/Seoul")).toLocalDate();
                if (divDate.equals(today)) {
                    return div.path("amount").decimalValue();
                }
            }
        } catch (Exception e) {
            log.warn("배당금 조회 실패: {}", ticker, e);
        }
        return BigDecimal.ZERO;
    }

    private StockPriceDto fetchPrice(String ticker) {
        try {
            String json = yahooRestClient.get()
                    .uri("/v8/finance/chart/{ticker}?interval=1m&range=1d", ticker)
                    .retrieve()
                    .body(String.class);

            JsonNode meta = objectMapper.readTree(json)
                    .path("chart").path("result").get(0).path("meta");

            BigDecimal price = TickSizeUtil.round(meta.path("regularMarketPrice").decimalValue());
            BigDecimal prevClose = TickSizeUtil.round(meta.path("chartPreviousClose").decimalValue());
            BigDecimal change = price.subtract(prevClose);
            BigDecimal changePercent = prevClose.compareTo(BigDecimal.ZERO) == 0
                    ? BigDecimal.ZERO
                    : change.divide(prevClose, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));

            return new StockPriceDto(ticker, price, change, changePercent, prevClose);
        } catch (Exception e) {
            log.warn("주가 조회 실패: {}", ticker, e);
            return new StockPriceDto(ticker, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }
    }

    private List<CandleDto> fetchHistory(String ticker, String period) {
        String interval = "1d".equals(period) ? "5m" : "1d";
        try {
            String json = yahooRestClient.get()
                    .uri("/v8/finance/chart/{ticker}?interval={interval}&range={period}",
                            ticker, interval, period)
                    .retrieve()
                    .body(String.class);

            JsonNode result = objectMapper.readTree(json)
                    .path("chart").path("result").get(0);
            JsonNode timestamps = result.path("timestamp");
            JsonNode quote = result.path("indicators").path("quote").get(0);

            List<CandleDto> candles = new ArrayList<>();
            for (int i = 0; i < timestamps.size(); i++) {
                JsonNode o = quote.path("open").get(i);
                JsonNode h = quote.path("high").get(i);
                JsonNode l = quote.path("low").get(i);
                JsonNode c = quote.path("close").get(i);
                JsonNode v = quote.path("volume").get(i);
                if (o.isNull() || c.isNull()) continue;
                candles.add(new CandleDto(
                        timestamps.get(i).longValue(),
                        o.decimalValue(), h.decimalValue(),
                        l.decimalValue(), c.decimalValue(),
                        v.longValue()));
            }
            return candles;
        } catch (Exception e) {
            log.warn("차트 데이터 조회 실패: {}", ticker, e);
            return List.of();
        }
    }
}
