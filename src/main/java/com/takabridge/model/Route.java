package com.takabridge.model;

import java.math.BigDecimal;
import java.util.List;

/**
 * One way of getting from one currency to another, e.g. BDT -> USD -> INR,
 * together with what you would end up with after the typical fees of each step.
 */
public class Route {

    private final List<String> path;        // currency codes in order, e.g. [BDT, USD, INR]
    private final BigDecimal received;      // amount at the end, after fees
    private final BigDecimal feePercent;    // total cost compared with the fee-free figure

    public Route(List<String> path, BigDecimal received, BigDecimal feePercent) {
        this.path = List.copyOf(path);
        this.received = received;
        this.feePercent = feePercent;
    }

    public List<String> getPath() {
        return path;
    }

    public BigDecimal getReceived() {
        return received;
    }

    public BigDecimal getFeePercent() {
        return feePercent;
    }

    /** True when the money goes straight from A to B with no stop in between. */
    public boolean isDirect() {
        return path.size() == 2;
    }

    /** The currency passed through on the way, or null for a direct route. */
    public String getVia() {
        return isDirect() ? null : path.get(1);
    }

    /** "BDT → USD → INR" */
    public String getLabel() {
        return String.join(" → ", path);
    }
}
