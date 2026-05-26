package com.mockinvest.domain.trade;

import com.mockinvest.domain.stock.Stock;
import com.mockinvest.domain.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "trades")
@Getter
@NoArgsConstructor
public class Trade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_id", nullable = false)
    private Stock stock;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TradeType type;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private BigDecimal fee;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum TradeType {
        BUY, SELL
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public static Trade of(User user, Stock stock, TradeType type, BigDecimal price, int quantity, BigDecimal fee) {
        Trade t = new Trade();
        t.user = user;
        t.stock = stock;
        t.type = type;
        t.price = price;
        t.quantity = quantity;
        t.amount = price.multiply(BigDecimal.valueOf(quantity));
        t.fee = fee;
        return t;
    }
}
