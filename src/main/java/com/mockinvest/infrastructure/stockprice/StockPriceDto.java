package com.mockinvest.infrastructure.stockprice;

import java.math.BigDecimal;

public record StockPriceDto(
        String ticker,
        BigDecimal price,
        BigDecimal change,
        BigDecimal changePercent
) {}
