package com.sample.positionbook.dto;

import com.sample.positionbook.entity.EventType;

import javax.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;

public class TradeEventRequest {
    @NotNull @Positive
    @Schema(example = "21", description = "Unique trade ID. CANCEL must reuse the original ID.")
    public Long id;
    @NotNull
    @Schema(example = "BUY")
    public EventType type;
    @Schema(example = "ACC1", description = "Required for BUY/SELL; optional for CANCEL and must match original account if supplied.")
    public String tradingAccount;
    @Schema(example = "SEC1", description = "Required for BUY/SELL; ignored for CANCEL.")
    public String securityId;
    @Schema(example = "100", description = "Positive whole number for BUY/SELL; ignored for CANCEL.")
    public Long quantity;
}
