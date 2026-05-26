package com.mockinvest.web.controller;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.mockinvest.domain.market.MarketHoursService;
import com.mockinvest.domain.portfolio.HoldingRepository;
import com.mockinvest.domain.stock.Stock;
import com.mockinvest.domain.stock.StockService;
import com.mockinvest.domain.user.UserService;
import com.mockinvest.infrastructure.stockprice.CandleDto;
import com.mockinvest.infrastructure.stockprice.StockPriceDto;
import com.mockinvest.infrastructure.stockprice.StockPriceProvider;
import com.mockinvest.infrastructure.stockprice.TickSizeUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

@Controller
@RequiredArgsConstructor
public class StockController {

    private final StockService stockService;
    private final StockPriceProvider stockPriceProvider;
    private final HoldingRepository holdingRepository;
    private final UserService userService;
    private final MarketHoursService marketHoursService;

    // 추천 종목 결과를 1분간 캐싱 (가격 + 캔들 포함)
    private final Cache<String, List<Map<String, Object>>> featuredCache = Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .maximumSize(1)
            .build();

    @GetMapping("/stocks/search")
    public String searchPage(@RequestParam(required = false) String q, Model model) {
        if (q != null && !q.isBlank()) {
            model.addAttribute("results", stockService.search(q));
            model.addAttribute("q", q);
        }
        return "stock/search";
    }

    @GetMapping("/stocks/{ticker}")
    public String stockDetail(@PathVariable String ticker,
                              @RequestParam(defaultValue = "1mo") String period,
                              Model model,
                              Principal principal) {
        Stock stock = stockService.getByTicker(ticker);
        StockPriceDto price = stockPriceProvider.getPrice(ticker);
        List<CandleDto> candles = stockPriceProvider.getHistory(ticker, period);

        var user = userService.getByUsername(principal.getName());
        int heldQuantity = holdingRepository
                .findByUserIdAndStockId(user.getId(), stock.getId())
                .map(h -> h.getQuantity())
                .orElse(0);

        model.addAttribute("stock", stock);
        model.addAttribute("price", price);
        model.addAttribute("candles", candles);
        model.addAttribute("period", period);
        model.addAttribute("heldQuantity", heldQuantity);
        model.addAttribute("marketOpen", marketHoursService.isOpen());
        model.addAttribute("marketStatus", marketHoursService.getStatusLabel());
        model.addAttribute("nextEventMs", marketHoursService.getNextEventMillis());
        model.addAttribute("nextEventOpen", marketHoursService.isNextEventOpen());
        model.addAttribute("tickSize", TickSizeUtil.tickSize(price.price()));
        return "stock/detail";
    }

    @GetMapping("/api/stocks/price/{ticker}")
    @ResponseBody
    public ResponseEntity<StockPriceDto> getPrice(@PathVariable String ticker) {
        return ResponseEntity.ok(stockPriceProvider.getPrice(ticker));
    }

    @GetMapping("/api/stocks/history/{ticker}")
    @ResponseBody
    public ResponseEntity<List<CandleDto>> getHistory(@PathVariable String ticker,
                                                       @RequestParam(defaultValue = "1mo") String period) {
        return ResponseEntity.ok(stockPriceProvider.getHistory(ticker, period));
    }

    /**
     * 추천 종목 API.
     * - 1분 캐시: 캐시 히트 시 즉시 반환, 만료 시 새 랜덤 종목으로 교체
     * - Java 21 가상 스레드: 블로킹 HTTP 호출을 진짜 병렬로 실행
     * - 응답에 캔들 데이터 포함: 브라우저 추가 요청 0건
     */
    @GetMapping("/api/stocks/featured")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getFeatured(
            @RequestParam(defaultValue = "12") int limit) {

        List<Map<String, Object>> cached = featuredCache.getIfPresent("featured");
        if (cached != null) return ResponseEntity.ok(cached);

        List<Stock> stocks = stockService.getRandom(limit);
        List<Map<String, Object>> result;

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<Map<String, Object>>> futures = stocks.stream()
                    .map(s -> executor.submit(() -> {
                        StockPriceDto p = stockPriceProvider.getPrice(s.getTicker());
                        List<CandleDto> candles = stockPriceProvider.getHistory(s.getTicker(), "1mo");
                        Map<String, Object> m = new HashMap<>();
                        m.put("ticker", s.getTicker());
                        m.put("name", s.getName());
                        m.put("market", s.getMarket().name());
                        m.put("price", p.price());
                        m.put("change", p.change());
                        m.put("changePercent", p.changePercent());
                        m.put("candles", candles);
                        return m;
                    }))
                    .toList();

            result = futures.stream()
                    .map(f -> {
                        try { return f.get(10, TimeUnit.SECONDS); }
                        catch (Exception e) { return null; }
                    })
                    .filter(Objects::nonNull)
                    .collect(java.util.stream.Collectors.toList());
        }

        featuredCache.put("featured", result);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/api/stocks/search")
    @ResponseBody
    public ResponseEntity<List<Map<String, String>>> searchApi(@RequestParam String q) {
        List<Map<String, String>> results = stockService.search(q).stream()
                .map(s -> Map.of(
                        "ticker", s.getTicker(),
                        "name", s.getName(),
                        "market", s.getMarket().name()
                ))
                .toList();
        return ResponseEntity.ok(results);
    }
}
