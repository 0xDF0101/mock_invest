package com.mockinvest.web.controller;

import com.mockinvest.domain.market.MarketHoursService;
import com.mockinvest.domain.portfolio.PortfolioService;
import com.mockinvest.domain.portfolio.PortfolioSnapshotService;
import com.mockinvest.domain.portfolio.PortfolioSnapshot;
import com.mockinvest.domain.user.UserService;
import com.mockinvest.infrastructure.stockprice.StockPriceProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final PortfolioService portfolioService;
    private final PortfolioSnapshotService snapshotService;
    private final UserService userService;
    private final StockPriceProvider stockPriceProvider;
    private final MarketHoursService marketHoursService;

    @GetMapping
    public String dashboard(Model model, Principal principal) {
        model.addAttribute("portfolio", portfolioService.getPortfolio(principal.getName()));
        model.addAttribute("kospi", stockPriceProvider.getPrice("^KS11"));
        model.addAttribute("kosdaq", stockPriceProvider.getPrice("^KQ11"));
        model.addAttribute("marketOpen", marketHoursService.isOpen());
        model.addAttribute("marketStatus", marketHoursService.getStatusLabel());
        model.addAttribute("nextEventMs", marketHoursService.getNextEventMillis());
        model.addAttribute("nextEventOpen", marketHoursService.isNextEventOpen());

        Long userId = userService.getByUsername(principal.getName()).getId();
        List<PortfolioSnapshot> snapshots = snapshotService.getSnapshots(userId);
        model.addAttribute("snapshotDates", snapshots.stream().map(s -> s.getSnapshotDate().toString()).toList());
        model.addAttribute("snapshotAssets", snapshots.stream().map(s -> s.getTotalAsset().longValue()).toList());

        return "dashboard/index";
    }
}
