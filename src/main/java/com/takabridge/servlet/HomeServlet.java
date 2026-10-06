package com.takabridge.servlet;

import com.takabridge.model.Conversion;
import com.takabridge.model.Currency;
import com.takabridge.model.RateAlert;
import com.takabridge.model.Route;
import com.takabridge.model.Trend;
import com.takabridge.service.ConversionService;
import com.takabridge.service.RateHistoryClient;
import com.takabridge.util.AlertCookie;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

/**
 * The converter page.
 *
 * A conversion is performed by ConvertServlet, which then redirects back here.
 * This servlet only reads what happened, clears it from the session so it is
 * shown exactly once, and draws the page - together with the cheapest route
 * for that conversion, any rate alerts that fired, and a small 7-day trend.
 */
@WebServlet("/home")
public class HomeServlet extends BaseServlet {

    /** How many past conversions appear under the converter. */
    private static final int RECENT_ON_HOME = 5;

    /** Days shown in the small trend card and the change column. */
    private static final int TREND_DAYS = 7;

    private final ConversionService service = new ConversionService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession();

        // The Reset link empties the form and any message still waiting.
        if (request.getParameter("reset") != null) {
            Flash.clear(session);
        }

        // Whatever ConvertServlet left behind, shown once and then removed.
        Conversion result = (Conversion) Flash.take(session, Flash.RESULT);
        String formFrom = Flash.takeText(session, Flash.FROM, "USD");
        String formTo = Flash.takeText(session, Flash.TO, "BDT");

        request.setAttribute("result", result);
        request.setAttribute("error", Flash.take(session, Flash.ERROR));
        request.setAttribute("formFrom", formFrom);
        request.setAttribute("formTo", formTo);
        request.setAttribute("formAmount", Flash.takeText(session, Flash.AMOUNT, "100"));

        try {
            List<Currency> currencies = service.listCurrencies();
            request.setAttribute("currencies", currencies);
            request.setAttribute("recent", service.recentHistory(session.getId(), RECENT_ON_HOME));
            request.setAttribute("ratesUpdated", service.ratesLastUpdated());

            // Smart route: the cheapest way to make the conversion just shown.
            if (result != null) {
                List<Route> routes = service.routesFor(result, currencies);
                Route direct = routes.stream().filter(Route::isDirect).findFirst().orElse(null);
                if (direct != null) {
                    Route best = routes.get(0);
                    request.setAttribute("bestRoute", best);
                    request.setAttribute("directRoute", direct);
                    request.setAttribute("routeSaving", best.getReceived().subtract(direct.getReceived()));
                }
            }

            // Rate alerts that have reached their target since the last visit.
            List<RateAlert> alerts = service.checkAlerts(AlertCookie.read(request), currencies);
            List<RateAlert> triggered = alerts.stream().filter(RateAlert::isTriggered).toList();
            request.setAttribute("alertCount", alerts.size());
            request.setAttribute("triggeredAlerts", triggered);

        } catch (SQLException e) {
            getServletContext().log("Could not read from Oracle", e);
            request.setAttribute("dbError",
                    "TakaBridge cannot reach the database right now. "
                  + "Please check that Oracle is running and try again.");
        }

        addTrends(request, formFrom, formTo);

        render(request, response, "index.jsp", "home");
    }

    /**
     * The 7-day sparkline for the selected pair and the 7-day change of every
     * currency against the dollar. Only past rates already in memory are used,
     * so the home page never waits on the network; if none are there yet, the
     * page is simply drawn without them.
     */
    private void addTrends(HttpServletRequest request, String from, String to) {
        RateHistoryClient history = rateHistory();
        if (history == null) {
            return;
        }
        SortedMap<LocalDate, Map<String, BigDecimal>> week = history.lastDaysCached(TREND_DAYS);
        if (week.size() < 2) {
            return;
        }

        request.setAttribute("miniTrend", Trend.of(from, to, week));

        Map<String, BigDecimal> weekChange = new HashMap<>();
        for (String code : week.get(week.lastKey()).keySet()) {
            Trend trend = Trend.of("USD", code, week);
            if (trend != null) {
                weekChange.put(code, trend.getChangePercent());
            }
        }
        request.setAttribute("weekChange", weekChange);
    }
}
