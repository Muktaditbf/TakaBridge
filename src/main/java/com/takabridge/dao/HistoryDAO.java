package com.takabridge.dao;

import com.takabridge.model.Conversion;
import com.takabridge.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Create, read and delete for the CONVERSION_HISTORY table.
 *
 * History belongs to a browser session, so every statement is filtered by
 * session id and one visitor can never see another visitor's conversions.
 */
public class HistoryDAO {

    private static final String INSERT = """
            INSERT INTO conversion_history
                   (session_id, from_code, to_code, amount, converted, rate_used)
            VALUES (?, ?, ?, ?, ?, ?)
            """;

    /** The join brings the two currency symbols along with the row. */
    private static final String SELECT_RECENT = """
            SELECT h.history_id, h.from_code, h.to_code, h.amount, h.converted,
                   h.rate_used, h.created_at,
                   f.symbol AS from_symbol, t.symbol AS to_symbol
            FROM   conversion_history h
                   JOIN currencies f ON f.code = h.from_code
                   JOIN currencies t ON t.code = h.to_code
            WHERE  h.session_id = ?
            ORDER  BY h.created_at DESC, h.history_id DESC
            FETCH  FIRST ? ROWS ONLY
            """;

    private static final String DELETE_BY_SESSION = """
            DELETE FROM conversion_history
            WHERE  session_id = ?
            """;

    /** Saves one completed conversion. */
    public void save(Conversion conversion) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(INSERT)) {

            ps.setString(1, conversion.getSessionId());
            ps.setString(2, conversion.getFromCode());
            ps.setString(3, conversion.getToCode());
            ps.setBigDecimal(4, conversion.getAmount());
            ps.setBigDecimal(5, conversion.getConverted());
            ps.setBigDecimal(6, conversion.getRateUsed());
            ps.executeUpdate();
        }
    }

    /** The newest conversions of one visitor, most recent first. */
    public List<Conversion> findRecent(String sessionId, int limit) throws SQLException {
        List<Conversion> history = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_RECENT)) {

            ps.setString(1, sessionId);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    history.add(readRow(rs));
                }
            }
        }
        return history;
    }

    /** Removes every conversion of one visitor. Returns how many rows went. */
    public int deleteBySession(String sessionId) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(DELETE_BY_SESSION)) {

            ps.setString(1, sessionId);
            return ps.executeUpdate();
        }
    }

    private Conversion readRow(ResultSet rs) throws SQLException {
        Conversion c = new Conversion();
        c.setHistoryId(rs.getLong("history_id"));
        c.setFromCode(rs.getString("from_code").trim());
        c.setToCode(rs.getString("to_code").trim());
        c.setAmount(rs.getBigDecimal("amount"));
        c.setConverted(rs.getBigDecimal("converted"));
        c.setRateUsed(rs.getBigDecimal("rate_used"));
        c.setFromSymbol(rs.getString("from_symbol"));
        c.setToSymbol(rs.getString("to_symbol"));

        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) {
            c.setCreatedAt(created.toLocalDateTime());
        }
        return c;
    }
}
