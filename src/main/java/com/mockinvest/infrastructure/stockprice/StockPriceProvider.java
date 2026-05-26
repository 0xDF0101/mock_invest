package com.mockinvest.infrastructure.stockprice;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface StockPriceProvider {
    StockPriceDto getPrice(String ticker);
    Map<String, StockPriceDto> getPrices(List<String> tickers);
    List<CandleDto> getHistory(String ticker, String period);
    BigDecimal getDividendToday(String ticker);
}
