package com.mockinvest.domain.trade;

import com.mockinvest.domain.market.MarketHoursService;
import com.mockinvest.domain.portfolio.Holding;
import com.mockinvest.domain.portfolio.HoldingRepository;
import com.mockinvest.domain.stock.Stock;
import com.mockinvest.domain.stock.StockService;
import com.mockinvest.domain.user.User;
import com.mockinvest.domain.user.UserService;
import com.mockinvest.event.TradeCompletedEvent;
import com.mockinvest.infrastructure.stockprice.StockPriceProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class TradeService {

    private final TradeRepository tradeRepository;
    private final HoldingRepository holdingRepository;
    private final UserService userService;
    private final StockService stockService;
    private final StockPriceProvider stockPriceProvider;
    private final ApplicationEventPublisher eventPublisher;
    private final MarketHoursService marketHoursService;

    @Transactional
    public Trade buy(String username, String ticker, int quantity) {
        if (!marketHoursService.isOpen()) {
            throw new IllegalStateException("장 운영 시간이 아닙니다. 평일 09:00 ~ 15:30에 거래 가능합니다.");
        }
        User user = userService.getByUsername(username);
        Stock stock = stockService.getByTicker(ticker);

        BigDecimal currentPrice = stockPriceProvider.getPrice(ticker).price();
        BigDecimal totalCost = currentPrice.multiply(BigDecimal.valueOf(quantity));

        user.deductCash(totalCost);

        Holding holding = holdingRepository.findByUserIdAndStockId(user.getId(), stock.getId())
                .orElseGet(() -> holdingRepository.save(Holding.create(user, stock)));
        holding.addShares(quantity, currentPrice);

        Trade trade = tradeRepository.save(Trade.of(user, stock, Trade.TradeType.BUY, currentPrice, quantity));
        eventPublisher.publishEvent(new TradeCompletedEvent(this, trade));
        return trade;
    }

    @Transactional
    public Trade sell(String username, String ticker, int quantity) {
        if (!marketHoursService.isOpen()) {
            throw new IllegalStateException("장 운영 시간이 아닙니다. 평일 09:00 ~ 15:30에 거래 가능합니다.");
        }
        User user = userService.getByUsername(username);
        Stock stock = stockService.getByTicker(ticker);

        BigDecimal currentPrice = stockPriceProvider.getPrice(ticker).price();
        BigDecimal totalAmount = currentPrice.multiply(BigDecimal.valueOf(quantity));

        Holding holding = holdingRepository.findByUserIdAndStockId(user.getId(), stock.getId())
                .orElseThrow(() -> new IllegalStateException("보유하지 않은 종목입니다."));
        holding.removeShares(quantity);

        user.addCash(totalAmount);

        Trade trade = tradeRepository.save(Trade.of(user, stock, Trade.TradeType.SELL, currentPrice, quantity));
        eventPublisher.publishEvent(new TradeCompletedEvent(this, trade));
        return trade;
    }

    @Transactional(readOnly = true)
    public Page<Trade> getHistory(String username, Pageable pageable) {
        User user = userService.getByUsername(username);
        return tradeRepository.findByUserIdWithStock(user.getId(), pageable);
    }
}
