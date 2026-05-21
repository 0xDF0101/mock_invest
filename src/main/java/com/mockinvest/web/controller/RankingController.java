package com.mockinvest.web.controller;

import com.mockinvest.domain.ranking.RankEntry;
import com.mockinvest.domain.ranking.RankingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class RankingController {

    private final RankingService rankingService;

    @GetMapping("/ranking")
    public String ranking(Model model, Principal principal) {
        List<RankEntry> all = rankingService.getRankings();

        List<RankEntry> top10 = all.stream().limit(10).toList();
        RankEntry myEntry = all.stream()
                .filter(e -> e.username().equals(principal.getName()))
                .findFirst()
                .orElse(null);

        model.addAttribute("top10", top10);
        model.addAttribute("myEntry", myEntry);
        model.addAttribute("totalUsers", all.size());
        return "ranking/index";
    }
}
