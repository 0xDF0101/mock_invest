package com.mockinvest.domain.portfolio;

import com.mockinvest.infrastructure.stockprice.StockPriceProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DividendService {

    private final HoldingRepository holdingRepository;
    private final DividendPayoutRepository dividendPayoutRepository;
    private final StockPriceProvider stockPriceProvider;

    // 평일 15:35 KST — 장마감 직후 배당금 확인
    @Scheduled(cron = "0 35 15 * * MON-FRI", zone = "Asia/Seoul")
    public void processDividends() {
        LocalDate today = LocalDate.now();
        List<Holding> holdings = holdingRepository.findAllActiveHoldings();

        Map<String, List<Holding>> byTicker = holdings.stream()
                .collect(Collectors.groupingBy(h -> h.getStock().getTicker()));

        for (Map.Entry<String, List<Holding>> entry : byTicker.entrySet()) {
            String ticker = entry.getKey();
            try {
                BigDecimal dividendPerShare = stockPriceProvider.getDividendToday(ticker);
                if (dividendPerShare.compareTo(BigDecimal.ZERO) <= 0) continue;

                for (Holding holding : entry.getValue()) {
                    payDividend(holding, today, dividendPerShare);
                }
            } catch (Exception e) {
                log.warn("배당금 처리 실패: ticker={}", ticker, e);
            }
        }
    }

    @Transactional
    protected void payDividend(Holding holding, LocalDate date, BigDecimal dividendPerShare) {
        Long userId = holding.getUser().getId();
        Long stockId = holding.getStock().getId();

        if (dividendPayoutRepository.findByUserIdAndStockIdAndPayoutDate(userId, stockId, date).isPresent()) return;

        BigDecimal total = dividendPerShare.multiply(BigDecimal.valueOf(holding.getQuantity()));
        holding.getUser().addCash(total);
        dividendPayoutRepository.save(DividendPayout.of(holding.getUser(), holding.getStock(), date, total));
        log.info("배당금 지급: {} {} {}주 × {}원 = {}원",
                holding.getUser().getUsername(), holding.getStock().getTicker(),
                holding.getQuantity(), dividendPerShare, total);
    }
}
