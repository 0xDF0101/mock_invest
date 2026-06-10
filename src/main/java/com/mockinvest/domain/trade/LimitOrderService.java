package com.mockinvest.domain.trade;

import com.mockinvest.domain.market.MarketHoursService;
import com.mockinvest.domain.portfolio.Holding;
import com.mockinvest.domain.portfolio.HoldingRepository;
import com.mockinvest.domain.stock.Stock;
import com.mockinvest.domain.stock.StockService;
import com.mockinvest.domain.user.User;
import com.mockinvest.domain.user.UserService;
import com.mockinvest.infrastructure.stockprice.StockPriceProvider;
import com.mockinvest.infrastructure.stockprice.TickSizeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LimitOrderService {

    @Lazy
    @Autowired
    private LimitOrderService self;

    private static final BigDecimal COMMISSION_RATE = new BigDecimal("0.00015");
    private static final BigDecimal TAX_RATE        = new BigDecimal("0.0018");

    private final LimitOrderRepository limitOrderRepository;
    private final HoldingRepository holdingRepository;
    private final TradeRepository tradeRepository;
    private final UserService userService;
    private final StockService stockService;
    private final StockPriceProvider stockPriceProvider;
    private final MarketHoursService marketHoursService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public LimitOrder placeBuy(String username, String ticker, BigDecimal limitPrice, int quantity) {
        limitPrice = TickSizeUtil.round(limitPrice);
        User user = userService.getByUsername(username);
        Stock stock = stockService.getByTicker(ticker);

        BigDecimal amount = limitPrice.multiply(BigDecimal.valueOf(quantity));
        BigDecimal fee = amount.multiply(COMMISSION_RATE).setScale(0, RoundingMode.UP);
        BigDecimal reserved = amount.add(fee);

        user.deductCash(reserved);
        return limitOrderRepository.save(LimitOrder.of(user, stock, Trade.TradeType.BUY, limitPrice, quantity, reserved));
    }

    @Transactional
    public LimitOrder placeSell(String username, String ticker, BigDecimal limitPrice, int quantity) {
        limitPrice = TickSizeUtil.round(limitPrice);
        User user = userService.getByUsername(username);
        Stock stock = stockService.getByTicker(ticker);

        Holding holding = holdingRepository.findByUserIdAndStockId(user.getId(), stock.getId())
                .orElseThrow(() -> new IllegalStateException("보유하지 않은 종목입니다."));
        if (holding.getQuantity() < quantity) {
            throw new IllegalStateException("보유 수량이 부족합니다.");
        }
        return limitOrderRepository.save(LimitOrder.of(user, stock, Trade.TradeType.SELL, limitPrice, quantity, BigDecimal.ZERO));
    }

    @Transactional
    public void cancel(String username, Long orderId) {
        LimitOrder order = limitOrderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalStateException("존재하지 않는 주문입니다."));
        if (!order.getUser().getUsername().equals(username)) {
            throw new IllegalStateException("본인의 주문만 취소할 수 있습니다.");
        }
        if (order.getStatus() != LimitOrder.OrderStatus.PENDING) {
            throw new IllegalStateException("대기 중인 주문만 취소할 수 있습니다.");
        }
        order.cancel();
        if (order.getReservedCash().compareTo(BigDecimal.ZERO) > 0) {
            order.getUser().addCash(order.getReservedCash());
        }
    }

    @Transactional(readOnly = true)
    public List<LimitOrder> getOrders(String username) {
        User user = userService.getByUsername(username);
        return limitOrderRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
    }

    @Scheduled(fixedDelay = 30_000)
    public void checkAndFill() {
        if (!marketHoursService.isOpen()) return;

        List<LimitOrder> pending = limitOrderRepository.findAllPending();
        for (LimitOrder order : pending) {
            try {
                BigDecimal currentPrice = stockPriceProvider.getPrice(order.getStock().getTicker()).price();
                if (order.getType() == Trade.TradeType.BUY && currentPrice.compareTo(order.getLimitPrice()) <= 0) {
                    self.fillBuy(order, currentPrice);
                } else if (order.getType() == Trade.TradeType.SELL && currentPrice.compareTo(order.getLimitPrice()) >= 0) {
                    self.fillSell(order, currentPrice);
                }
            } catch (Exception e) {
                log.warn("지정가 주문 체결 처리 실패: orderId={}", order.getId(), e);
            }
        }
    }

    @Transactional
    public void fillBuy(LimitOrder order, BigDecimal fillPrice) {
        BigDecimal amount = fillPrice.multiply(BigDecimal.valueOf(order.getQuantity()));
        BigDecimal fee = amount.multiply(COMMISSION_RATE).setScale(0, RoundingMode.UP);
        BigDecimal actualCost = amount.add(fee);
        BigDecimal refund = order.getReservedCash().subtract(actualCost);

        Holding holding = holdingRepository.findByUserIdAndStockId(order.getUser().getId(), order.getStock().getId())
                .orElseGet(() -> holdingRepository.save(Holding.create(order.getUser(), order.getStock())));
        holding.addShares(order.getQuantity(), fillPrice);

        if (refund.compareTo(BigDecimal.ZERO) > 0) {
            order.getUser().addCash(refund);
        }
        order.fill();
        Trade trade = tradeRepository.save(Trade.of(order.getUser(), order.getStock(), Trade.TradeType.BUY, fillPrice, order.getQuantity(), fee));
        eventPublisher.publishEvent(new com.mockinvest.event.TradeCompletedEvent(this, trade));
        log.info("지정가 매수 체결: {} {} {}주 @{}원", order.getUser().getUsername(), order.getStock().getTicker(), order.getQuantity(), fillPrice);
    }

    @Transactional
    public void fillSell(LimitOrder order, BigDecimal fillPrice) {
        Holding holding = holdingRepository.findByUserIdAndStockId(order.getUser().getId(), order.getStock().getId())
                .orElseThrow(() -> new IllegalStateException("보유 종목 없음"));
        if (holding.getQuantity() < order.getQuantity()) {
            order.cancel();
            log.warn("지정가 매도 취소 (보유 수량 부족): orderId={}", order.getId());
            return;
        }
        holding.removeShares(order.getQuantity());

        BigDecimal amount = fillPrice.multiply(BigDecimal.valueOf(order.getQuantity()));
        BigDecimal commission = amount.multiply(COMMISSION_RATE).setScale(0, RoundingMode.UP);
        BigDecimal tax = amount.multiply(TAX_RATE).setScale(0, RoundingMode.UP);
        BigDecimal fee = commission.add(tax);

        order.getUser().addCash(amount.subtract(fee));
        order.fill();
        Trade trade = tradeRepository.save(Trade.of(order.getUser(), order.getStock(), Trade.TradeType.SELL, fillPrice, order.getQuantity(), fee));
        eventPublisher.publishEvent(new com.mockinvest.event.TradeCompletedEvent(this, trade));
        log.info("지정가 매도 체결: {} {} {}주 @{}원", order.getUser().getUsername(), order.getStock().getTicker(), order.getQuantity(), fillPrice);
    }
}
