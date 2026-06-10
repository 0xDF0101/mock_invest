package com.mockinvest.domain.watchlist;

import com.mockinvest.domain.stock.StockService;
import com.mockinvest.domain.user.UserService;
import com.mockinvest.infrastructure.stockprice.StockPriceDto;
import com.mockinvest.infrastructure.stockprice.StockPriceProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class WatchlistService {

    private final WatchlistRepository watchlistRepository;
    private final UserService userService;
    private final StockService stockService;
    private final StockPriceProvider stockPriceProvider;

    @Transactional
    public void toggle(String username, String ticker) {
        var user  = userService.getByUsername(username);
        var stock = stockService.getByTicker(ticker);
        if (watchlistRepository.existsByUserIdAndStockId(user.getId(), stock.getId())) {
            watchlistRepository.deleteByUserIdAndStockId(user.getId(), stock.getId());
        } else {
            watchlistRepository.save(Watchlist.of(user, stock));
        }
    }

    @Transactional(readOnly = true)
    public boolean isWatched(String username, String ticker) {
        var user  = userService.getByUsername(username);
        var stock = stockService.getByTicker(ticker);
        return watchlistRepository.existsByUserIdAndStockId(user.getId(), stock.getId());
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getWatchlistWithPrices(String username) {
        var user  = userService.getByUsername(username);
        List<Watchlist> items = watchlistRepository.findByUserIdWithStock(user.getId());

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<Map<String, Object>>> futures = items.stream()
                    .map(w -> executor.submit(() -> {
                        StockPriceDto p = stockPriceProvider.getPrice(w.getStock().getTicker());
                        Map<String, Object> m = new LinkedHashMap<>();
                        m.put("ticker",        w.getStock().getTicker());
                        m.put("name",          w.getStock().getName());
                        m.put("market",        w.getStock().getMarket().name());
                        m.put("price",         p.price());
                        m.put("change",        p.change());
                        m.put("changePercent", p.changePercent());
                        return m;
                    }))
                    .toList();

            return futures.stream()
                    .map(f -> {
                        try { return f.get(10, TimeUnit.SECONDS); }
                        catch (Exception e) { return null; }
                    })
                    .filter(Objects::nonNull)
                    .collect(java.util.stream.Collectors.toList());
        }
    }
}
