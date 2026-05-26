package com.mockinvest.domain.portfolio;

import com.mockinvest.domain.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "portfolio_snapshots")
@Getter
@NoArgsConstructor
public class PortfolioSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDate snapshotDate;

    @Column(nullable = false)
    private BigDecimal totalAsset;

    public static PortfolioSnapshot of(User user, LocalDate date, BigDecimal totalAsset) {
        PortfolioSnapshot s = new PortfolioSnapshot();
        s.user = user;
        s.snapshotDate = date;
        s.totalAsset = totalAsset;
        return s;
    }
}
