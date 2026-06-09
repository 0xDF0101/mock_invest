package com.mockinvest.domain.portfolio;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PortfolioSnapshotRepository extends JpaRepository<PortfolioSnapshot, Long> {

    List<PortfolioSnapshot> findByUserIdOrderBySnapshotDateAsc(Long userId);

    Optional<PortfolioSnapshot> findByUserIdAndSnapshotDate(Long userId, LocalDate date);

    boolean existsByUserId(Long userId);
}
