package com.mockinvest.event;

import com.mockinvest.domain.trade.Trade;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class TradeCompletedEvent extends ApplicationEvent {

    private final Trade trade;

    public TradeCompletedEvent(Object source, Trade trade) {
        super(source);
        this.trade = trade;
    }
}
