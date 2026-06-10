package com.mockinvest.domain.trade;

import com.mockinvest.domain.portfolio.Holding;
import com.mockinvest.domain.portfolio.HoldingRepository;
import com.mockinvest.domain.stock.Stock;
import com.mockinvest.domain.user.UserService;
import com.mockinvest.infrastructure.stockprice.StockPriceProvider;
import com.mockinvest.web.dto.TradeStatsDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TradeStatsService {

    private final TradeRepository tradeRepository;
    private final HoldingRepository holdingRepository;
    private final StockPriceProvider stockPriceProvider;
    private final UserService userService;

    @Transactional(readOnly = true)
    public TradeStatsDto getStats(String username) {
        var user = userService.getByUsername(username);
        List<Trade> trades = tradeRepository.findAllByUserIdWithStock(user.getId());
        List<Holding> holdings = holdingRepository.findActiveHoldingsByUserId(user.getId());

        int buyCount  = (int) trades.stream().filter(t -> t.getType() == Trade.TradeType.BUY).count();
        int sellCount = (int) trades.stream().filter(t -> t.getType() == Trade.TradeType.SELL).count();

        Map<Long, BigDecimal> avgBuyByStockId = computeAvgBuyPrices(trades);
        long wins = trades.stream()
                .filter(t -> t.getType() == Trade.TradeType.SELL)
                .filter(t -> {
                    BigDecimal avg = avgBuyByStockId.get(t.getStock().getId());
                    return avg != null && t.getPrice().compareTo(avg) > 0;
                })
                .count();
        double winRate = sellCount > 0 ? wins * 100.0 / sellCount : 0.0;

        List<TradeStatsDto.TopStock> topStocks = trades.stream()
                .collect(Collectors.groupingBy(Trade::getStock, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<Stock, Long>comparingByValue().reversed())
                .limit(5)
                .map(e -> new TradeStatsDto.TopStock(e.getKey().getName(), e.getKey().getTicker(), e.getValue()))
                .toList();

        List<TradeStatsDto.MonthlyCount> monthly = buildMonthlyData(trades);

        List<TradeStatsDto.HoldingShare> holdingShares = holdings.stream()
                .map(h -> {
                    try {
                        BigDecimal price = stockPriceProvider.getPrice(h.getStock().getTicker()).price();
                        return new TradeStatsDto.HoldingShare(
                                h.getStock().getName(),
                                price.multiply(BigDecimal.valueOf(h.getQuantity()))
                        );
                    } catch (Exception e) {
                        return new TradeStatsDto.HoldingShare(h.getStock().getName(), BigDecimal.ZERO);
                    }
                })
                .filter(s -> s.value().compareTo(BigDecimal.ZERO) > 0)
                .toList();

        return new TradeStatsDto(trades.size(), buyCount, sellCount, winRate,
                holdings.size(), topStocks, monthly, holdingShares);
    }

    private Map<Long, BigDecimal> computeAvgBuyPrices(List<Trade> trades) {
        Map<Long, BigDecimal> totalCost = new HashMap<>();
        Map<Long, Integer> totalQty = new HashMap<>();
        for (Trade t : trades) {
            if (t.getType() == Trade.TradeType.BUY) {
                Long id = t.getStock().getId();
                totalCost.merge(id, t.getPrice().multiply(BigDecimal.valueOf(t.getQuantity())), BigDecimal::add);
                totalQty.merge(id, t.getQuantity(), Integer::sum);
            }
        }
        Map<Long, BigDecimal> result = new HashMap<>();
        for (var entry : totalCost.entrySet()) {
            int qty = totalQty.get(entry.getKey());
            result.put(entry.getKey(),
                    entry.getValue().divide(BigDecimal.valueOf(qty), 2, RoundingMode.HALF_UP));
        }
        return result;
    }

    private List<TradeStatsDto.MonthlyCount> buildMonthlyData(List<Trade> trades) {
        YearMonth now = YearMonth.now();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yy.MM");
        List<TradeStatsDto.MonthlyCount> result = new ArrayList<>();
        for (int i = 11; i >= 0; i--) {
            YearMonth ym = now.minusMonths(i);
            long buy = trades.stream()
                    .filter(t -> t.getType() == Trade.TradeType.BUY
                            && YearMonth.from(t.getCreatedAt()).equals(ym))
                    .count();
            long sell = trades.stream()
                    .filter(t -> t.getType() == Trade.TradeType.SELL
                            && YearMonth.from(t.getCreatedAt()).equals(ym))
                    .count();
            result.add(new TradeStatsDto.MonthlyCount(ym.format(fmt), buy, sell));
        }
        return result;
    }
}
