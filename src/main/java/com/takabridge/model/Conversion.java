package com.takabridge.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * One completed conversion, e.g. 100 USD to 12,200.00 BDT.
 *
 * The same object is used twice: once to show the result on screen, and once
 * to save the row into the CONVERSION_HISTORY table.
 */
public class Conversion {

    private long historyId;
    private String sessionId;
    private String fromCode;
    private String toCode;
    private BigDecimal amount;     // what the user typed
    private BigDecimal converted;  // what we calculated
    private BigDecimal rateUsed;   // 1 fromCode = rateUsed toCode
    private LocalDateTime createdAt;

    // Symbols are read together with the row so the page can print the sign
    // in front of the figure without a second database call.
    private String fromSymbol;
    private String toSymbol;

    public Conversion() {
    }

    public Conversion(String sessionId, String fromCode, String toCode,
                      BigDecimal amount, BigDecimal converted, BigDecimal rateUsed) {
        this.sessionId = sessionId;
        this.fromCode = fromCode;
        this.toCode = toCode;
        this.amount = amount;
        this.converted = converted;
        this.rateUsed = rateUsed;
    }

    public long getHistoryId() {
        return historyId;
    }

    public void setHistoryId(long historyId) {
        this.historyId = historyId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getFromCode() {
        return fromCode;
    }

    public void setFromCode(String fromCode) {
        this.fromCode = fromCode;
    }

    public String getToCode() {
        return toCode;
    }

    public void setToCode(String toCode) {
        this.toCode = toCode;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getConverted() {
        return converted;
    }

    public void setConverted(BigDecimal converted) {
        this.converted = converted;
    }

    public BigDecimal getRateUsed() {
        return rateUsed;
    }

    public void setRateUsed(BigDecimal rateUsed) {
        this.rateUsed = rateUsed;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getFromSymbol() {
        return fromSymbol;
    }

    public void setFromSymbol(String fromSymbol) {
        this.fromSymbol = fromSymbol;
    }

    public String getToSymbol() {
        return toSymbol;
    }

    public void setToSymbol(String toSymbol) {
        this.toSymbol = toSymbol;
    }
}
