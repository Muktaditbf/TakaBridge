package com.takabridge.model;

import java.math.BigDecimal;

/**
 * Money sent home to Bangladesh through one channel, worked out step by step:
 *
 *     amount sent  -  transfer fee  =  amount exchanged
 *     amount exchanged  x  the channel's rate  =  taka before bonus
 *     taka before bonus  +  2.5% government bonus  =  what the family receives
 */
public class Remittance {

    private final String channel;          // e.g. "Online transfer app"
    private final String fromCode;         // e.g. "SAR"
    private final String fromSymbol;
    private final BigDecimal sent;         // what the sender hands over
    private final BigDecimal fee;          // flat transfer fee, in the sending currency
    private final BigDecimal exchanged;    // sent - fee
    private final BigDecimal marketRate;   // 1 fromCode in BDT, mid-market
    private final BigDecimal channelRate;  // the slightly lower rate the channel gives
    private final BigDecimal takaBeforeBonus;
    private final BigDecimal bonus;        // the government's incentive, in taka
    private final BigDecimal familyGets;   // the final figure

    public Remittance(String channel, String fromCode, String fromSymbol,
                      BigDecimal sent, BigDecimal fee, BigDecimal exchanged,
                      BigDecimal marketRate, BigDecimal channelRate,
                      BigDecimal takaBeforeBonus, BigDecimal bonus, BigDecimal familyGets) {
        this.channel = channel;
        this.fromCode = fromCode;
        this.fromSymbol = fromSymbol;
        this.sent = sent;
        this.fee = fee;
        this.exchanged = exchanged;
        this.marketRate = marketRate;
        this.channelRate = channelRate;
        this.takaBeforeBonus = takaBeforeBonus;
        this.bonus = bonus;
        this.familyGets = familyGets;
    }

    public String getChannel() {
        return channel;
    }

    public String getFromCode() {
        return fromCode;
    }

    public String getFromSymbol() {
        return fromSymbol;
    }

    public BigDecimal getSent() {
        return sent;
    }

    public BigDecimal getFee() {
        return fee;
    }

    public BigDecimal getExchanged() {
        return exchanged;
    }

    public BigDecimal getMarketRate() {
        return marketRate;
    }

    public BigDecimal getChannelRate() {
        return channelRate;
    }

    public BigDecimal getTakaBeforeBonus() {
        return takaBeforeBonus;
    }

    public BigDecimal getBonus() {
        return bonus;
    }

    public BigDecimal getFamilyGets() {
        return familyGets;
    }

    /** True when the fee would swallow the whole amount. */
    public boolean isTooSmall() {
        return exchanged.signum() <= 0;
    }
}
