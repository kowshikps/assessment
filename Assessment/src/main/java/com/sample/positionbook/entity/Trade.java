package com.sample.positionbook.entity;

/** A persisted original trade and its cancellation state. */
public class Trade {
    private final long id;
    private final EventType type;
    private final String tradingAccount;
    private final String securityId;
    private final long quantity;
    private final boolean cancelled;

    public Trade(long id, EventType type, String account, String security, long quantity, boolean cancelled) {
        this.id = id; this.type = type; this.tradingAccount = account;
        this.securityId = security; this.quantity = quantity; this.cancelled = cancelled;
    }
    public long getId() { return id; }
    public EventType getType() { return type; }
    public String getTradingAccount() { return tradingAccount; }
    public String getSecurityId() { return securityId; }
    public long getQuantity() { return quantity; }
    public boolean isCancelled() { return cancelled; }
}
