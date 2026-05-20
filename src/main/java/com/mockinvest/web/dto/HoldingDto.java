package com.mockinvest.web.dto;

import java.math.BigDecimal;

public record HoldingDto(
        String ticker,
        String name,
        String market,
        int quantity,
        BigDecimal avgBuyPrice,
        BigDecimal currentPrice,
        BigDecimal change,
        BigDecimal changePercent,
        BigDecimal evalAmount,
        BigDecimal returnRate
) {}
