package com.takabridge.servlet;

import com.takabridge.service.ConversionService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

/**
 * Deletes this visitor's history, but only after they confirm it.
 *
 * The link opens a confirmation page (GET); the button on that page actually
 * deletes (POST). Asking first is what keeps an irreversible action safe.
 */
@WebServlet("/clear-history")
public class ClearHistoryServlet extends BaseServlet {

    private final ConversionService service = new ConversionService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        render(request, response, "clear-history.jsp", "history");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        try {
            int removed = service.clearHistory(request.getSession().getId());
            Flash.put(request.getSession(), Flash.RESULT,
                    removed == 0
                        ? "There was nothing to clear."
                        : removed + (removed == 1 ? " conversion was cleared."
                                                  : " conversions were cleared."));

        } catch (SQLException e) {
            getServletContext().log("Could not clear the history table", e);
            Flash.put(request.getSession(), Flash.RESULT,
                    "TakaBridge could not clear the history. Please try again.");
        }

        response.sendRedirect(request.getContextPath() + "/history");
    }
}
