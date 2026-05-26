package com.mockinvest.web.controller;

import com.mockinvest.domain.trade.LimitOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class LimitOrderController {

    private final LimitOrderService limitOrderService;

    @PostMapping("/trade/limit/buy")
    public String placeBuy(@RequestParam String ticker,
                           @RequestParam BigDecimal limitPrice,
                           @RequestParam int quantity,
                           Principal principal,
                           RedirectAttributes ra) {
        try {
            limitOrderService.placeBuy(principal.getName(), ticker, limitPrice, quantity);
            ra.addFlashAttribute("successMessage", "지정가 매수 주문이 등록되었습니다.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/stocks/" + ticker;
    }

    @PostMapping("/trade/limit/sell")
    public String placeSell(@RequestParam String ticker,
                            @RequestParam BigDecimal limitPrice,
                            @RequestParam int quantity,
                            Principal principal,
                            RedirectAttributes ra) {
        try {
            limitOrderService.placeSell(principal.getName(), ticker, limitPrice, quantity);
            ra.addFlashAttribute("successMessage", "지정가 매도 주문이 등록되었습니다.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/stocks/" + ticker;
    }

    @PostMapping("/trade/limit/cancel/{id}")
    public String cancel(@PathVariable Long id,
                         Principal principal,
                         RedirectAttributes ra) {
        try {
            limitOrderService.cancel(principal.getName(), id);
            ra.addFlashAttribute("successMessage", "주문이 취소되었습니다.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/trade/orders";
    }

    @GetMapping("/trade/orders")
    public String orders(Model model, Principal principal) {
        model.addAttribute("orders", limitOrderService.getOrders(principal.getName()));
        return "trade/orders";
    }
}
