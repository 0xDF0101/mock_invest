package com.mockinvest.web.controller;

import com.mockinvest.domain.trade.TradeStatsService;
import com.mockinvest.web.dto.TradeStatsDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class StatsController {

    private final TradeStatsService tradeStatsService;

    @GetMapping("/trade/stats")
    public String stats(Model model, Principal principal) {
        TradeStatsDto stats = tradeStatsService.getStats(principal.getName());
        model.addAttribute("stats", stats);
        model.addAttribute("monthlyLabels", stats.monthly().stream().map(TradeStatsDto.MonthlyCount::label).toList());
        model.addAttribute("monthlyBuy",    stats.monthly().stream().map(TradeStatsDto.MonthlyCount::buy).toList());
        model.addAttribute("monthlySell",   stats.monthly().stream().map(TradeStatsDto.MonthlyCount::sell).toList());
        model.addAttribute("holdingNames",  stats.holdingShares().stream().map(TradeStatsDto.HoldingShare::name).toList());
        model.addAttribute("holdingValues", stats.holdingShares().stream().map(TradeStatsDto.HoldingShare::value).toList());
        return "trade/stats";
    }
}
