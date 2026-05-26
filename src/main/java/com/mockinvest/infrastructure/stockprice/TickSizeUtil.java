package com.mockinvest.infrastructure.stockprice;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class TickSizeUtil {

    private TickSizeUtil() {}

    public static BigDecimal tickSize(BigDecimal price) {
        int p = price.intValue();
        if (p < 1_000)   return BigDecimal.ONE;
        if (p < 5_000)   return new BigDecimal("5");
        if (p < 10_000)  return new BigDecimal("10");
        if (p < 50_000)  return new BigDecimal("50");
        if (p < 100_000) return new BigDecimal("100");
        if (p < 500_000) return new BigDecimal("500");
        return new BigDecimal("1000");
    }

    public static BigDecimal round(BigDecimal price) {
        if (price.compareTo(BigDecimal.ZERO) <= 0) return price;
        BigDecimal tick = tickSize(price);
        return price.divide(tick, 0, RoundingMode.HALF_UP).multiply(tick);
    }
}
