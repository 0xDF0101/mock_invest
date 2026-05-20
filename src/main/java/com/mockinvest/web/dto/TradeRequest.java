package com.mockinvest.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TradeRequest {

    @NotBlank
    private String ticker;

    @Min(value = 1, message = "수량은 1 이상이어야 합니다.")
    private int quantity;
}
