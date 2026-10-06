package com.takabridge.service;

import com.takabridge.dao.CurrencyDAO;
import com.takabridge.dao.HistoryDAO;
import com.takabridge.model.Conversion;
import com.takabridge.model.Currency;
import com.takabridge.model.RateAlert;
import com.takabridge.model.Remittance;
import com.takabridge.model.Route;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The brain of TakaBridge: it checks the input, does the arithmetic and
 * records the result.
 *
 * Servlets never calculate and never touch the database directly - they ask
 * this class, which is what keeps the layers apart.
 */
public class ConversionService {

    /** Largest amount we accept, one trillion. */
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("1000000000000");

    /** Working precision before the result is rounded for display. */
    private static final int WORKING_SCALE = 12;

    private final CurrencyDAO currencyDAO = new CurrencyDAO();
    private final HistoryDAO historyDAO = new HistoryDAO();
    private final RouteFinder routeFinder = new RouteFinder();
    private final RemittanceCalculator remittanceCalculator = new RemittanceCalculator();

    /** Every supported currency, for the dropdowns and the rates table. */
    public List<Currency> listCurrencies() throws SQLException {
        return currencyDAO.findAll();
    }

    /** When the exchange rates were last refreshed, or null if unknown. */
    public LocalDateTime ratesLastUpdated() throws SQLException {
        return currencyDAO.findLastUpdate();
    }

    /** The newest conversions of this visitor. */
    public List<Conversion> recentHistory(String sessionId, int limit) throws SQLException {
        return historyDAO.findRecent(sessionId, limit);
    }

    /** Deletes this visitor's history. */
    public int clearHistory(String sessionId) throws SQLException {
        return historyDAO.deleteBySession(sessionId);
    }

    /**
     * Validates the form, converts the amount and saves the result.
     *
     * Rates are stored as units per 1 US Dollar, so converting from A to B is
     *      rate = rateOfB / rateOfA
     * and the money itself is BigDecimal from start to finish. A double would
     * lose paisa on large amounts, which is never acceptable for money.
     */
    public Conversion convert(String sessionId, String fromCode, String toCode, String rawAmount)
            throws ValidationException, SQLException {

        BigDecimal amount = parseAmount(rawAmount);

        if (fromCode == null || fromCode.isBlank() || toCode == null || toCode.isBlank()) {
            throw new ValidationException("Please choose both a From currency and a To currency.");
        }
        if (fromCode.equalsIgnoreCase(toCode)) {
            throw new ValidationException("Choose two different currencies, or press Swap.");
        }

        Currency from = currencyDAO.findByCode(fromCode);
        Currency to = currencyDAO.findByCode(toCode);
        if (from == null || to == null) {
            throw new ValidationException("That currency is not supported yet.");
        }

        BigDecimal rate = crossRate(from, to);

        BigDecimal converted = amount.multiply(rate).setScale(2, RoundingMode.HALF_UP);

        Conversion conversion = new Conversion(
                sessionId,
                from.getCode(),
                to.getCode(),
                amount.setScale(2, RoundingMode.HALF_UP),
                converted,
                rate.setScale(6, RoundingMode.HALF_UP));

        conversion.setFromSymbol(from.getSymbol());
        conversion.setToSymbol(to.getSymbol());

        historyDAO.save(conversion);
        return conversion;
    }

    /**
     * How many units of {@code to} one unit of {@code from} buys.
     *
     * The one formula every feature shares - the converter, the route finder
     * and the rate alerts - so they can never disagree with each other.
     */
    public static BigDecimal crossRate(Currency from, Currency to) {
        return to.getRatePerUsd().divide(from.getRatePerUsd(), WORKING_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Every way of making the conversion just shown, cheapest first, after the
     * typical exchange fees. Returns an empty list if a currency is unknown.
     */
    public List<Route> routesFor(Conversion conversion, List<Currency> currencies) {
        Map<String, Currency> byCode = byCode(currencies);
        Currency from = byCode.get(conversion.getFromCode());
        Currency to = byCode.get(conversion.getToCode());
        if (from == null || to == null) {
            return List.of();
        }
        return routeFinder.findRoutes(from, to, conversion.getAmount(), currencies);
    }

    /**
     * Fills in today's rate on every alert, using a currency list the caller
     * has already loaded, and drops alerts for currencies that no longer exist.
     */
    public List<RateAlert> checkAlerts(List<RateAlert> alerts, List<Currency> currencies) {
        Map<String, Currency> byCode = byCode(currencies);
        List<RateAlert> checked = new ArrayList<>();
        for (RateAlert alert : alerts) {
            Currency from = byCode.get(alert.getFromCode());
            Currency to = byCode.get(alert.getToCode());
            if (from != null && to != null) {
                alert.setCurrentRate(crossRate(from, to).setScale(6, RoundingMode.HALF_UP));
                checked.add(alert);
            }
        }
        return checked;
    }

    /**
     * Money sent home: what the family receives through each channel, best
     * first, after fees and the government's 2.5% bonus.
     */
    public List<Remittance> remittance(String fromCode, String rawAmount)
            throws ValidationException, SQLException {

        BigDecimal amount = parseAmount(rawAmount);
        Currency from = currencyDAO.findByCode(fromCode);
        if (from == null || "BDT".equals(from.getCode())) {
            throw new ValidationException("Choose the currency you are sending from abroad.");
        }
        return remittanceCalculator.compare(amount, from, bdt(), usd());
    }

    /** True when a transfer of this size needs documents for the bonus. */
    public boolean remittanceNeedsDocuments(String fromCode, String rawAmount)
            throws ValidationException, SQLException {
        Currency from = currencyDAO.findByCode(fromCode);
        return from != null && remittanceCalculator.needsDocuments(parseAmount(rawAmount), from, usd());
    }

    private Currency bdt() throws SQLException {
        return required("BDT");
    }

    private Currency usd() throws SQLException {
        return required("USD");
    }

    private Currency required(String code) throws SQLException {
        Currency currency = currencyDAO.findByCode(code);
        if (currency == null) {
            throw new SQLException(code + " is missing from the CURRENCIES table");
        }
        return currency;
    }

    /** Validates the "add an alert" form and builds the alert. */
    public RateAlert createAlert(String fromCode, String toCode, String direction, String rawTarget)
            throws ValidationException, SQLException {

        if (fromCode == null || toCode == null || fromCode.equalsIgnoreCase(toCode)) {
            throw new ValidationException("Choose two different currencies for the alert.");
        }
        Currency from = currencyDAO.findByCode(fromCode);
        Currency to = currencyDAO.findByCode(toCode);
        if (from == null || to == null) {
            throw new ValidationException("That currency is not supported yet.");
        }
        RateAlert.Direction side = RateAlert.Direction.parse(direction);
        if (side == null) {
            throw new ValidationException("Choose whether the rate should go above or below the target.");
        }
        BigDecimal target = parsePositive(rawTarget, "target rate");
        return new RateAlert(from.getCode(), to.getCode(), side, target.setScale(6, RoundingMode.HALF_UP));
    }

    private static Map<String, Currency> byCode(List<Currency> currencies) {
        Map<String, Currency> map = new HashMap<>();
        for (Currency c : currencies) {
            map.put(c.getCode(), c);
        }
        return map;
    }

    /**
     * Turns the typed amount into a number, refusing anything that is not a
     * sensible positive figure. Thousands separators are allowed because
     * people paste them in.
     */
    private BigDecimal parseAmount(String rawAmount) throws ValidationException {
        return parsePositive(rawAmount, "amount");
    }

    /** Shared by the amount and the alert target: a sensible positive number. */
    private BigDecimal parsePositive(String raw, String what) throws ValidationException {
        if (raw == null || raw.isBlank()) {
            throw new ValidationException("Please enter the " + what + ".");
        }

        String cleaned = raw.trim().replace(",", "").replace(" ", "");

        BigDecimal value;
        try {
            value = new BigDecimal(cleaned);
        } catch (NumberFormatException e) {
            throw new ValidationException("The " + what + " must be a number, for example 100 or 2500.50");
        }

        if (value.signum() <= 0) {
            throw new ValidationException("The " + what + " must be greater than zero.");
        }
        if (value.compareTo(MAX_AMOUNT) > 0) {
            throw new ValidationException("That " + what + " is too large. Please enter one trillion or less.");
        }
        return value;
    }
}
