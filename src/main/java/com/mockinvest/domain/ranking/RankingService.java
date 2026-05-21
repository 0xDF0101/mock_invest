package com.mockinvest.domain.ranking;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.mockinvest.domain.portfolio.Holding;
import com.mockinvest.domain.portfolio.HoldingRepository;
import com.mockinvest.domain.user.User;
import com.mockinvest.domain.user.UserRepository;
import com.mockinvest.infrastructure.stockprice.StockPriceDto;
import com.mockinvest.infrastructure.stockprice.StockPriceProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RankingService {

    private final UserRepository userRepository;
    private final HoldingRepository holdingRepository;
    private final StockPriceProvider stockPriceProvider;

    private static final BigDecimal INITIAL_CAPITAL = new BigDecimal("10000000");

    private final Cache<String, List<RankEntry>> rankCache = Caffeine.newBuilder()
            .expireAfterWrite(60, TimeUnit.SECONDS)
            .maximumSize(1)
            .build();

    public List<RankEntry> getRankings() {
        return rankCache.get("all", k -> compute());
    }

    @Transactional(readOnly = true)
    protected List<RankEntry> compute() {
        List<User> users = userRepository.findAll();
        List<Holding> allHoldings = holdingRepository.findAllActiveHoldings();

        Map<Long, List<Holding>> byUser = allHoldings.stream()
                .collect(Collectors.groupingBy(h -> h.getUser().getId()));

        List<String> tickers = allHoldings.stream()
                .map(h -> h.getStock().getTicker())
                .distinct()
                .toList();

        Map<String, StockPriceDto> prices = tickers.isEmpty()
                ? Map.of()
                : stockPriceProvider.getPrices(tickers);

        List<RankEntry> unsorted = new ArrayList<>();
        for (User user : users) {
            List<Holding> holdings = byUser.getOrDefault(user.getId(), List.of());

            BigDecimal evalAmount = holdings.stream()
                    .map(h -> prices.getOrDefault(
                            h.getStock().getTicker(),
                            new StockPriceDto(h.getStock().getTicker(), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO)
                    ).price().multiply(BigDecimal.valueOf(h.getQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal totalAsset = user.getCashBalance().add(evalAmount);
            BigDecimal returnRate = totalAsset.subtract(INITIAL_CAPITAL)
                    .divide(INITIAL_CAPITAL, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));

            unsorted.add(new RankEntry(0, user.getUsername(), totalAsset, returnRate));
        }

        unsorted.sort((a, b) -> b.returnRate().compareTo(a.returnRate()));

        List<RankEntry> ranked = new ArrayList<>();
        for (int i = 0; i < unsorted.size(); i++) {
            RankEntry e = unsorted.get(i);
            ranked.add(new RankEntry(i + 1, e.username(), e.totalAsset(), e.returnRate()));
        }
        return ranked;
    }
}
