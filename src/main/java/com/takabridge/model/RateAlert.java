package com.takabridge.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * "Tell me when 1 USD goes below 120 BDT."
 *
 * Alerts are kept in a cookie in the visitor's own browser, not in the
 * database, so this class mirrors nothing in Oracle.
 */
public class RateAlert {

    /** Which side of the target the visitor is waiting for. */
    public enum Direction {
        ABOVE("A", "goes above"),
        BELOW("B", "goes below");

        private final String code;   // one letter, used inside the cookie
        private final String words;  // used on the page

        Direction(String code, String words) {
            this.code = code;
            this.words = words;
        }

        public String getCode() {
            return code;
        }

        public String getWords() {
            return words;
        }

        /** "A" or "ABOVE" both give ABOVE; anything else gives null. */
        public static Direction parse(String text) {
            if (text == null) {
                return null;
            }
            for (Direction d : values()) {
                if (d.code.equalsIgnoreCase(text) || d.name().equalsIgnoreCase(text)) {
                    return d;
                }
            }
            return null;
        }
    }

    private final String fromCode;
    private final String toCode;
    private final Direction direction;
    private final BigDecimal target;

    /** Today's rate, filled in by the servlet before the page is drawn. */
    private BigDecimal currentRate;

    public RateAlert(String fromCode, String toCode, Direction direction, BigDecimal target) {
        this.fromCode = fromCode;
        this.toCode = toCode;
        this.direction = direction;
        this.target = target;
    }

    public String getFromCode() {
        return fromCode;
    }

    public String getToCode() {
        return toCode;
    }

    public Direction getDirection() {
        return direction;
    }

    public BigDecimal getTarget() {
        return target;
    }

    public BigDecimal getCurrentRate() {
        return currentRate;
    }

    public void setCurrentRate(BigDecimal currentRate) {
        this.currentRate = currentRate;
    }

    /** True once today's rate has reached the target. */
    public boolean isTriggered() {
        if (currentRate == null) {
            return false;
        }
        int compared = currentRate.compareTo(target);
        return direction == Direction.ABOVE ? compared >= 0 : compared <= 0;
    }

    /** How far today's rate still is from the target, in percent, e.g. 2.61. */
    public BigDecimal getDistancePercent() {
        if (currentRate == null || currentRate.signum() == 0) {
            return null;
        }
        return target.subtract(currentRate).abs()
                .multiply(BigDecimal.valueOf(100))
                .divide(currentRate, 2, RoundingMode.HALF_UP);
    }
}
