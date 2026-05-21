package com.mockinvest.domain.ranking;

import java.math.BigDecimal;

public record RankEntry(int rank, String username, BigDecimal totalAsset, BigDecimal returnRate) {}
