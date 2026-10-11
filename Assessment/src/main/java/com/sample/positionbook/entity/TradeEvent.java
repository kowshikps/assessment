package com.sample.positionbook.entity;

/** An accepted event stored in the event history. */
public class TradeEvent {
    private final long id;
    private final EventType type;
    private final String tradingAccount;
    private final String securityId;
    private final long quantity;

    public TradeEvent(long id, EventType type, String account, String security, long quantity) {
        this.id = id; this.type = type; this.tradingAccount = account;
        this.securityId = security; this.quantity = quantity;
    }
    public long getId() { return id; }
    public EventType getType() { return type; }
    public String getTradingAccount() { return tradingAccount; }
    public String getSecurityId() { return securityId; }
    public long getQuantity() { return quantity; }
}
