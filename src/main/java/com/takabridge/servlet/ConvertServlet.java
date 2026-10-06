package com.takabridge.servlet;

import com.takabridge.model.Conversion;
import com.takabridge.service.ConversionService;
import com.takabridge.service.ValidationException;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.SQLException;

/**
 * Handles the converter form: both the Convert button and the Swap button.
 *
 * Nothing is drawn here. The outcome is put in the session and the browser is
 * redirected to /home, which is the Post / Redirect / Get pattern that stops a
 * refresh from repeating the conversion.
 */
@WebServlet("/convert")
public class ConvertServlet extends BaseServlet {

    private final ConversionService service = new ConversionService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        request.setCharacterEncoding("UTF-8");

        HttpSession session = request.getSession();
        String from = trimmed(request.getParameter("from"));
        String to = trimmed(request.getParameter("to"));
        String amount = trimmed(request.getParameter("amount"));
        String action = trimmed(request.getParameter("action"));

        // Swap simply exchanges the two dropdowns. Nothing is calculated and
        // nothing is written to the database.
        if ("swap".equals(action)) {
            rememberForm(session, to, from, amount);
            redirectHome(request, response);
            return;
        }

        try {
            Conversion conversion = service.convert(session.getId(), from, to, amount);
            Flash.put(session, Flash.RESULT, conversion);
            rememberForm(session, from, to, amount);

        } catch (ValidationException e) {
            // The visitor made a mistake, so the message is theirs to read.
            Flash.put(session, Flash.ERROR, e.getMessage());
            rememberForm(session, from, to, amount);

        } catch (SQLException e) {
            // A database problem is ours, not theirs. The details go to the
            // Tomcat log; the visitor gets a plain sentence.
            getServletContext().log("Conversion failed while talking to Oracle", e);
            Flash.put(session, Flash.ERROR,
                    "TakaBridge could not save that conversion. "
                  + "Please check that Oracle is running and try again.");
            rememberForm(session, from, to, amount);
        }

        redirectHome(request, response);
    }

    /** Sends the visitor to the converter page without resubmitting the form. */
    private void redirectHome(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.sendRedirect(request.getContextPath() + "/home#converter");
    }

    /** Keeps the form filled in exactly as the visitor left it. */
    private void rememberForm(HttpSession session, String from, String to, String amount) {
        Flash.put(session, Flash.FROM, from == null ? "USD" : from);
        Flash.put(session, Flash.TO, to == null ? "BDT" : to);
        Flash.put(session, Flash.AMOUNT, amount == null ? "" : amount);
    }

    private String trimmed(String value) {
        return (value == null) ? null : value.trim();
    }
}
