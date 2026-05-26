package com.mockinvest.domain.portfolio;

import com.mockinvest.domain.user.User;
import com.mockinvest.domain.user.UserService;
import com.mockinvest.infrastructure.stockprice.StockPriceDto;
import com.mockinvest.infrastructure.stockprice.StockPriceProvider;
import com.mockinvest.web.dto.HoldingDto;
import com.mockinvest.web.dto.PortfolioDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PortfolioService {

    private final HoldingRepository holdingRepository;
    private final UserService userService;
    private final StockPriceProvider stockPriceProvider;

    private static final BigDecimal INITIAL_CAPITAL = new BigDecimal("10000000");

    @Transactional(readOnly = true)
    public PortfolioDto getPortfolio(String username) {
        User user = userService.getByUsername(username);
        List<Holding> holdings = holdingRepository.findActiveHoldingsByUserId(user.getId());

        List<String> tickers = holdings.stream()
                .map(h -> h.getStock().getTicker())
                .collect(Collectors.toList());

        Map<String, StockPriceDto> prices = tickers.isEmpty()
                ? Map.of()
                : stockPriceProvider.getPrices(tickers);

        List<HoldingDto> holdingDtos = holdings.stream()
                .map(h -> {
                    StockPriceDto price = prices.getOrDefault(
                            h.getStock().getTicker(),
                            new StockPriceDto(h.getStock().getTicker(), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO)
                    );
                    BigDecimal evalAmount = price.price().multiply(BigDecimal.valueOf(h.getQuantity()));
                    BigDecimal returnRate = h.getAvgBuyPrice().compareTo(BigDecimal.ZERO) == 0
                            ? BigDecimal.ZERO
                            : price.price().subtract(h.getAvgBuyPrice())
                                .divide(h.getAvgBuyPrice(), 4, RoundingMode.HALF_UP)
                                .multiply(BigDecimal.valueOf(100));

                    return new HoldingDto(
                            h.getStock().getTicker(),
                            h.getStock().getName(),
                            h.getStock().getMarket().name(),
                            h.getQuantity(),
                            h.getAvgBuyPrice(),
                            price.price(),
                            price.change(),
                            price.changePercent(),
                            evalAmount,
                            returnRate
                    );
                })
                .collect(Collectors.toList());

        BigDecimal evalTotal = holdingDtos.stream()
                .map(HoldingDto::evalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalAsset = user.getCashBalance().add(evalTotal);
        BigDecimal returnRate = totalAsset.subtract(INITIAL_CAPITAL)
                .divide(INITIAL_CAPITAL, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));

        return new PortfolioDto(totalAsset, user.getCashBalance(), evalTotal, returnRate, holdingDtos);
    }
}
