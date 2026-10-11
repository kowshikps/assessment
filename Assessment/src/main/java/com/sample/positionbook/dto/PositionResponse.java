package com.sample.positionbook.dto;

import java.util.List;

public class PositionResponse {
    public final String tradingAccount;
    public final String securityId;
    public final long quantity;
    public final List<EventDetail> events;

    public PositionResponse(String account, String security, long quantity, List<EventDetail> events) {
        this.tradingAccount = account;
        this.securityId = security;
        this.quantity = quantity;
        this.events = List.copyOf(events);
    }
}
