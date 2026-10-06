package com.takabridge.servlet;

import com.takabridge.model.Conversion;
import com.takabridge.service.ConversionService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

/**
 * The full history page: this visitor's last fifty conversions, newest first.
 */
@WebServlet("/history")
public class HistoryServlet extends BaseServlet {

    private static final int MAX_ROWS = 50;

    private final ConversionService service = new ConversionService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        try {
            List<Conversion> history =
                    service.recentHistory(request.getSession().getId(), MAX_ROWS);
            request.setAttribute("history", history);

        } catch (SQLException e) {
            getServletContext().log("Could not read the history table", e);
            request.setAttribute("dbError",
                    "TakaBridge cannot reach the database right now. "
                  + "Please check that Oracle is running and try again.");
        }

        // A message left by ClearHistoryServlet, shown once.
        request.setAttribute("notice", Flash.take(request.getSession(), Flash.RESULT));

        render(request, response, "history.jsp", "history");
    }
}
