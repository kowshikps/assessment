package com.sample.positionbook.entity;

public class PositionKey {
    private final String tradingAccount;
    private final String securityId;
    public PositionKey(String account, String security) {
        this.tradingAccount = account; this.securityId = security;
    }
    public String getTradingAccount() { return tradingAccount; }
    public String getSecurityId() { return securityId; }
}
