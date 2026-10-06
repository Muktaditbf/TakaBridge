package com.takabridge.dao;

import com.takabridge.model.Currency;
import com.takabridge.util.DBConnection;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * All reading and updating of the CURRENCIES table happens here.
 *
 * Every statement is a PreparedStatement, so a value coming from the browser
 * can never become part of the SQL text itself.
 */
public class CurrencyDAO {

    /** BDT and USD are pinned to the top of the lists, the rest is A to Z. */
    private static final String SELECT_ALL = """
            SELECT currency_id, code, name, symbol, rate_per_usd
            FROM   currencies
            ORDER  BY CASE code
                          WHEN 'BDT' THEN 0
                          WHEN 'USD' THEN 1
                          ELSE 2
                      END,
                      code
            """;

    private static final String SELECT_BY_CODE = """
            SELECT currency_id, code, name, symbol, rate_per_usd
            FROM   currencies
            WHERE  code = ?
            """;

    /** Only currencies already in the table are touched; new codes are ignored. */
    private static final String UPDATE_RATE = """
            UPDATE currencies
            SET    rate_per_usd = ?,
                   updated_at   = SYSTIMESTAMP
            WHERE  code = ?
            """;

    private static final String SELECT_LAST_UPDATE = """
            SELECT MAX(updated_at) AS last_update
            FROM   currencies
            """;

    /** Every supported currency, ready for the dropdown lists and the rates table. */
    public List<Currency> findAll() throws SQLException {
        List<Currency> currencies = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_ALL);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                currencies.add(readRow(rs));
            }
        }
        return currencies;
    }

    /** One currency, or null when the code is not supported. */
    public Currency findByCode(String code) throws SQLException {
        if (code == null || code.isBlank()) {
            return null;
        }
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_BY_CODE)) {

            ps.setString(1, code.trim().toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? readRow(rs) : null;
            }
        }
    }

    /**
     * Saves a fresh set of rates, all of them or none of them.
     *
     * The updates run as one transaction: if any single row fails, the whole
     * batch is rolled back, so the table never holds half old and half new rates.
     *
     * @param ratesPerUsd currency code to units per 1 US Dollar
     * @return how many currencies were updated
     */
    public int updateRates(Map<String, BigDecimal> ratesPerUsd) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(UPDATE_RATE)) {

            conn.setAutoCommit(false);
            try {
                for (Map.Entry<String, BigDecimal> rate : ratesPerUsd.entrySet()) {
                    // The column holds six decimals, so round to fit it exactly.
                    ps.setBigDecimal(1, rate.getValue().setScale(6, RoundingMode.HALF_UP));
                    ps.setString(2, rate.getKey());
                    ps.addBatch();
                }

                int updated = 0;
                for (int rows : ps.executeBatch()) {
                    if (rows > 0 || rows == Statement.SUCCESS_NO_INFO) {
                        updated++;
                    }
                }
                conn.commit();
                return updated;

            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    /** When the rates were last changed, or null when the table is empty. */
    public LocalDateTime findLastUpdate() throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_LAST_UPDATE);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                Timestamp when = rs.getTimestamp("last_update");
                return when == null ? null : when.toLocalDateTime();
            }
            return null;
        }
    }

    /** Copies the current row of the result set into a Currency object. */
    private Currency readRow(ResultSet rs) throws SQLException {
        int id = rs.getInt("currency_id");
        String code = rs.getString("code").trim();   // CHAR(3) is space padded
        String name = rs.getString("name");
        String symbol = rs.getString("symbol");
        BigDecimal rate = rs.getBigDecimal("rate_per_usd");
        return new Currency(id, code, name, symbol, rate);
    }
}
