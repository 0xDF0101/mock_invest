package com.mockinvest.domain.portfolio;

import com.mockinvest.domain.user.User;
import com.mockinvest.domain.user.UserRepository;
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
public class PortfolioSnapshotService {

    private final PortfolioSnapshotRepository snapshotRepository;
    private final HoldingRepository holdingRepository;
    private final UserRepository userRepository;
    private final StockPriceProvider stockPriceProvider;

    @Transactional(readOnly = true)
    public List<PortfolioSnapshot> getSnapshots(Long userId) {
        return snapshotRepository.findByUserIdOrderBySnapshotDateAsc(userId);
    }

    // 평일 15:31 KST에 장마감 직후 스냅샷 저장
    @Scheduled(cron = "0 31 15 * * MON-FRI", zone = "Asia/Seoul")
    public void takeSnapshot() {
        LocalDate today = LocalDate.now();
        List<User> users = userRepository.findAll();
        List<Holding> allHoldings = holdingRepository.findAllActiveHoldings();

        Map<Long, List<Holding>> byUser = allHoldings.stream()
                .collect(Collectors.groupingBy(h -> h.getUser().getId()));

        List<String> tickers = allHoldings.stream()
                .map(h -> h.getStock().getTicker()).distinct().toList();

        var prices = tickers.isEmpty() ? Map.of() : stockPriceProvider.getPrices(tickers);

        for (User user : users) {
            try {
                saveSnapshot(user, today, byUser, prices);
            } catch (Exception e) {
                log.warn("스냅샷 저장 실패: userId={}", user.getId(), e);
            }
        }
        log.info("포트폴리오 스냅샷 저장 완료: {}명", users.size());
    }

    @Transactional
    protected void saveSnapshot(User user, LocalDate date,
                                Map<Long, List<Holding>> byUser,
                                Map<?, ?> prices) {
        if (snapshotRepository.findByUserIdAndSnapshotDate(user.getId(), date).isPresent()) return;

        List<Holding> holdings = byUser.getOrDefault(user.getId(), List.of());

        @SuppressWarnings("unchecked")
        var priceMap = (Map<String, com.mockinvest.infrastructure.stockprice.StockPriceDto>) prices;

        BigDecimal evalAmount = holdings.stream()
                .map(h -> priceMap.getOrDefault(
                        h.getStock().getTicker(),
                        new com.mockinvest.infrastructure.stockprice.StockPriceDto(
                                h.getStock().getTicker(), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO)
                ).price().multiply(BigDecimal.valueOf(h.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalAsset = user.getCashBalance().add(evalAmount);
        snapshotRepository.save(PortfolioSnapshot.of(user, date, totalAsset));
    }
}
