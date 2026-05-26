package com.mockinvest.domain.trade;

import com.mockinvest.domain.market.MarketHoursService;
import com.mockinvest.domain.portfolio.Holding;
import com.mockinvest.infrastructure.stockprice.TickSizeUtil;
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
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class TradeService {

    private static final BigDecimal COMMISSION_RATE = new BigDecimal("0.00015"); // 0.015%
    private static final BigDecimal TAX_RATE        = new BigDecimal("0.0018");  // 0.18% 증권거래세 (매도만)

    private static final BigDecimal UPPER_LIMIT_RATE = new BigDecimal("1.30");
    private static final BigDecimal LOWER_LIMIT_RATE = new BigDecimal("0.70");

    private void validatePriceLimit(BigDecimal currentPrice, BigDecimal prevClose) {
        if (prevClose.compareTo(BigDecimal.ZERO) == 0) return;
        BigDecimal upper = TickSizeUtil.round(prevClose.multiply(UPPER_LIMIT_RATE));
        BigDecimal lower = TickSizeUtil.round(prevClose.multiply(LOWER_LIMIT_RATE));
        if (currentPrice.compareTo(upper) > 0) {
            throw new IllegalStateException("상한가(" + upper.toPlainString() + "원)를 초과한 가격으로 거래할 수 없습니다.");
        }
        if (currentPrice.compareTo(lower) < 0) {
            throw new IllegalStateException("하한가(" + lower.toPlainString() + "원) 미만 가격으로 거래할 수 없습니다.");
        }
    }

    private BigDecimal calcFee(BigDecimal amount, Trade.TradeType type) {
        BigDecimal commission = amount.multiply(COMMISSION_RATE).setScale(0, RoundingMode.UP);
        BigDecimal tax = type == Trade.TradeType.SELL
                ? amount.multiply(TAX_RATE).setScale(0, RoundingMode.UP)
                : BigDecimal.ZERO;
        return commission.add(tax);
    }

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

        var priceDto = stockPriceProvider.getPrice(ticker);
        BigDecimal currentPrice = priceDto.price();
        validatePriceLimit(currentPrice, priceDto.prevClose());
        BigDecimal amount = currentPrice.multiply(BigDecimal.valueOf(quantity));
        BigDecimal fee = calcFee(amount, Trade.TradeType.BUY);

        user.deductCash(amount.add(fee));

        Holding holding = holdingRepository.findByUserIdAndStockId(user.getId(), stock.getId())
                .orElseGet(() -> holdingRepository.save(Holding.create(user, stock)));
        holding.addShares(quantity, currentPrice);

        Trade trade = tradeRepository.save(Trade.of(user, stock, Trade.TradeType.BUY, currentPrice, quantity, fee));
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

        var priceDto = stockPriceProvider.getPrice(ticker);
        BigDecimal currentPrice = priceDto.price();
        validatePriceLimit(currentPrice, priceDto.prevClose());
        BigDecimal amount = currentPrice.multiply(BigDecimal.valueOf(quantity));
        BigDecimal fee = calcFee(amount, Trade.TradeType.SELL);

        Holding holding = holdingRepository.findByUserIdAndStockId(user.getId(), stock.getId())
                .orElseThrow(() -> new IllegalStateException("보유하지 않은 종목입니다."));
        holding.removeShares(quantity);

        user.addCash(amount.subtract(fee));

        Trade trade = tradeRepository.save(Trade.of(user, stock, Trade.TradeType.SELL, currentPrice, quantity, fee));
        eventPublisher.publishEvent(new TradeCompletedEvent(this, trade));
        return trade;
    }

    @Transactional(readOnly = true)
    public Page<Trade> getHistory(String username, Pageable pageable) {
        User user = userService.getByUsername(username);
        return tradeRepository.findByUserIdWithStock(user.getId(), pageable);
    }
}
