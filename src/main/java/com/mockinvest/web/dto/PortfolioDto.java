package com.mockinvest.web.dto;

import java.math.BigDecimal;
import java.util.List;

public record PortfolioDto(
        BigDecimal totalAsset,
        BigDecimal cashBalance,
        BigDecimal evalAmount,
        BigDecimal returnRate,
        List<HoldingDto> holdings
) {}
