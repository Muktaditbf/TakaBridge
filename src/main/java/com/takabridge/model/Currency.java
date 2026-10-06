package com.takabridge.model;

import java.math.BigDecimal;

/**
 * One supported currency, e.g. BDT - Bangladeshi Taka.
 *
 * A plain encapsulated class: every field is private and is reached only
 * through its getter / setter. This mirrors one row of the CURRENCIES table.
 */
public class Currency {

    private int currencyId;
    private String code;         // 3 letter ISO code, e.g. "BDT"
    private String name;         // full name, e.g. "Bangladeshi Taka"
    private String symbol;       // display symbol, e.g. the Taka sign
    private BigDecimal ratePerUsd; // how many units equal 1 US Dollar

    /** No-argument constructor, used when a Currency is built field by field. */
    public Currency() {
    }

    /** Full constructor, used by CurrencyDAO when reading a database row. */
    public Currency(int currencyId, String code, String name, String symbol, BigDecimal ratePerUsd) {
        this.currencyId = currencyId;
        this.code = code;
        this.name = name;
        this.symbol = symbol;
        this.ratePerUsd = ratePerUsd;
    }

    public int getCurrencyId() {
        return currencyId;
    }

    public void setCurrencyId(int currencyId) {
        this.currencyId = currencyId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public BigDecimal getRatePerUsd() {
        return ratePerUsd;
    }

    public void setRatePerUsd(BigDecimal ratePerUsd) {
        this.ratePerUsd = ratePerUsd;
    }

    /** Label shown inside the dropdown lists, e.g. "BDT - Bangladeshi Taka". */
    public String getLabel() {
        return code + " - " + name;
    }

    @Override
    public String toString() {
        return getLabel();
    }
}
