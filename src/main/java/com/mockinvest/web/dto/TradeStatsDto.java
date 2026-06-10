package com.mockinvest.web.dto;

import java.math.BigDecimal;
import java.util.List;

public record TradeStatsDto(
        int totalTrades,
        int buyCount,
        int sellCount,
        double winRate,
        int holdingCount,
        List<TopStock> topStocks,
        List<MonthlyCount> monthly,
        List<HoldingShare> holdingShares
) {
    public record TopStock(String name, String ticker, long count) {}
    public record MonthlyCount(String label, long buy, long sell) {}
    public record HoldingShare(String name, BigDecimal value) {}
}
