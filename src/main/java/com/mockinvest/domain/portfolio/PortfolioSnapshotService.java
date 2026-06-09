package com.mockinvest.domain.portfolio;

import com.mockinvest.domain.user.User;
import com.mockinvest.domain.user.UserRepository;
import com.mockinvest.infrastructure.stockprice.StockPriceDto;
import com.mockinvest.infrastructure.stockprice.StockPriceProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
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

    // 앱 시작 시 스냅샷이 없는 기존 유저 보정
    @EventListener(ApplicationReadyEvent.class)
    public void initMissingSnapshots() {
        LocalDate today = LocalDate.now();
        List<User> targets = userRepository.findAll().stream()
                .filter(u -> !snapshotRepository.existsByUserId(u.getId()))
                .toList();
        if (targets.isEmpty()) return;

        List<Holding> allHoldings = holdingRepository.findAllActiveHoldings();
        Map<Long, List<Holding>> byUser = allHoldings.stream()
                .collect(Collectors.groupingBy(h -> h.getUser().getId()));
        List<String> tickers = allHoldings.stream()
                .map(h -> h.getStock().getTicker()).distinct().toList();
        Map<String, StockPriceDto> prices = tickers.isEmpty()
                ? Map.of()
                : stockPriceProvider.getPrices(tickers);

        for (User user : targets) {
            try {
                List<Holding> holdings = byUser.getOrDefault(user.getId(), List.of());
                BigDecimal evalAmount = holdings.stream()
                        .map(h -> prices.getOrDefault(h.getStock().getTicker(),
                                new StockPriceDto(h.getStock().getTicker(), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO))
                                .price().multiply(BigDecimal.valueOf(h.getQuantity())))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal totalAsset = user.getCashBalance().add(evalAmount);
                snapshotRepository.save(PortfolioSnapshot.of(user, today, totalAsset));
            } catch (Exception e) {
                log.warn("초기 스냅샷 저장 실패: userId={}", user.getId(), e);
            }
        }
        log.info("초기 스냅샷 생성 완료: {}명", targets.size());
    }

    // 신규 가입 유저 초기 스냅샷 (자산 1,000만원, 보유 없음)
    @Transactional
    public void createInitialSnapshot(User user) {
        LocalDate today = LocalDate.now();
        if (snapshotRepository.findByUserIdAndSnapshotDate(user.getId(), today).isPresent()) return;
        snapshotRepository.save(PortfolioSnapshot.of(user, today, new BigDecimal("10000000")));
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
