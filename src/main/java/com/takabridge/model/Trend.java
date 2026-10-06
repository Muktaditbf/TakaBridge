package com.takabridge.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SortedMap;

/**
 * How the rate between two currencies moved over the last few days, plus the
 * points needed to draw it as a line chart.
 *
 * The chart is plain SVG written by the JSP, so the coordinates are worked out
 * here in Java and no JavaScript is needed in the browser.
 */
public class Trend {

    /**
     * One day on the chart. x and y are SVG coordinates; dayChange is the
     * percent change from the day before (null for the first day).
     */
    public record Point(LocalDate date, BigDecimal rate, BigDecimal dayChange, double x, double y) {
    }

    private final String fromCode;
    private final String toCode;
    private final List<LocalDate> dates;
    private final List<BigDecimal> rates;
    private final BigDecimal min;
    private final BigDecimal max;

    private Trend(String fromCode, String toCode, List<LocalDate> dates, List<BigDecimal> rates) {
        this.fromCode = fromCode;
        this.toCode = toCode;
        this.dates = dates;
        this.rates = rates;
        this.min = rates.stream().min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        this.max = rates.stream().max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
    }

    /**
     * Builds the trend of 1 fromCode in toCode from daily rates per US Dollar.
     * Days where either currency is missing are skipped.
     *
     * @return the trend, or null when fewer than two days are available
     */
    public static Trend of(String fromCode, String toCode,
                           SortedMap<LocalDate, Map<String, BigDecimal>> ratesPerUsdByDay) {
        List<LocalDate> dates = new ArrayList<>();
        List<BigDecimal> rates = new ArrayList<>();

        for (Map.Entry<LocalDate, Map<String, BigDecimal>> day : ratesPerUsdByDay.entrySet()) {
            BigDecimal from = day.getValue().get(fromCode);
            BigDecimal to = day.getValue().get(toCode);
            if (from != null && to != null && from.signum() > 0) {
                dates.add(day.getKey());
                // Same formula as the converter: rate = rate of B / rate of A
                rates.add(to.divide(from, 6, RoundingMode.HALF_UP));
            }
        }
        return dates.size() < 2 ? null : new Trend(fromCode, toCode, dates, rates);
    }

    public String getFromCode() {
        return fromCode;
    }

    public String getToCode() {
        return toCode;
    }

    public int getDays() {
        return dates.size();
    }

    public LocalDate getFirstDate() {
        return dates.get(0);
    }

    public LocalDate getLastDate() {
        return dates.get(dates.size() - 1);
    }

    public BigDecimal getFirst() {
        return rates.get(0);
    }

    public BigDecimal getLast() {
        return rates.get(rates.size() - 1);
    }

    public BigDecimal getMin() {
        return min;
    }

    public BigDecimal getMax() {
        return max;
    }

    /** Halfway between the lowest and highest rate, for the middle chart line. */
    public BigDecimal getMid() {
        return min.add(max).divide(BigDecimal.valueOf(2), 6, RoundingMode.HALF_UP);
    }

    /** Change from the first day to the last, in percent, e.g. 0.42 or -1.10 */
    public BigDecimal getChangePercent() {
        return percentChange(getFirst(), getLast());
    }

    private static BigDecimal percentChange(BigDecimal before, BigDecimal after) {
        return after.subtract(before)
                .multiply(BigDecimal.valueOf(100))
                .divide(before, 2, RoundingMode.HALF_UP);
    }

    /** +1 when the rate went up, -1 when it went down, 0 when it did not move. */
    public int getDirection() {
        return getChangePercent().signum();
    }

    /** Every day, newest first, for the table under the chart. */
    public List<Point> getPointsNewestFirst(double width, double height) {
        List<Point> points = new ArrayList<>(getPoints(width, height));
        Collections.reverse(points);
        return points;
    }

    /**
     * Places every day inside a box of the given size. The oldest day is on
     * the left, the lowest rate at the bottom, with a little padding so the
     * line never touches the edges.
     */
    public List<Point> getPoints(double width, double height) {
        double padY = height * 0.1;
        double usableHeight = height - 2 * padY;
        double step = width / (dates.size() - 1);
        double spread = max.subtract(min).doubleValue();

        List<Point> points = new ArrayList<>();
        for (int i = 0; i < dates.size(); i++) {
            double share = spread == 0 ? 0.5 : max.subtract(rates.get(i)).doubleValue() / spread;
            BigDecimal dayChange = (i == 0) ? null : percentChange(rates.get(i - 1), rates.get(i));
            points.add(new Point(dates.get(i), rates.get(i), dayChange, i * step, padY + share * usableHeight));
        }
        return points;
    }

    /** The "x,y x,y ..." list an SVG polyline expects. */
    public String getPolyline(double width, double height) {
        StringBuilder out = new StringBuilder();
        for (Point p : getPoints(width, height)) {
            out.append(String.format(Locale.US, "%.1f,%.1f ", p.x(), p.y()));
        }
        return out.toString().trim();
    }

    /** The same line closed along the bottom edge, for the soft filled area. */
    public String getArea(double width, double height) {
        return String.format(Locale.US, "0,%.1f %s %.1f,%.1f",
                height, getPolyline(width, height), width, height);
    }
}
