package com.mockinvest.web.controller;

import com.mockinvest.domain.portfolio.PortfolioService;
import com.mockinvest.infrastructure.stockprice.StockPriceDto;
import com.mockinvest.infrastructure.stockprice.StockPriceProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.security.Principal;

@Controller
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final PortfolioService portfolioService;
    private final StockPriceProvider stockPriceProvider;

    @GetMapping
    public String dashboard(Model model, Principal principal) {
        model.addAttribute("portfolio", portfolioService.getPortfolio(principal.getName()));
        model.addAttribute("kospi", stockPriceProvider.getPrice("^KS11"));
        model.addAttribute("kosdaq", stockPriceProvider.getPrice("^KQ11"));
        return "dashboard/index";
    }
}
