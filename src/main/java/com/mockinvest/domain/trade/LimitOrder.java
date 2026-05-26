package com.mockinvest.domain.trade;

import com.mockinvest.domain.stock.Stock;
import com.mockinvest.domain.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "limit_orders")
@Getter
@NoArgsConstructor
public class LimitOrder {

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
    private Trade.TradeType type;

    @Column(nullable = false)
    private BigDecimal limitPrice;

    @Column(nullable = false)
    private int quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(nullable = false)
    private BigDecimal reservedCash;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime filledAt;

    public enum OrderStatus { PENDING, FILLED, CANCELLED }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public static LimitOrder of(User user, Stock stock, Trade.TradeType type,
                                BigDecimal limitPrice, int quantity, BigDecimal reservedCash) {
        LimitOrder o = new LimitOrder();
        o.user = user;
        o.stock = stock;
        o.type = type;
        o.limitPrice = limitPrice;
        o.quantity = quantity;
        o.status = OrderStatus.PENDING;
        o.reservedCash = reservedCash;
        return o;
    }

    public void fill() {
        this.status = OrderStatus.FILLED;
        this.filledAt = LocalDateTime.now();
    }

    public void cancel() {
        this.status = OrderStatus.CANCELLED;
    }
}
