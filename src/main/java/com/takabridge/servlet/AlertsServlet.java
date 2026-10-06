package com.takabridge.servlet;

import com.takabridge.model.Currency;
import com.takabridge.model.RateAlert;
import com.takabridge.service.ConversionService;
import com.takabridge.service.ValidationException;
import com.takabridge.util.AlertCookie;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Rate alerts: "tell me when 1 USD goes below 120 BDT".
 *
 * The alerts live in a cookie in the visitor's browser (see AlertCookie), so
 * this feature needs no database table. Every time a page is opened, the
 * alerts are compared with today's rates and the ones that fired are shown.
 *
 * Adding and deleting follow Post / Redirect / Get, like the converter.
 */
@WebServlet("/alerts")
public class AlertsServlet extends BaseServlet {

    private final ConversionService service = new ConversionService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession();
        List<RateAlert> alerts = AlertCookie.read(request);

        try {
            List<Currency> currencies = service.listCurrencies();
            alerts = service.checkAlerts(alerts, currencies);
            request.setAttribute("currencies", currencies);
            request.setAttribute("triggeredAlerts",
                    alerts.stream().filter(RateAlert::isTriggered).toList());
        } catch (SQLException e) {
            getServletContext().log("Could not read currencies for the alerts page", e);
            request.setAttribute("dbError",
                    "Today's rates cannot be read right now, so alerts cannot be checked. "
                  + "Please check that Oracle is running and try again.");
        }

        request.setAttribute("alerts", alerts);
        request.setAttribute("maxAlerts", AlertCookie.MAX_ALERTS);
        request.setAttribute("error", Flash.take(session, Flash.ALERT_ERROR));
        request.setAttribute("notice", Flash.take(session, Flash.ALERT_NOTICE));

        render(request, response, "alerts.jsp", "alerts");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        request.setCharacterEncoding("UTF-8");
        HttpSession session = request.getSession();
        List<RateAlert> alerts = new ArrayList<>(AlertCookie.read(request));
        String action = request.getParameter("action");

        if ("delete".equals(action)) {
            int index = parseIndex(request.getParameter("index"));
            if (index >= 0 && index < alerts.size()) {
                alerts.remove(index);
                AlertCookie.write(request, response, alerts);
                Flash.put(session, Flash.ALERT_NOTICE, "Alert removed.");
            }

        } else if ("add".equals(action)) {
            if (alerts.size() >= AlertCookie.MAX_ALERTS) {
                Flash.put(session, Flash.ALERT_ERROR,
                        "You already have " + AlertCookie.MAX_ALERTS + " alerts. Remove one to add another.");
            } else {
                try {
                    alerts.add(service.createAlert(
                            request.getParameter("from"),
                            request.getParameter("to"),
                            request.getParameter("direction"),
                            request.getParameter("target")));
                    AlertCookie.write(request, response, alerts);
                    Flash.put(session, Flash.ALERT_NOTICE, "Alert saved. We will check it every time you visit.");

                } catch (ValidationException e) {
                    Flash.put(session, Flash.ALERT_ERROR, e.getMessage());
                } catch (SQLException e) {
                    getServletContext().log("Could not check the alert currencies", e);
                    Flash.put(session, Flash.ALERT_ERROR,
                            "The alert could not be saved because the database is not reachable.");
                }
            }
        }

        response.sendRedirect(request.getContextPath() + "/alerts");
    }

    private int parseIndex(String raw) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
