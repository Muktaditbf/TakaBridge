package com.takabridge.service;

import com.takabridge.model.Currency;
import com.takabridge.model.Route;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Finds the cheapest way to change money from one currency to another.
 *
 * Real exchange houses charge more for rarely traded pairs. Changing taka
 * straight into rupees is expensive, while taka to dollars and dollars to
 * rupees are both common, so going through the dollar often leaves you with
 * more money. This class tries the direct route and every route with one stop
 * in between, and returns them best first.
 *
 * The fees below are typical estimates for study, not quotes from a bank.
 */
public class RouteFinder {

    /** Heavily traded currencies, cheap to exchange between each other. */
    private static final Set<String> MAJORS = Set.of("USD", "EUR", "GBP", "JPY", "CHF", "CAD", "AUD");

    public static final BigDecimal FEE_MAJOR_MAJOR = new BigDecimal("0.005");  // 0.5%
    public static final BigDecimal FEE_MAJOR_MINOR = new BigDecimal("0.015");  // 1.5%
    public static final BigDecimal FEE_MINOR_MINOR = new BigDecimal("0.040");  // 4%

    private static final int WORKING_SCALE = 12;

    /**
     * Every possible route from {@code from} to {@code to}, best first.
     * When two routes give the same amount, the direct one wins.
     */
    public List<Route> findRoutes(Currency from, Currency to, BigDecimal amount, List<Currency> all) {
        List<Route> routes = new ArrayList<>();
        BigDecimal feeFree = amount.multiply(ConversionService.crossRate(from, to));

        routes.add(walk(amount, feeFree, List.of(from, to)));
        for (Currency via : all) {
            if (!via.getCode().equals(from.getCode()) && !via.getCode().equals(to.getCode())) {
                routes.add(walk(amount, feeFree, List.of(from, via, to)));
            }
        }

        // A stable sort keeps the direct route ahead of an equally good detour.
        routes.sort(Comparator.comparing(Route::getReceived).reversed());
        return routes;
    }

    /** The fee for one exchange, decided by how commonly the pair is traded. */
    public static BigDecimal feeFor(String a, String b) {
        boolean majorA = MAJORS.contains(a);
        boolean majorB = MAJORS.contains(b);
        if (majorA && majorB) {
            return FEE_MAJOR_MAJOR;
        }
        return (majorA || majorB) ? FEE_MAJOR_MINOR : FEE_MINOR_MINOR;
    }

    /** Follows the path step by step, taking the fee off after each exchange. */
    private Route walk(BigDecimal amount, BigDecimal feeFree, List<Currency> path) {
        BigDecimal money = amount;
        List<String> codes = new ArrayList<>();
        codes.add(path.get(0).getCode());

        for (int i = 1; i < path.size(); i++) {
            Currency a = path.get(i - 1);
            Currency b = path.get(i);
            BigDecimal keep = BigDecimal.ONE.subtract(feeFor(a.getCode(), b.getCode()));
            money = money.multiply(ConversionService.crossRate(a, b))
                         .multiply(keep)
                         .setScale(WORKING_SCALE, RoundingMode.HALF_UP);
            codes.add(b.getCode());
        }

        BigDecimal feePercent = BigDecimal.ONE
                .subtract(money.divide(feeFree, WORKING_SCALE, RoundingMode.HALF_UP))
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);

        return new Route(codes, money.setScale(2, RoundingMode.HALF_UP), feePercent);
    }
}
