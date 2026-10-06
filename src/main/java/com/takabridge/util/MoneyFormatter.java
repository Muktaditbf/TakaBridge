package com.takabridge.util;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Turns raw numbers and timestamps into the strings the pages display.
 *
 * Keeping the formatting here means every page shows figures the same way
 * and no JSP ever has to do arithmetic or string building of its own.
 */
public final class MoneyFormatter {

    private static final DecimalFormatSymbols SYMBOLS =
            DecimalFormatSymbols.getInstance(Locale.US);

    /** 12200 becomes "12,200.00" - always two decimals, grouped thousands. */
    private static final DecimalFormat MONEY = new DecimalFormat("#,##0.00", SYMBOLS);

    /** 122 becomes "122.0000" - rates are shown with four decimals. */
    private static final DecimalFormat RATE = new DecimalFormat("#,##0.0000", SYMBOLS);

    private static final DateTimeFormatter TIME_ONLY =
            DateTimeFormatter.ofPattern("hh:mm a", Locale.US);

    private static final DateTimeFormatter FULL_DATE =
            DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.US);

    private static final DateTimeFormatter SHORT_DATE =
            DateTimeFormatter.ofPattern("dd MMM", Locale.US);

    private MoneyFormatter() {
    }

    /** Money with grouping and two decimals. */
    public static String money(BigDecimal value) {
        if (value == null) {
            return "0.00";
        }
        return MONEY.format(value);
    }

    /** An exchange rate with four decimals. */
    public static String rate(BigDecimal value) {
        if (value == null) {
            return "0.0000";
        }
        return RATE.format(value);
    }

    /** The currency sign in front of the figure, e.g. the Taka sign + 12,200.00 */
    public static String withSymbol(String symbol, BigDecimal value) {
        String figure = money(value);
        if (symbol == null || symbol.isBlank()) {
            return figure;
        }
        return symbol + figure;
    }

    /** "Today", "Yesterday", or "10 Sep 2026" - easier to scan than a raw date. */
    public static String day(LocalDateTime when) {
        if (when == null) {
            return "";
        }
        LocalDate date = when.toLocalDate();
        LocalDate today = LocalDate.now();
        if (date.isEqual(today)) {
            return "Today";
        }
        if (date.isEqual(today.minusDays(1))) {
            return "Yesterday";
        }
        return FULL_DATE.format(date);
    }

    /** "+0.42%" or "-1.10%" - a change with its sign always shown. */
    public static String percent(BigDecimal value) {
        if (value == null) {
            return "";
        }
        String figure = new DecimalFormat("0.00", SYMBOLS).format(value.abs());
        int sign = value.signum();
        return (sign > 0 ? "+" : sign < 0 ? "−" : "") + figure + "%";
    }

    /** "06 Oct" - short labels under the trend charts. */
    public static String shortDay(LocalDate date) {
        return date == null ? "" : SHORT_DATE.format(date);
    }

    /** "06 Oct 2026" */
    public static String fullDay(LocalDate date) {
        return date == null ? "" : FULL_DATE.format(date);
    }

    /** "01:33 AM" - shown under the day label in the history table. */
    public static String time(LocalDateTime when) {
        if (when == null) {
            return "";
        }
        return TIME_ONLY.format(when);
    }
}
