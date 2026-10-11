package com.sample.positionbook.dto;

import com.sample.positionbook.entity.EventType;

public class EventDetail {
    public final long id;
    public final EventType type;
    public final String tradingAccount;
    public final String securityId;
    public final long quantity;

    public EventDetail(long id, EventType type, String tradingAccount, String securityId, long quantity) {
        this.id = id;
        this.type = type;
        this.tradingAccount = tradingAccount;
        this.securityId = securityId;
        this.quantity = quantity;
    }
}
