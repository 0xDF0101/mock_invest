package com.mockinvest.domain.portfolio;

import com.mockinvest.domain.stock.Stock;
import com.mockinvest.domain.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "dividend_payouts")
@Getter
@NoArgsConstructor
public class DividendPayout {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_id", nullable = false)
    private Stock stock;

    @Column(nullable = false)
    private LocalDate payoutDate;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }

    public static DividendPayout of(User user, Stock stock, LocalDate date, BigDecimal amount) {
        DividendPayout d = new DividendPayout();
        d.user = user;
        d.stock = stock;
        d.payoutDate = date;
        d.amount = amount;
        return d;
    }
}
