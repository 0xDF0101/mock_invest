package com.mockinvest.web.controller;

import com.mockinvest.domain.watchlist.WatchlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class WatchlistController {

    private final WatchlistService watchlistService;

    @GetMapping("/watchlist")
    public String index(Model model, Principal principal) {
        model.addAttribute("items", watchlistService.getWatchlistWithPrices(principal.getName()));
        return "watchlist/index";
    }

    @PostMapping("/watchlist/toggle")
    public String toggle(@RequestParam String ticker,
                         @RequestParam(defaultValue = "") String redirect,
                         Principal principal,
                         RedirectAttributes ra) {
        try {
            watchlistService.toggle(principal.getName(), ticker);
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:" + (redirect.isBlank() ? "/watchlist" : redirect);
    }
}
