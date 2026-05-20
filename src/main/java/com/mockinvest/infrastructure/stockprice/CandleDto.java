package com.mockinvest.infrastructure.stockprice;

import java.math.BigDecimal;

public record CandleDto(
        long time,
        BigDecimal open,
        BigDecimal high,
        BigDecimal low,
        BigDecimal close,
        long volume
) {}
