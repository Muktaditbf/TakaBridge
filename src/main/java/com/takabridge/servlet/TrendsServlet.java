package com.takabridge.servlet;

import com.takabridge.model.Currency;
import com.takabridge.model.Trend;
import com.takabridge.service.ConversionService;
import com.takabridge.service.RateHistoryClient;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;

/**
 * The rate trend page: how one currency moved against another over the last
 * 7 or 30 days, drawn as a line chart.
 *
 * The past rates come from RateHistoryClient (kept in memory, never in the
 * database). The chart itself is plain SVG written by trends.jsp.
 */
@WebServlet("/trends")
public class TrendsServlet extends BaseServlet {

    private static final Set<Integer> ALLOWED_DAYS = Set.of(7, 30);

    private final ConversionService service = new ConversionService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String from = code(request.getParameter("from"), "USD");
        String to = code(request.getParameter("to"), "BDT");
        int days = days(request.getParameter("days"));

        try {
            List<Currency> currencies = service.listCurrencies();
            request.setAttribute("currencies", currencies);
            // Only currencies TakaBridge supports may be charted.
            boolean known = currencies.stream().anyMatch(c -> c.getCode().equals(from))
                         && currencies.stream().anyMatch(c -> c.getCode().equals(to));
            if (!known) {
                request.setAttribute("error", "That currency is not supported yet.");
            }
        } catch (SQLException e) {
            getServletContext().log("Could not read currencies for the trends page", e);
            request.setAttribute("dbError",
                    "TakaBridge cannot reach the database right now. "
                  + "Please check that Oracle is running and try again.");
        }

        if (from.equals(to)) {
            request.setAttribute("error", "Choose two different currencies to compare.");
        }

        RateHistoryClient history = rateHistory();
        if (request.getAttribute("error") == null && history != null) {
            Trend trend = Trend.of(from, to, history.lastDays(days));
            if (trend == null) {
                request.setAttribute("error",
                        "Past rates could not be downloaded right now. "
                      + "Please check the internet connection and try again.");
            }
            request.setAttribute("trend", trend);
        }

        request.setAttribute("from", from);
        request.setAttribute("to", to);
        request.setAttribute("days", days);

        render(request, response, "trends.jsp", "trends");
    }

    /** A three-letter code in capitals, or the fallback for anything else. */
    private String code(String raw, String fallback) {
        if (raw == null) {
            return fallback;
        }
        String trimmed = raw.trim().toUpperCase();
        return trimmed.matches("[A-Z]{3}") ? trimmed : fallback;
    }

    private int days(String raw) {
        try {
            int value = Integer.parseInt(raw);
            return ALLOWED_DAYS.contains(value) ? value : 7;
        } catch (NumberFormatException e) {
            return 7;
        }
    }
}
