package com.mockinvest.web.controller;

import com.mockinvest.domain.trade.TradeService;
import com.mockinvest.web.dto.TradeRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class TradeController {

    private final TradeService tradeService;

    @PostMapping("/trade/buy")
    public String buy(@Valid @ModelAttribute TradeRequest req,
                      Principal principal,
                      RedirectAttributes redirectAttributes) {
        try {
            tradeService.buy(principal.getName(), req.getTicker(), req.getQuantity());
            redirectAttributes.addFlashAttribute("successMessage", "매수가 완료되었습니다.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/stocks/" + req.getTicker();
    }

    @PostMapping("/trade/sell")
    public String sell(@Valid @ModelAttribute TradeRequest req,
                       Principal principal,
                       RedirectAttributes redirectAttributes) {
        try {
            tradeService.sell(principal.getName(), req.getTicker(), req.getQuantity());
            redirectAttributes.addFlashAttribute("successMessage", "매도가 완료되었습니다.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/stocks/" + req.getTicker();
    }

    @GetMapping("/trade/history")
    public String history(@RequestParam(defaultValue = "0") int page,
                          Model model,
                          Principal principal) {
        model.addAttribute("trades", tradeService.getHistory(principal.getName(), PageRequest.of(page, 20)));
        return "trade/history";
    }
}
